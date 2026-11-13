package ru.itmo.soa.movie.service.exception;

public class InvalidMovieDataException extends RuntimeException {

    public InvalidMovieDataException(String message) {
        super(message);
    }
}