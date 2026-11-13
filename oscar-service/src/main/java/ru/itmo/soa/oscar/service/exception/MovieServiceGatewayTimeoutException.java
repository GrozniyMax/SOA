package ru.itmo.soa.oscar.service.exception;

public class MovieServiceGatewayTimeoutException extends RuntimeException {

    public MovieServiceGatewayTimeoutException(String message) {
        super(message);
    }

    public MovieServiceGatewayTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}