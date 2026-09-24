package ru.tbank.soa.movie.service.exception;

/**
 * Исключение о нарушении целостности бизнес-данных фильма.
 * Маппится на HTTP 422 на уровне контроллера.
 */
public class InvalidMovieDataException extends RuntimeException {

    public InvalidMovieDataException(String message) {
        super(message);
    }
}