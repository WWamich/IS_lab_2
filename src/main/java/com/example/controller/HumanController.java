package com.example.controller;

import com.example.model.Human;
import com.example.model.WeaponType;
import com.example.service.HumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.service.ImportService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/human-s")
@CrossOrigin(origins = "*")
public class HumanController {

    @Autowired
    private HumanService humanService;

    @Autowired
    private ImportService importService;

    @GetMapping
    public ResponseEntity<List<Human>> getAllHumans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<Human> humans = humanService.findAll(page, size);
        return ResponseEntity.ok(humans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Human> getHumanById(@PathVariable Long id) {
        Optional<Human> human = humanService.findById(id);
        return human.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Human> createHuman(@Valid @RequestBody Human human) {
        Human savedHuman = humanService.save(human);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedHuman);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Human> updateHuman(@PathVariable Long id, 
                                                      @Valid @RequestBody Human human) {
        Optional<Human> existingHuman = humanService.findById(id);
        if (existingHuman.isPresent()) {
            human.setId(id);
            Human updatedHuman = humanService.save(human);
            return ResponseEntity.ok(updatedHuman);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHuman(@PathVariable Long id) {
        Optional<Human> human = humanService.findById(id);
        if (human.isPresent()) {
            humanService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Human>> searchByName(@RequestParam String name) {
        List<Human> humans = humanService.findByNameContaining(name);
        return ResponseEntity.ok(humans);
    }

    @GetMapping("/search/soundtrack")
    public ResponseEntity<List<Human>> searchBySoundtrackName(@RequestParam String soundtrackName) {
        List<Human> humans = humanService.findBySoundtrackNameContaining(soundtrackName);
        return ResponseEntity.ok(humans);
    }

    @GetMapping("/search/weapon-type")
    public ResponseEntity<List<Human>> searchByWeaponType(@RequestParam WeaponType weaponType) {
        List<Human> humans = humanService.findByWeaponType(weaponType);
        return ResponseEntity.ok(humans);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCount() {
        long count = humanService.count();
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/weapon-type/{weaponType}")
    public ResponseEntity<Void> deleteAllByWeaponType(@PathVariable WeaponType weaponType) {
        humanService.deleteAllByWeaponType(weaponType);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/max-mood")
    public ResponseEntity<Human> getOneWithMaxMood() {
        Optional<Human> human = humanService.findOneWithMaxMood();
        return human.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/waiting-greater-than")
    public ResponseEntity<List<Human>> getByMinutesOfWaitingGreaterThan(@RequestParam float minutes) {
        List<Human> humans = humanService.findByMinutesOfWaitingGreaterThan(minutes);
        return ResponseEntity.ok(humans);
    }

    @PutMapping("/update-mood-to-gloom")
    public ResponseEntity<Void> updateAllHeroesMoodToGloom() {
        humanService.updateAllHeroesMoodToGloom();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/assign-red-lada-kalina")
    public ResponseEntity<Void> updateAllHeroesWithoutCarToRedLadaKalina() {
        humanService.updateAllHeroesWithoutCarToRedLadaKalina();
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importHumans(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mappings") String mappingsJson) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> mappings = objectMapper.readValue(mappingsJson,
                    new TypeReference<Map<String, String>>() {});

            importService.importHumansFromCsv(file, mappings);
            return ResponseEntity.ok("Импорт выполнен успешно");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка импорта: " + e.getMessage());
        }
    }
}
