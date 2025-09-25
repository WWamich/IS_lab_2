package com.example.service;

import com.example.model.Coordinates;
import com.example.repository.CoordinatesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CoordinatesService {

    @Autowired
    private CoordinatesRepository coordinatesRepository;

    public List<Coordinates> findAll() {
        return coordinatesRepository.findAll();
    }

    public Optional<Coordinates> findById(Long id) {
        return coordinatesRepository.findById(id);
    }

    public Coordinates save(Coordinates coordinates) {
        return coordinatesRepository.save(coordinates);
    }

    public void deleteById(Long id) {
        coordinatesRepository.deleteById(id);
    }
}
