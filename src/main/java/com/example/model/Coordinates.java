package com.example.model;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "coordinates")
public class Coordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "x", nullable = false)
    @NotNull(message = "X coordinate cannot be null")
    @Max(value = 147, message = "X coordinate cannot be greater than 147")
    private Float x;

    @Column(name = "y", nullable = false)
    @Min(value = -772, message = "Y coordinate must be greater than -773")
    private long y;

    public Coordinates() {}

    public Coordinates(Float x, long y) {
        this.x = x;
        this.y = y;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getX() {
        return x;
    }

    public void setX(Float x) {
        this.x = x;
    }

    public long getY() {
        return y;
    }

    public void setY(long y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "Coordinates{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
