package com.example.repository;

import com.example.model.Human;
import com.example.model.Mood;
import com.example.model.WeaponType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface HumanRepository extends JpaRepository<Human, Long> {

    @Query("SELECT h FROM Human h WHERE " +
            "(:name IS NULL OR :name = '' OR lower(h.name) = lower(:name)) AND " +
            "(:soundtrackName IS NULL OR :soundtrackName = '' OR lower(h.soundtrackName) = lower(:soundtrackName))")
    Page<Human> findAllWithFilters(@Param("name") String name, @Param("soundtrackName") String soundtrackName, Pageable pageable);


    @Transactional
    @Modifying
    void deleteAllByWeaponType(WeaponType weaponType);
    @Query("FROM Human h WHERE h.mood = (SELECT MAX(h2.mood) FROM Human h2 WHERE h2.mood IS NOT NULL)")

    List<Human> findTopByMaxMood();
    @Query("SELECT h FROM Human h WHERE h.minutesOfWaiting > :minutes")
    List<Human> findByMinutesOfWaitingGreaterThan(@Param("minutes") float minutes);

    @Transactional
    @Modifying
    @Query("UPDATE Human h SET h.mood = :mood WHERE h.realHero = true")

    void updateAllHeroesMoodToGloom(@Param("mood") Mood mood); 
    @Query("SELECT h FROM Human h WHERE h.realHero = true AND h.car IS NULL")

    List<Human> findAllHeroesWithoutCar();
    List<Human> findByNameContainingIgnoreCase(String name);
    List<Human> findBySoundtrackNameContainingIgnoreCase(String soundtrackName);
    List<Human> findByWeaponType(WeaponType weaponType);

    boolean existsByNameAndSoundtrackName(String name, String soundtrackName);

    boolean existsByNameAndSoundtrackNameAndIdNot(String name, String soundtrackName, Long id);
}