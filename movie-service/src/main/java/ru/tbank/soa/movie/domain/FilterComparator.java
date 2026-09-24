package ru.tbank.soa.movie.domain;

/**
 * Оператор сравнения для фильтра динамического поиска.
 * Назван FilterComparator, а не Comparator, чтобы не путать с java.util.Comparator.
 */
public enum FilterComparator {
    GT,
    LT,
    EQ,
    SUBSTRING
}