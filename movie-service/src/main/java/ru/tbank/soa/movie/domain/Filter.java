package ru.tbank.soa.movie.domain;

/**
 * Один фильтр динамического поиска.
 *
 * @param path       путь к полю сущности
 * @param value      значение фильтра (строка)
 * @param comparator оператор сравнения
 */
public record Filter(MovieFieldPath path, String value, FilterComparator comparator) {
}