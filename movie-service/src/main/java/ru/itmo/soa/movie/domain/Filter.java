package ru.itmo.soa.movie.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Один фильтр динамического поиска.
 *
 * <p>Была record; переписана в immutable-класс (Java 11) в стиле Lombok {@code @Value}.</p>
 */
@Value
@AllArgsConstructor
public class Filter {

    MovieFieldPath path;

    String value;

    FilterComparator comparator;
}
