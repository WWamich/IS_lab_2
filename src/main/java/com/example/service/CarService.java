package com.example.service;

import com.example.model.Car;
import com.example.repository.CarRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;


import java.util.List;
import java.util.Optional;

@Service
public class CarService {

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private void notifyClients() {
        messagingTemplate.convertAndSend("/topic/humans", "update");
    }

    public List<Car> findAll() {
        return carRepository.findAll();
    }

    public Optional<Car> findById(Long id) {
        return carRepository.findById(id);
    }

    public Car save(Car car) {
        Car savedCar = carRepository.save(car);
        notifyClients();
        return savedCar;
    }

    public void deleteById(Long id) {
        carRepository.deleteById(id);
        notifyClients();
    }

    public List<Car> findByNameContaining(String name) {
        return carRepository.findByNameContainingIgnoreCase(name);
    }
}