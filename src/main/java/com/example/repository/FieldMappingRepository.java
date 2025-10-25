package com.example.repository;

import com.example.model.FieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FieldMappingRepository extends JpaRepository<FieldMapping, Long> {
    Optional<FieldMapping> findBySourceFieldName(String sourceFieldName);
}