package com.example.service;

import com.example.model.Car;
import com.example.model.Human;
import com.example.model.Mood;
import com.example.model.WeaponType;
import com.example.repository.HumanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class HumanService {
    @Autowired
    private HumanRepository humanRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private void notifyClients() {
        messagingTemplate.convertAndSend("/topic/humans", "update");
    }

    public Page<Human> findAllWithFilters(String name, String soundtrackName, Pageable pageable) {
        return humanRepository.findAllWithFilters(name, soundtrackName, pageable);
    }
    
    public Optional<Human> findById(Long id) {
        return humanRepository.findById(id);
    }

    public Human save(Human human) {
        if (human.getId() == null) {
            if (humanRepository.existsByNameAndSoundtrackName(human.getName(), human.getSoundtrackName())) {
                throw new IllegalArgumentException("Объект с именем '" + human.getName() + "' и саундтреком '" + human.getSoundtrackName() + "' уже существует!");
            }
        } else {
            if (humanRepository.existsByNameAndSoundtrackNameAndIdNot(human.getName(), human.getSoundtrackName(), human.getId())) {
                throw new IllegalArgumentException("Объект с именем '" + human.getName() + "' и саундтреком '" + human.getSoundtrackName() + "' уже существует!");
            }
        }

        Human savedHuman = humanRepository.save(human);
        notifyClients();
        return savedHuman;
    }

    public void deleteById(Long id) {
        humanRepository.deleteById(id);
        notifyClients(); 
    }
    public void deleteAllByWeaponType(WeaponType weaponType) {
        humanRepository.deleteAllByWeaponType(weaponType);
        notifyClients(); 
    }

    public Optional<Human> findOneWithMaxMood() {
        List<Human> humans = humanRepository.findTopByMaxMood();
        return humans.isEmpty() ? Optional.empty() : Optional.of(humans.get(0));
    }
    
    public List<Human> findByMinutesOfWaitingGreaterThan(float minutes) {
        return humanRepository.findByMinutesOfWaitingGreaterThan(minutes);
    }

    public void updateAllHeroesMoodToGloom() {
        humanRepository.updateAllHeroesMoodToGloom(Mood.GLOOM);
        notifyClients(); 
    }
    
    @Transactional
    public void updateAllHeroesWithoutCarToRedLadaKalina() {
        Car redLada = new Car("Lada Kalina", false);
        List<Human> heroesWithoutCar = humanRepository.findAllHeroesWithoutCar();
        for (Human hero : heroesWithoutCar) {
            hero.setCar(redLada);
        }
        humanRepository.saveAll(heroesWithoutCar);
        notifyClients(); 
    }
    
    public List<Human> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return humanRepository.findAll(pageable).getContent();
    }

    public List<Human> findByNameContaining(String name) {
        return humanRepository.findByNameContainingIgnoreCase(name);
    }
    
    public List<Human> findBySoundtrackNameContaining(String soundtrackName) {
        return humanRepository.findBySoundtrackNameContainingIgnoreCase(soundtrackName);
    }

    public List<Human> findByWeaponType(WeaponType weaponType) {
        return humanRepository.findByWeaponType(weaponType);
    }

    public long count() {
        return humanRepository.count();
    }
}