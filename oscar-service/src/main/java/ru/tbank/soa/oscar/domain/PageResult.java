package ru.tbank.soa.oscar.domain;

import java.util.List;

/**
 * Универсальный результат пагинации.
 *
 * @param items список элементов страницы
 * @param total общее количество элементов
 * @param page  номер страницы
 * @param size  размер страницы
 * @param <T>   тип элементов
 */
public record PageResult<T>(List<T> items, long total, int page, int size) {
}