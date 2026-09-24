package ru.tbank.soa.oscar.service.exception;

/**
 * Некорректный ответ от сервиса фильмов (не-2xx статус или ошибка десериализации).
 * Маппится в HTTP 502.
 */
public class MovieServiceBadGatewayException extends RuntimeException {

    public MovieServiceBadGatewayException(String message) {
        super(message);
    }

    public MovieServiceBadGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}