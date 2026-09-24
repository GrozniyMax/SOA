package ru.tbank.soa.movie.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ru.tbank.soa.movie.domain.MovieGenre;

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