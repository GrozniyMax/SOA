package ru.itmo.soa.oscar.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Связь оператора с фильмом.
 *
 * <p>Была record; переписана в immutable-класс (Java 11) в стиле Lombok {@code @Value}.</p>
 */
@Value
@AllArgsConstructor
public class OperatorFilm {

    Long id;

    Long operatorId;

    Long filmId;
}
