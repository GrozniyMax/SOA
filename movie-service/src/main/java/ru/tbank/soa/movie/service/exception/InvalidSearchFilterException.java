package ru.tbank.soa.movie.service.exception;

public class InvalidSearchFilterException extends RuntimeException {

    public InvalidSearchFilterException(String message) {
        super(message);
    }
}
