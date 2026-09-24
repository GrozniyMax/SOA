package ru.tbank.soa.movie.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA-сущность координат фильма (отдельная таблица coordinates).
 * Может разделяться несколькими фильмами; {@code refCount} — число фильмов,
 * ссылающихся на эти координаты. При {@code refCount == 0} координаты удаляются.
 */
@Entity
@Table(name = "coordinates")
@Getter
@Setter
public class CoordinatesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int x;

    @Column(nullable = false)
    private float y;

    @Column(name = "ref_count", nullable = false)
    private int refCount;
}