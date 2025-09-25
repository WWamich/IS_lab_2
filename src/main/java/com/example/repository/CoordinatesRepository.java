package com.example.repository;

import com.example.model.Coordinates;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CoordinatesRepository {

    @Autowired
    private SessionFactory sessionFactory;

    public List<Coordinates> findAll() {
        Session session = sessionFactory.getCurrentSession();
        Query<Coordinates> query = session.createQuery("FROM Coordinates", Coordinates.class);
        return query.list();
    }

    public Optional<Coordinates> findById(Long id) {
        Session session = sessionFactory.getCurrentSession();
        Coordinates coordinates = session.get(Coordinates.class, id);
        return Optional.ofNullable(coordinates);
    }

    public Coordinates save(Coordinates coordinates) {
        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(coordinates);
        return coordinates;
    }

    public void deleteById(Long id) {
        Session session = sessionFactory.getCurrentSession();
        Coordinates coordinates = session.get(Coordinates.class, id);
        if (coordinates != null) {
            session.delete(coordinates);
        }
    }
}
