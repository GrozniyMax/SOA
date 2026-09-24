package ru.tbank.soa.movie.repository.entity;

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
import ru.tbank.soa.movie.domain.EyeColor;
import ru.tbank.soa.movie.domain.HairColor;

import java.time.LocalDate;

/**
 * JPA-сущность режиссёра (отдельная таблица director).
 * Один режиссёр может быть у многих фильмов ({@code @ManyToOne} из MovieEntity).
 * Перечисления ({@link EyeColor}, {@link HairColor}) переиспользуются из домена.
 */
@Entity
@Table(name = "director")
@Getter
@Setter
public class PersonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    private EyeColor eyeColor;

    @Enumerated(EnumType.STRING)
    private HairColor hairColor;
}