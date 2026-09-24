package ru.tbank.soa.movie.service.exception;

/**
 * Исключение о том, что фильм не найден.
 * Несёт id ненайденного фильма для последующего маппинга в HTTP 404.
 */
public class MovieNotFoundException extends RuntimeException {

    private final long id;

    public MovieNotFoundException(long id) {
        super("Movie not found with id: " + id);
        this.id = id;
    }

    public MovieNotFoundException(String message) {
        super(message);
        this.id = -1L;
    }

    public long getId() {
        return id;
    }
}