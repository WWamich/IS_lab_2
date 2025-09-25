package com.example.repository;

import com.example.model.Car;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CarRepository {

    @Autowired
    private SessionFactory sessionFactory;

    public List<Car> findAll() {
        Session session = sessionFactory.getCurrentSession();
        Query<Car> query = session.createQuery("FROM Car", Car.class);
        return query.list();
    }

    public Optional<Car> findById(Long id) {
        Session session = sessionFactory.getCurrentSession();
        Car car = session.get(Car.class, id);
        return Optional.ofNullable(car);
    }

    public Car save(Car car) {
        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(car);
        return car;
    }

    public void deleteById(Long id) {
        Session session = sessionFactory.getCurrentSession();
        Car car = session.get(Car.class, id);
        if (car != null) {
            session.delete(car);
        }
    }

    public List<Car> findByNameContaining(String name) {
        Session session = sessionFactory.getCurrentSession();
        Query<Car> query = session.createQuery(
            "FROM Car c WHERE c.name LIKE :name", Car.class);
        query.setParameter("name", "%" + name + "%");
        return query.list();
    }
}
