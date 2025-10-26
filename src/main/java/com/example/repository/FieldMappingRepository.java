package com.example.repository;

import com.example.model.FieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FieldMappingRepository extends JpaRepository<FieldMapping, Long> {

    Optional<FieldMapping> findBySourceFieldName(String sourceFieldName);

    List<FieldMapping> findByLastUsedAfter(LocalDate date);

    @Transactional
    @Modifying
    @Query("UPDATE FieldMapping fm SET fm.usageCount = fm.usageCount + 1, fm.lastUsed = :now WHERE fm.sourceFieldName = :sourceFieldName")
    void incrementUsageCount(@Param("sourceFieldName") String sourceFieldName, @Param("now") LocalDate now);

    List<FieldMapping> findByUsageCountGreaterThanOrderByUsageCountDesc(Integer minUsageCount);
}