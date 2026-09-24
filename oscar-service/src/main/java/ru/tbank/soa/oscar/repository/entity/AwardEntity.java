package ru.tbank.soa.oscar.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ru.tbank.soa.oscar.domain.MovieGenre;

/**
 * JPA-сущность оскарной записи фильма (таблица award).
 * Перечисление {@link MovieGenre} переиспользуется из домена.
 */
@Entity
@Table(name = "award")
@Getter
@Setter
public class AwardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "film_id", nullable = false)
    private Long filmId;

    @Column(name = "oscars_count", nullable = false)
    private int oscarsCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovieGenre genre;

    @Column(nullable = false)
    private String director;
}