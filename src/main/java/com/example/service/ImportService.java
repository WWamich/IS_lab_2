package com.example.service;

import com.example.model.*;
import com.example.repository.FieldMappingRepository;
import com.example.repository.HumanRepository;
import com.opencsv.CSVReader;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ImportService {

    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private ImportLogService importLogService;
    @Autowired
    private FieldMappingRepository fieldMappingRepository;
    @Autowired
    private Validator validator;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final List<String> ENTITY_FIELDS;

    private static final Map<String, List<String>> CORE_SYNONYMS;

    static {
        Map<String, List<String>> map = new HashMap<>();
        map.put("name", Arrays.asList("имя", "name", "водитель", "driver", "фио"));
        map.put("realHero", Arrays.asList("герой", "realhero", "hero", "правдивый", "настоящий"));
        map.put("hasToothpick", Arrays.asList("зубочистка", "toothpick", "зуб"));
        map.put("impactSpeed", Arrays.asList("скорость", "impact", "speed", "удар"));
        map.put("soundtrackName", Arrays.asList("трек", "soundtrack", "музыка", "песня", "саундтрек"));
        map.put("minutesOfWaiting", Arrays.asList("ожидание", "waiting", "минуты", "minutes"));
        map.put("mood", Arrays.asList("настроение", "mood", "эмоция"));
        map.put("weaponType", Arrays.asList("оружие", "weapon", "тип", "type"));
        map.put("coordinates.x", Arrays.asList("координата x", "x", "coord x"));
        map.put("coordinates.y", Arrays.asList("координата y", "y", "coord y"));
        map.put("car.name", Arrays.asList("автомобиль", "car", "машина"));
        map.put("car.cool", Arrays.asList("крутой", "cool", "awesome"));
        CORE_SYNONYMS = Collections.unmodifiableMap(map);
    }

    private final Map<String, LearnedMapping> learnedMappings = new ConcurrentHashMap<>();

    private static final double BASE_NAME_WEIGHT = 0.3;
    private static final double BASE_TYPE_WEIGHT = 0.5;
    private static final double BASE_VALUE_WEIGHT = 0.2;
    private static final double MINIMUM_CONFIDENCE_SCORE = 0.4;

    static {
        List<String> fields = Stream.of(Human.class.getDeclaredFields())
                .map(Field::getName)
                .filter(name -> !name.equals("id") && !name.equals("creationDate") && !name.equals("version"))
                .collect(Collectors.toList());
        fields.addAll(Arrays.asList("coordinates.x", "coordinates.y", "car.name", "car.cool"));
        ENTITY_FIELDS = Collections.unmodifiableList(fields);
    }

    private static class LearnedMapping {
        private final String targetField;
        private int usageCount;
        private LocalDate lastUsed;

        public LearnedMapping(String targetField, int usageCount) {
            this.targetField = targetField;
            this.usageCount = usageCount;
            this.lastUsed = LocalDate.now();
        }

        public String getTargetField() { return targetField; }
        public int getUsageCount() { return usageCount; }
        public void incrementUsageCount() {
            usageCount++;
            lastUsed = LocalDate.now();
        }
        public double getConfidence() {
            return 0.7 + (0.3 * Math.min(usageCount / 10.0, 1.0));
        }
    }

    @PostConstruct
    public void loadLearnedMappings() {
        fieldMappingRepository.findAll().forEach(mapping -> {
            learnedMappings.put(mapping.getSourceFieldName(),
                    new LearnedMapping(mapping.getTargetEntityField(), mapping.getUsageCount()));
        });
    }

    public List<String> getTargetFields() {
        return ENTITY_FIELDS;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void importHumansFromCsv(MultipartFile file, Map<String, String> confirmedMappings) throws Exception {
        ImportLog log = importLogService.createNewLog();

        try {
            saveNewMappings(confirmedMappings);
            List<Human> humansToSave = parseAndValidateCsv(file, confirmedMappings);

            Set<String> uniqueKeysInBatch = new HashSet<>();
            for (Human human : humansToSave) {
                String uniqueKey = human.getName().toLowerCase() + "||" + human.getSoundtrackName().toLowerCase();
                if (!uniqueKeysInBatch.add(uniqueKey)) {
                    throw new Exception("Ошибка уникальности в файле: Найден дубликат с именем '" + human.getName() +
                            "' и саундтреком '" + human.getSoundtrackName() + "'.");
                }
                if (humanRepository.existsByNameAndSoundtrackName(human.getName(), human.getSoundtrackName())) {
                    throw new Exception("Ошибка уникальности: Объект с именем '" + human.getName() +
                            "' и саундтреком '" + human.getSoundtrackName() + "' уже существует в базе.");
                }
            }

            humanRepository.saveAll(humansToSave);
            messagingTemplate.convertAndSend("/topic/humans", "update");

            importLogService.markAsSuccess(log.getId(), humansToSave.size());

        } catch (Exception e) {
            importLogService.markAsFailed(log.getId(), e.getMessage());
            throw e;
        }
    }

    private List<Human> parseAndValidateCsv(MultipartFile file, Map<String, String> confirmedMappings) throws Exception {
        List<Human> humans = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> allRows = reader.readAll();
            if (allRows.size() < 2) {
                throw new Exception("CSV файл должен содержать заголовок и хотя бы одну строку с данными.");
            }
            String[] headers = allRows.get(0);
            Map<Integer, String> fieldMap = mapHeadersToFields(headers, confirmedMappings);

            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                Human human = new Human();
                Coordinates coordinates = new Coordinates();
                Car car = new Car();
                boolean carDataPresent = false;

                for (int j = 0; j < row.length; j++) {
                    String targetField = fieldMap.get(j);
                    String value = (j < row.length) ? row[j] : "";
                    if (targetField == null || value == null || value.isBlank() || targetField.equals("none")) {
                        continue;
                    }

                    try {
                        switch (targetField) {
                            case "name": human.setName(value); break;
                            case "realHero": human.setRealHero(parseBoolean(value)); break;
                            case "hasToothpick": human.setHasToothpick(parseBoolean(value)); break;
                            case "impactSpeed": human.setImpactSpeed(Long.parseLong(value)); break;
                            case "soundtrackName": human.setSoundtrackName(value); break;
                            case "minutesOfWaiting": human.setMinutesOfWaiting(Float.parseFloat(value)); break;
                            case "mood": human.setMood(parseMood(value)); break;
                            case "weaponType": human.setWeaponType(parseWeaponType(value)); break;
                            case "coordinates.x": coordinates.setX(Float.parseFloat(value)); break;
                            case "coordinates.y": coordinates.setY(Long.parseLong(value)); break;
                            case "car.name": car.setName(value); carDataPresent = true; break;
                            case "car.cool": car.setCool(parseBoolean(value)); carDataPresent = true; break;
                        }
                    } catch (Exception e) {
                        throw new Exception("Ошибка конвертации данных в строке " + (i + 1) + ", столбец '" + headers[j] + "': недопустимое значение '" + value + "' для поля " + targetField);
                    }
                }

                human.setCreationDate(LocalDate.now());
                human.setCoordinates(coordinates);
                if (carDataPresent) {
                    human.setCar(car);
                }

                Set<ConstraintViolation<Human>> violations = validator.validate(human);
                if (!violations.isEmpty()) {
                    String errors = violations.stream()
                            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .collect(Collectors.joining(", "));
                    throw new Exception("Ошибка валидации в строке " + (i + 1) + ": " + errors);
                }
                humans.add(human);
            }
        }
        return humans;
    }

    private Boolean parseBoolean(String value) {
        if (value == null) return false;
        String lowerValue = value.toLowerCase().trim();
        return lowerValue.equals("true") || lowerValue.equals("да") || lowerValue.equals("1") || lowerValue.equals("yes");
    }

    private Mood parseMood(String value) {
        if (value == null) return null;
        String upperValue = value.toUpperCase().trim();

        Map<String, Mood> moodMap = new HashMap<>();
        moodMap.put("GLOOM", Mood.GLOOM);
        moodMap.put("УНЫНИЕ", Mood.GLOOM);
        moodMap.put("ГРУСТЬ", Mood.GLOOM);
        moodMap.put("RAGE", Mood.RAGE);
        moodMap.put("ГНЕВ", Mood.RAGE);
        moodMap.put("ЗЛОСТЬ", Mood.RAGE);
        moodMap.put("FRENZY", Mood.FRENZY);
        moodMap.put("БЕШЕНСТВО", Mood.FRENZY);
        moodMap.put("ЯРОСТЬ", Mood.FRENZY);

        return moodMap.get(upperValue);
    }


    private WeaponType parseWeaponType(String value) {
        if (value == null) return null;
        String upperValue = value.toUpperCase().trim();

        Map<String, WeaponType> weaponMap = new HashMap<>();
        weaponMap.put("RIFLE", WeaponType.RIFLE);
        weaponMap.put("ВИНТОВКА", WeaponType.RIFLE);
        weaponMap.put("MACHINE_GUN", WeaponType.MACHINE_GUN);
        weaponMap.put("ПУЛЕМЕТ", WeaponType.MACHINE_GUN);
        weaponMap.put("АВТОМАТ", WeaponType.MACHINE_GUN);
        weaponMap.put("BAT", WeaponType.BAT);
        weaponMap.put("БИТА", WeaponType.BAT);
        weaponMap.put("ДУБИНА", WeaponType.BAT);

        return weaponMap.get(upperValue);
    }


    private List<String[]> readSampleData(MultipartFile file, int sampleSize) throws Exception {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> allRows = reader.readAll();
            if (allRows.size() < 2) {
                return allRows.isEmpty() ? new ArrayList<>() : allRows.subList(0, 1);
            }
            int limit = Math.min(allRows.size(), sampleSize + 1);
            return new ArrayList<>(allRows.subList(0, limit));
        }
    }

    private double calculateNameSimilarity(String sourceHeader, String targetField) {
        JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

        String normalizedSource = normalizeHeader(sourceHeader);
        String cleanTarget = targetField.replace("coordinates.", "").replace("car.", "").toLowerCase();

        double directScore = similarity.apply(normalizedSource, cleanTarget);

        double synonymScore = CORE_SYNONYMS.getOrDefault(targetField, Collections.emptyList())
                .stream()
                .mapToDouble(synonym -> {
                    String normalizedSynonym = normalizeHeader(synonym);
                    return similarity.apply(normalizedSource, normalizedSynonym);
                })
                .max()
                .orElse(0.0);

        return Math.max(directScore, synonymScore);
    }

    private String normalizeHeader(String header) {
        String transliterated = transliterateRussian(header.toLowerCase());
        return transliterated.replaceAll("[^a-z0-9]", "")
                .replaceAll("(field|column|поле|столбец)", "");
    }

    private String transliterateRussian(String text) {
        Map<Character, String> translitMap = new HashMap<>();
        translitMap.put('а', "a");
        translitMap.put('б', "b");
        translitMap.put('в', "v");
        translitMap.put('г', "g");
        translitMap.put('д', "d");
        translitMap.put('е', "e");
        translitMap.put('ё', "e");
        translitMap.put('ж', "zh");
        translitMap.put('з', "z");
        translitMap.put('и', "i");
        translitMap.put('й', "y");
        translitMap.put('к', "k");
        translitMap.put('л', "l");
        translitMap.put('м', "m");
        translitMap.put('н', "n");
        translitMap.put('о', "o");
        translitMap.put('п', "p");
        translitMap.put('р', "r");
        translitMap.put('с', "s");
        translitMap.put('т', "t");
        translitMap.put('у', "u");
        translitMap.put('ф', "f");
        translitMap.put('х', "kh");
        translitMap.put('ц', "ts");
        translitMap.put('ч', "ch");
        translitMap.put('ш', "sh");
        translitMap.put('щ', "shch");
        translitMap.put('ы', "y");
        translitMap.put('э', "e");
        translitMap.put('ю', "yu");
        translitMap.put('я', "ya");

        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (translitMap.containsKey(c)) {
                result.append(translitMap.get(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }


    private double calculateTypeCompatibility(List<String> values, String targetField) {
        if (values.isEmpty()) {
            return 0.0;
        }

        long compatibleCount = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .filter(value -> isValueCompatibleWithField(value, targetField))
                .count();

        double baseScore = (double) compatibleCount / values.size();

        if (baseScore > 0.8) {
            return Math.min(1.0, baseScore + 0.2);
        }

        return baseScore;
    }

    private boolean isValueCompatibleWithField(String value, String targetField) {
        try {
            switch (targetField) {
                case "impactSpeed":
                case "coordinates.y":
                    Long.parseLong(value.trim());
                    return true;
                case "minutesOfWaiting":
                case "coordinates.x":
                    Float.parseFloat(value.trim());
                    return true;
                case "realHero":
                case "hasToothpick":
                case "car.cool":
                    String boolVal = value.trim().toLowerCase();
                    return boolVal.equals("true") || boolVal.equals("false") ||
                            boolVal.equals("да") || boolVal.equals("нет") ||
                            boolVal.equals("1") || boolVal.equals("0") ||
                            boolVal.equals("yes") || boolVal.equals("no");
                case "mood":
                    return parseMood(value) != null;
                case "weaponType":
                    return parseWeaponType(value) != null;
                default:
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private double calculateValueFrequencyScore(List<String> values, String targetField) {
        if (values.isEmpty()) return 0.0;

        Set<String> expectedValues = getExpectedValuesForField(targetField);
        if (expectedValues.isEmpty()) {
            return 0.0;
        }

        long matchingCount = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .filter(v -> expectedValues.contains(v.toUpperCase()) ||
                        isValueMatchingExpected(v, targetField, expectedValues))
                .count();

        return (double) matchingCount / values.size();
    }

    private boolean isValueMatchingExpected(String value, String targetField, Set<String> expectedValues) {
        String upperValue = value.toUpperCase();

        return switch (targetField) {
            case "mood" ->
                    (upperValue.contains("УНЫНИЕ") && expectedValues.contains("GLOOM")) ||
                            (upperValue.contains("ГНЕВ") && expectedValues.contains("RAGE")) ||
                            (upperValue.contains("БЕШЕНСТВО") && expectedValues.contains("FRENZY"));
            case "weaponType" ->
                    (upperValue.contains("ВИНТОВКА") && expectedValues.contains("RIFLE")) ||
                            (upperValue.contains("ПУЛЕМЕТ") && expectedValues.contains("MACHINE_GUN")) ||
                            (upperValue.contains("БИТА") && expectedValues.contains("BAT"));
            default -> false;
        };
    }

    private Set<String> getExpectedValuesForField(String targetField) {
        return switch (targetField) {
            case "mood" -> Arrays.stream(Mood.values()).map(Enum::name).collect(Collectors.toSet());
            case "weaponType" -> Arrays.stream(WeaponType.values()).map(Enum::name).collect(Collectors.toSet());
            default -> Collections.emptySet();
        };
    }

    public Map<String, String> suggestMappings(MultipartFile file) throws Exception {
        Map<String, String> suggestedMappings = new LinkedHashMap<>();

        List<String[]> sampleData = readSampleData(file, 50);
        if (sampleData.isEmpty()) {
            return suggestedMappings;
        }

        String[] headers = sampleData.get(0);
        List<String[]> dataRows = sampleData.size() > 1 ? sampleData.subList(1, sampleData.size()) : new ArrayList<>();

        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim();

            LearnedMapping learnedMapping = learnedMappings.get(header);
            if (learnedMapping != null && learnedMapping.getConfidence() >= MINIMUM_CONFIDENCE_SCORE) {
                suggestedMappings.put(header, learnedMapping.getTargetField());
                continue;
            }

            final int colIndex = i;
            List<String> columnValues = dataRows.stream()
                    .filter(row -> row.length > colIndex)
                    .map(row -> row[colIndex])
                    .collect(Collectors.toList());

            FieldMatch bestMatch = findBestFieldMatch(header, columnValues);

            if (bestMatch.getConfidenceScore() >= MINIMUM_CONFIDENCE_SCORE) {
                suggestedMappings.put(header, bestMatch.getFieldName());
            } else {
                suggestedMappings.put(header, "none");
            }
        }

        return suggestedMappings;
    }

    private FieldMatch findBestFieldMatch(String header, List<String> columnValues) {
        FieldMatch bestMatch = new FieldMatch("none", 0.0);

        for (String targetField : getTargetFields()) {
            double nameScore = calculateNameSimilarity(header, targetField);
            double typeScore = calculateTypeCompatibility(columnValues, targetField);
            double valueScore = calculateValueFrequencyScore(columnValues, targetField);

            double[] weights = calculateAdaptiveWeights(typeScore, valueScore);

            double totalScore = (nameScore * weights[0]) +
                    (typeScore * weights[1]) +
                    (valueScore * weights[2]);

            if (totalScore > bestMatch.getConfidenceScore()) {
                bestMatch = new FieldMatch(targetField, totalScore);
            }
        }

        return bestMatch;
    }

    private double[] calculateAdaptiveWeights(double typeScore, double valueScore) {
        double nameWeight = BASE_NAME_WEIGHT;
        double typeWeight = BASE_TYPE_WEIGHT;
        double valueWeight = BASE_VALUE_WEIGHT;

        if (typeScore > 0.9) {
            typeWeight = 0.7;
            nameWeight = 0.2;
            valueWeight = 0.1;
        }

        if (valueScore > 0.8) {
            valueWeight = 0.4;
            nameWeight = 0.3;
            typeWeight = 0.3;
        }

        return new double[]{nameWeight, typeWeight, valueWeight};
    }

    private static class FieldMatch {
        private final String fieldName;
        private final double confidenceScore;

        public FieldMatch(String fieldName, double confidenceScore) {
            this.fieldName = fieldName;
            this.confidenceScore = confidenceScore;
        }

        public String getFieldName() { return fieldName; }
        public double getConfidenceScore() { return confidenceScore; }
    }

    private Map<Integer, String> mapHeadersToFields(String[] headers, Map<String, String> confirmedMappings) {
        Map<Integer, String> positionToFieldMap = new HashMap<>();
        for(int i = 0; i < headers.length; i++){
            String header = headers[i];
            positionToFieldMap.put(i, confirmedMappings.get(header));
        }
        return positionToFieldMap;
    }

    private void saveNewMappings(Map<String, String> confirmedMappings) {
        for(Map.Entry<String, String> entry : confirmedMappings.entrySet()){
            String sourceField = entry.getKey();
            String targetField = entry.getValue();

            if(targetField != null && !targetField.equals("none")) {
                learnFromUserMapping(sourceField, targetField);
                saveToDatabase(sourceField, targetField);
            }
        }
    }

    private void learnFromUserMapping(String sourceHeader, String targetField) {
        LearnedMapping existing = learnedMappings.get(sourceHeader);
        if (existing != null && existing.getTargetField().equals(targetField)) {
            existing.incrementUsageCount();
        } else {
            learnedMappings.put(sourceHeader, new LearnedMapping(targetField, 1));
        }
    }

    private void saveToDatabase(String sourceField, String targetField) {
        Optional<FieldMapping> existing = fieldMappingRepository.findBySourceFieldName(sourceField);
        if (existing.isPresent()) {
            FieldMapping mapping = existing.get();
            mapping.setTargetEntityField(targetField);
            mapping.setLastUsed(LocalDate.now());
            mapping.setUsageCount(mapping.getUsageCount() + 1);
            fieldMappingRepository.save(mapping);
        } else {
            FieldMapping newMapping = new FieldMapping();
            newMapping.setSourceFieldName(sourceField);
            newMapping.setTargetEntityField(targetField);
            newMapping.setLastUsed(LocalDate.now());
            newMapping.setUsageCount(1);
            fieldMappingRepository.save(newMapping);
        }
    }
}