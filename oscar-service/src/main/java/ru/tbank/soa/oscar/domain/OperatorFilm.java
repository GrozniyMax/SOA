package ru.tbank.soa.oscar.domain;

/**
 * Связь оператора с фильмом.
 *
 * @param id         идентификатор связи
 * @param operatorId идентификатор оператора
 * @param filmId     идентификатор фильма
 */
public record OperatorFilm(
        Long id,
        Long operatorId,
        Long filmId) {
}