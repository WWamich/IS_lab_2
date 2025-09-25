package com.example.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "cars")
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    @NotNull(message = "Car name cannot be null")
    private String name;

    @Column(name = "cool", nullable = false)
    @NotNull(message = "Cool field cannot be null")
    private Boolean cool;

    public Car() {}

    public Car(String name, Boolean cool) {
        this.name = name;
        this.cool = cool;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getCool() {
        return cool;
    }

    public void setCool(Boolean cool) {
        this.cool = cool;
    }

    @Override
    public String toString() {
        return "Car{" +
                "name='" + name + '\'' +
                ", cool=" + cool +
                '}';
    }
}
