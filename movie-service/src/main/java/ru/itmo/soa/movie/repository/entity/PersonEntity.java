package ru.itmo.soa.movie.repository.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ru.itmo.soa.movie.domain.EyeColor;
import ru.itmo.soa.movie.domain.HairColor;

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