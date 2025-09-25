package com.example.service;

import com.example.model.Human;
import com.example.model.WeaponType;
import com.example.repository.HumanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class HumanService {

    @Autowired
    private HumanRepository humanRepository;

    public List<Human> findAll() {
        return humanRepository.findAll();
    }

    public List<Human> findAll(int page, int size) {
        return humanRepository.findAll(page, size);
    }

    public Optional<Human> findById(Long id) {
        return humanRepository.findById(id);
    }

    public Human save(Human human) {
        return humanRepository.save(human);
    }

    public void deleteById(Long id) {
        humanRepository.deleteById(id);
    }

    public List<Human> findByNameContaining(String name) {
        return humanRepository.findByNameContaining(name);
    }

    public List<Human> findBySoundtrackNameContaining(String soundtrackName) {
        return humanRepository.findBySoundtrackNameContaining(soundtrackName);
    }

    public List<Human> findByWeaponType(WeaponType weaponType) {
        return humanRepository.findByWeaponType(weaponType);
    }

    public long count() {
        return humanRepository.count();
    }
    
    public void deleteAllByWeaponType(WeaponType weaponType) {
        humanRepository.deleteByWeaponType(weaponType);
    }

    public Optional<Human> findOneWithMaxMood() {
        return humanRepository.findOneWithMaxMood();
    }

    public List<Human> findByMinutesOfWaitingGreaterThan(float minutes) {
        return humanRepository.findByMinutesOfWaitingGreaterThan(minutes);
    }

    public void updateAllHeroesMoodToGloom() {
        humanRepository.updateAllMoodToGloom();
    }

    public void updateAllHeroesWithoutCarToRedLadaKalina() {
        humanRepository.updateAllHeroesWithoutCarToRedLadaKalina();
    }
}
