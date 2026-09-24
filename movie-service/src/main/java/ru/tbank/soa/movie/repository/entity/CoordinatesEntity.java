package ru.tbank.soa.movie.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * JPA-представление координат фильма (встраиваемый value-объект).
 */
@Embeddable
public class CoordinatesEntity {

    @Column(nullable = false)
    private int x;

    @Column(nullable = false)
    private float y;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }
}