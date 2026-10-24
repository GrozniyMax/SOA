package ru.itmo.soa.movie.service.exception;

public class InvalidSearchFilterException extends RuntimeException {

    public InvalidSearchFilterException(String message) {
        super(message);
    }
}
