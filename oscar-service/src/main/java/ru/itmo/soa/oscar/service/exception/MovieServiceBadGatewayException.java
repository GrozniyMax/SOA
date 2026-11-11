package ru.itmo.soa.oscar.service.exception;

public class MovieServiceBadGatewayException extends RuntimeException {

    public MovieServiceBadGatewayException(String message) {
        super(message);
    }

    public MovieServiceBadGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}