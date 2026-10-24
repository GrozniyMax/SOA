package ru.itmo.soa.movie.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Координаты фильма (значение-объект).
 *
 * <p>Была record; переписана в immutable-класс (Java 11) в стиле Lombok {@code @Value}.</p>
 */
@Value
@AllArgsConstructor
public class Coordinates {

    int x;

    float y;
}
