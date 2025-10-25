package com.example.service;

import com.example.model.*;
import com.example.repository.FieldMappingRepository;
import com.example.repository.HumanRepository;
import com.example.repository.ImportLogRepository;
import com.opencsv.CSVReader;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ImportService {

    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private ImportLogRepository importLogRepository;
    @Autowired
    private FieldMappingRepository fieldMappingRepository;
    @Autowired
    private Validator validator;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final List<String> ENTITY_FIELDS;

    static {
        List<String> fields = Stream.of(Human.class.getDeclaredFields())
                .map(Field::getName)
                .filter(name -> !name.equals("id") && !name.equals("creationDate") && !name.equals("version") && !name.equals("coordinates") && !name.equals("car"))
                .collect(Collectors.toList());
        fields.addAll(Arrays.asList("coordinates.x", "coordinates.y", "car.name", "car.cool"));
        ENTITY_FIELDS = Collections.unmodifiableList(fields);
    }

    public List<String> getTargetFields() {
        return ENTITY_FIELDS;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void importHumansFromCsv(MultipartFile file, Map<String, String> confirmedMappings) throws Exception {
        ImportLog log = new ImportLog();
        log.setStatus(ImportStatus.IN_PROGRESS);
        importLogRepository.saveAndFlush(log);

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

            log.setStatus(ImportStatus.SUCCESS);
            log.setAddedCount(humansToSave.size());
            importLogRepository.save(log);

        } catch (Exception e) {
            log.setStatus(ImportStatus.FAILED);
            log.setErrorDetails(e.getMessage().length() > 1024 ? e.getMessage().substring(0, 1024) : e.getMessage());
            importLogRepository.save(log);
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
                            case "realHero": human.setRealHero(Boolean.parseBoolean(value)); break;
                            case "hasToothpick": human.setHasToothpick(Boolean.parseBoolean(value)); break;
                            case "impactSpeed": human.setImpactSpeed(Long.parseLong(value)); break;
                            case "soundtrackName": human.setSoundtrackName(value); break;
                            case "minutesOfWaiting": human.setMinutesOfWaiting(Float.parseFloat(value)); break;
                            case "mood": human.setMood(Mood.valueOf(value.toUpperCase())); break;
                            case "weaponType": human.setWeaponType(WeaponType.valueOf(value.toUpperCase())); break;
                            case "coordinates.x": coordinates.setX(Float.parseFloat(value)); break;
                            case "coordinates.y": coordinates.setY(Long.parseLong(value)); break;
                            case "car.name": car.setName(value); carDataPresent = true; break;
                            case "car.cool": car.setCool(Boolean.parseBoolean(value)); carDataPresent = true; break;
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

    public Map<String, String> suggestMappings(MultipartFile file) throws Exception {
        Map<String, String> suggestedMappings = new LinkedHashMap<>();
        Map<String, FieldMapping> existingMappings = fieldMappingRepository.findAll().stream()
                .collect(Collectors.toMap(FieldMapping::getSourceFieldName, Function.identity()));

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = reader.readNext();
            if (headers == null) return suggestedMappings;

            JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

            for (String header : headers) {
                if (existingMappings.containsKey(header)) {
                    suggestedMappings.put(header, existingMappings.get(header).getTargetEntityField());
                } else {
                    String bestMatch = ENTITY_FIELDS.stream()
                            .max(Comparator.comparing(field -> similarity.apply(header.toLowerCase(), field.toLowerCase())))
                            .orElse(null);

                    if(bestMatch != null && similarity.apply(header.toLowerCase(), bestMatch.toLowerCase()) > 0.8){
                        suggestedMappings.put(header, bestMatch);
                    } else {
                        suggestedMappings.put(header, "none");
                    }
                }
            }
        }
        return suggestedMappings;
    }

    private Map<Integer, String> mapHeadersToFields(String[] headers, Map<String, String> confirmedMappings) {
        Map<Integer, String> positionToFieldMap = new HashMap<>();
        for(int i = 0; i < headers.length; i++){
            String header = headers[i];
            positionToFieldMap.put(i, confirmedMappings.get(header));
        }
        return positionToFieldMap;
    }

    private void saveNewMappings(Map<String, String> confirmedMappings){
        for(Map.Entry<String, String> entry : confirmedMappings.entrySet()){
            String sourceField = entry.getKey();
            String targetField = entry.getValue();

            if(targetField != null && !targetField.equals("none")) {
                Optional<FieldMapping> existing = fieldMappingRepository.findBySourceFieldName(sourceField);
                if (existing.isEmpty()) {
                    FieldMapping newMapping = new FieldMapping();
                    newMapping.setSourceFieldName(sourceField);
                    newMapping.setTargetEntityField(targetField);
                    fieldMappingRepository.save(newMapping);
                }
            }
        }
    }
}