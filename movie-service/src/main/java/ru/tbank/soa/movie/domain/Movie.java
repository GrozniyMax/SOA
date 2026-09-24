package ru.tbank.soa.movie.domain;

import java.time.LocalDate;

/**
 * Агрегат фильма (корень агрегата Movie Collection Service).
 *
 * @param id            идентификатор (автогенерируемый, > 0)
 * @param name          название (не null, не пустое)
 * @param coordinates   координаты (не null)
 * @param creationDate  дата создания (не null, автогенерируемая)
 * @param oscarsCount   количество оскаров (nullable, > 0 при наличии)
 * @param totalBoxOffice общие сборы (nullable, > 0 при наличии)
 * @param tagline       слоган (nullable, длина <= 181)
 * @param genre         жанр (не null)
 * @param director      режиссёр (не null)
 */
public record Movie(
        long id,
        String name,
        Coordinates coordinates,
        LocalDate creationDate,
        Integer oscarsCount,
        Float totalBoxOffice,
        String tagline,
        MovieGenre genre,
        Person director) {
}