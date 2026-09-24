package ru.tbank.soa.movie.domain;

import java.time.LocalDate;

/**
 * Персона (режиссёр фильма) — значение-объект.
 *
 * @param name     имя (обязательно, не пустое)
 * @param birthday дата рождения (может отсутствовать)
 * @param eyeColor цвет глаз (может отсутствовать)
 * @param hairColor цвет волос (может отсутствовать)
 */
public record Person(String name, LocalDate birthday, EyeColor eyeColor, HairColor hairColor) {
}