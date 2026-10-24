package ru.itmo.soa.movie.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.LocalDate;

/**
 * Агрегат фильма (корень агрегата Movie Collection Service).
 *
 * <p>Была record; переписана в immutable-класс (Java 11) в стиле Lombok {@code @Value}.</p>
 */
@Value
@AllArgsConstructor
public class Movie {

    long id;

    String name;

    Coordinates coordinates;

    LocalDate creationDate;

    Integer oscarsCount;

    Float totalBoxOffice;

    String tagline;

    MovieGenre genre;

    Person director;
}
