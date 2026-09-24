package ru.tbank.soa.oscar.domain;

/**
 * Оператор, работающий с фильмами.
 *
 * @param id   идентификатор оператора
 * @param name имя оператора
 */
public record Operator(
        Long id,
        String name) {
}