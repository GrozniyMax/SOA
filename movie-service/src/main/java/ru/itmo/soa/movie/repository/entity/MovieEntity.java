package ru.itmo.soa.movie.repository.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ru.itmo.soa.movie.domain.MovieGenre;

import java.time.LocalDate;

/**
 * JPA-сущность фильма (таблица movie).
 * Координаты и режиссёр — отдельные таблицы, на которые фильм ссылается через {@code @ManyToOne}.
 * Перечисление {@link MovieGenre} переиспользуется из домена.
 */
@Entity
@Table(name = "movie")
@Getter
@Setter
public class MovieEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coordinates_id", nullable = false)
    private CoordinatesEntity coordinates;

    @Column(nullable = false)
    private LocalDate creationDate;

    private Integer oscarsCount;

    private Float totalBoxOffice;

    private String tagline;

    @Enumerated(EnumType.STRING)
    private MovieGenre genre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "director_id", nullable = false)
    private PersonEntity director;
}