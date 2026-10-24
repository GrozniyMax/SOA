package ru.itmo.soa.oscar.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Результат отзыва оскаров.
 *
 * <p>Была record; переписана в immutable-класс (Java 11) в стиле Lombok {@code @Value}.</p>
 */
@Value
@AllArgsConstructor
public class HumiliationResult {

    MovieGenre genre;

    int affectedDirectors;

    int revokedOscars;
}
