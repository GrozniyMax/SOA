package ru.itmo.soa.movie.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.LocalDate;

/**
 * Персона (режиссёр фильма) — значение-объект.
 *
 * <p>Была record; переписана в immutable-класс (Java 11) в стиле Lombok {@code @Value}.</p>
 */
@Value
@AllArgsConstructor
public class Person {

    String name;

    LocalDate birthday;

    EyeColor eyeColor;

    HairColor hairColor;
}
