package com.example.repository;

import com.example.model.Human;
import com.example.model.Car;
import com.example.model.Mood;
import com.example.model.WeaponType;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class HumanRepository {

    @Autowired
    private SessionFactory sessionFactory;

    public List<Human> findAll() {
        Session session = sessionFactory.getCurrentSession();
        Query<Human> query = session.createQuery("FROM Human", Human.class);
        return query.list();
    }

    public List<Human> findAll(int page, int size) {
        Session session = sessionFactory.getCurrentSession();
        Query<Human> query = session.createQuery("FROM Human", Human.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.list();
    }

    public Optional<Human> findById(Long id) {
        Session session = sessionFactory.getCurrentSession();
        Human human = session.get(Human.class, id);
        return Optional.ofNullable(human);
    }

    public Human save(Human human) {
        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(human);
        return human;
    }

    public void deleteById(Long id) {
        Session session = sessionFactory.getCurrentSession();
        Human human = session.get(Human.class, id);
        if (human != null) {
            session.delete(human);
        }
    }

    public List<Human> findByNameContaining(String name) {
        Session session = sessionFactory.getCurrentSession();
        Query<Human> query = session.createQuery(
            "FROM Human h WHERE h.name LIKE :name", Human.class);
        query.setParameter("name", "%" + name + "%");
        return query.list();
    }

    public List<Human> findBySoundtrackNameContaining(String soundtrackName) {
        Session session = sessionFactory.getCurrentSession();
        Query<Human> query = session.createQuery(
            "FROM Human h WHERE h.soundtrackName LIKE :soundtrackName", Human.class);
        query.setParameter("soundtrackName", "%" + soundtrackName + "%");
        return query.list();
    }

    public List<Human> findByWeaponType(WeaponType weaponType) {
        Session session = sessionFactory.getCurrentSession();
        Query<Human> query = session.createQuery(
            "FROM Human h WHERE h.weaponType = :weaponType", Human.class);
        query.setParameter("weaponType", weaponType);
        return query.list();
    }

    public void deleteByWeaponType(WeaponType weaponType) {
        Session session = sessionFactory.getCurrentSession();
        Query<?> query = session.createQuery(
            "DELETE FROM Human h WHERE h.weaponType = :weaponType");
        query.setParameter("weaponType", weaponType);
        query.executeUpdate();
    }

    public Optional<Human> findOneWithMaxMood() {
        Session session = sessionFactory.getCurrentSession();
        Query<Human> query = session.createQuery(
            "FROM Human h WHERE h.mood = (SELECT MAX(h2.mood) FROM Human h2 WHERE h2.mood IS NOT NULL)", 
            Human.class);
        query.setMaxResults(1);
        List<Human> results = query.list();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Human> findByMinutesOfWaitingGreaterThan(float minutes) {
        Session session = sessionFactory.getCurrentSession();
        Query<Human> query = session.createQuery(
            "FROM Human h WHERE h.minutesOfWaiting > :minutes", Human.class);
        query.setParameter("minutes", minutes);
        return query.list();
    }

    public void updateAllMoodToGloom() {
        Session session = sessionFactory.getCurrentSession();
        Query<?> query = session.createQuery(
            "UPDATE Human h SET h.mood = :mood WHERE h.realHero = true");
        query.setParameter("mood", Mood.GLOOM);
        query.executeUpdate();
    }

    public void updateAllHeroesWithoutCarToRedLadaKalina() {
        Session session = sessionFactory.getCurrentSession();
        
        Query<Human> carQuery = session.createQuery(
            "FROM Human h WHERE h.car.name = 'Lada Kalina' AND h.car.cool = false", 
            Human.class);
        List<Human> existingCarOwners = carQuery.list();
        
        if (!existingCarOwners.isEmpty()) {
            Car redLadaKalina = existingCarOwners.get(0).getCar();
            
            Query<?> updateQuery = session.createQuery(
                "UPDATE Human h SET h.car = :car WHERE h.realHero = true AND h.car IS NULL");
            updateQuery.setParameter("car", redLadaKalina);
            updateQuery.executeUpdate();
        } else {
            Car redLadaKalina = new Car("Lada Kalina", false);
            session.save(redLadaKalina);
            
            Query<?> updateQuery = session.createQuery(
                "UPDATE Human h SET h.car = :car WHERE h.realHero = true AND h.car IS NULL");
            updateQuery.setParameter("car", redLadaKalina);
            updateQuery.executeUpdate();
        }
    }

    public long count() {
        Session session = sessionFactory.getCurrentSession();
        Query<Long> query = session.createQuery("SELECT COUNT(*) FROM Human", Long.class);
        return query.uniqueResult();
    }
}
