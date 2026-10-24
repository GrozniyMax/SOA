package ru.itmo.soa.oscar.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;

/**
 * Универсальный результат пагинации.
 *
 * <p>Была record; переписана в immutable-класс (Java 11) в стиле Lombok {@code @Value}.</p>
 */
@Value
@AllArgsConstructor
public class PageResult<T> {

    List<T> items;

    long total;

    int page;

    int size;
}
