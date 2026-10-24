package ru.itmo.soa.movie.repository.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
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