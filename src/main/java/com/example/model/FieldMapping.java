package com.example.model;

import javax.persistence.*;

@Entity
@Table(name = "field_mapping", uniqueConstraints = @UniqueConstraint(columnNames = "sourceFieldName"))
public class FieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_field_name", nullable = false, unique = true)
    private String sourceFieldName;

    @Column(name = "target_entity_field", nullable = false)
    private String targetEntityField;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceFieldName() { return sourceFieldName; }
    public void setSourceFieldName(String sourceFieldName) { this.sourceFieldName = sourceFieldName; }
    public String getTargetEntityField() { return targetEntityField; }
    public void setTargetEntityField(String targetEntityField) { this.targetEntityField = targetEntityField; }
}