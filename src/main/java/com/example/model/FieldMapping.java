package com.example.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "field_mappings")
public class FieldMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_field_name", nullable = false, unique = true)
    private String sourceFieldName;

    @Column(name = "target_entity_field", nullable = false)
    private String targetEntityField;

    @Column(name = "last_used")
    private LocalDate lastUsed;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    public FieldMapping() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSourceFieldName() { return sourceFieldName; }
    public void setSourceFieldName(String sourceFieldName) { this.sourceFieldName = sourceFieldName; }

    public String getTargetEntityField() { return targetEntityField; }
    public void setTargetEntityField(String targetEntityField) { this.targetEntityField = targetEntityField; }

    public LocalDate getLastUsed() { return lastUsed; }
    public void setLastUsed(LocalDate lastUsed) { this.lastUsed = lastUsed; }

    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
}