package ru.tbank.soa.oscar.domain;

/**
 * Результат отзыва оскаров.
 *
 * @param genre            жанр, по которому производился отзыв
 * @param affectedDirectors количество режиссёров, которых затронул отзыв
 * @param revokedOscars     количество отозванных оскаров
 */
public record HumiliationResult(
        MovieGenre genre,
        int affectedDirectors,
        int revokedOscars) {
}