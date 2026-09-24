package ru.tbank.soa.movie.domain;

/**
 * Координаты фильма (значение-объект).
 *
 * @param x координата x (обязательна)
 * @param y координата y (обязательна)
 */
public record Coordinates(int x, float y) {
}