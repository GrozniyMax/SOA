package ru.itmo.soa.movie.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Количество Оскаров, отозванных у фильмов одного режиссёра.
 *
 * <p>Была record; переписана в immutable-класс (Java 11) в стиле Lombok {@code @Value}.</p>
 */
@Value
@AllArgsConstructor
public class DirectorsHumiliation {

    String director;

    int revokedOscars;
}
