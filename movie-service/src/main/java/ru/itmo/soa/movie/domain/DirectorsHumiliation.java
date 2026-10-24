package ru.itmo.soa.movie.domain;

/**
 * Количество Оскаров, отозванных у фильмов одного режиссёра.
 *
 * @param director      имя режиссёра
 * @param revokedOscars суммарное количество отозванных Оскаров
 */
public record DirectorsHumiliation(String director, int revokedOscars) {
}
