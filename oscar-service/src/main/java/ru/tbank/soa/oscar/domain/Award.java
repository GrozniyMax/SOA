package ru.tbank.soa.oscar.domain;

/**
 * Оскарная запись фильма.
 *
 * @param id          идентификатор записи
 * @param filmId      идентификатор фильма
 * @param oscarsCount количество оскаров
 * @param genre       жанр фильма
 * @param director    режиссёр фильма
 */
public record Award(
        Long id,
        Long filmId,
        int oscarsCount,
        MovieGenre genre,
        String director) {
}