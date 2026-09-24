package ru.tbank.soa.oscar.service.exception;

/**
 * Сервис фильмов недоступен (например, соединение отвергнуто). Маппится в HTTP 503.
 */
public class MovieServiceUnavailableException extends RuntimeException {

    public MovieServiceUnavailableException(String message) {
        super(message);
    }

    public MovieServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}