package ru.itmo.soa.oscar.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Оператор, работающий с фильмами.
 *
 * <p>Была record; переписана в immutable-класс (Java 11) в стиле Lombok {@code @Value}.</p>
 */
@Value
@AllArgsConstructor
public class Operator {

    Long id;

    String name;
}
