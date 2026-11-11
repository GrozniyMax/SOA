package ru.itmo.soa.movie.service.exception;

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