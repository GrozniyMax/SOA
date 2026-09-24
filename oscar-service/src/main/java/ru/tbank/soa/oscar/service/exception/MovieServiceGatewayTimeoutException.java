package ru.tbank.soa.oscar.service.exception;

/**
 * Таймаут чтения при обращении к сервису фильмов. Маппится в HTTP 504.
 */
public class MovieServiceGatewayTimeoutException extends RuntimeException {

    public MovieServiceGatewayTimeoutException(String message) {
        super(message);
    }

    public MovieServiceGatewayTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}