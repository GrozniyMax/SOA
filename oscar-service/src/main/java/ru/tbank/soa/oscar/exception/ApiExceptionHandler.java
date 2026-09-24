package ru.tbank.soa.oscar.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.tbank.soa.oscar.dto.ApiError;
import ru.tbank.soa.oscar.service.exception.MovieServiceBadGatewayException;
import ru.tbank.soa.oscar.service.exception.MovieServiceGatewayTimeoutException;
import ru.tbank.soa.oscar.service.exception.MovieServiceUnavailableException;

import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений: маппит ошибки в {@link ApiError} и корректные HTTP-коды
 * в соответствии со спецификацией (400/405/406/415/422/500/502/503/504).
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MovieServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleServiceUnavailable(MovieServiceUnavailableException e) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(MovieServiceGatewayTimeoutException.class)
    public ResponseEntity<ApiError> handleGatewayTimeout(MovieServiceGatewayTimeoutException e) {
        return build(HttpStatus.GATEWAY_TIMEOUT, e.getMessage());
    }

    @ExceptionHandler(MovieServiceBadGatewayException.class)
    public ResponseEntity<ApiError> handleBadGateway(MovieServiceBadGatewayException e) {
        return build(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            HttpMessageNotReadableException.class,
            BindException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> handleNotAcceptable(HttpMediaTypeNotAcceptableException e) {
        return build(HttpStatus.NOT_ACCEPTABLE, e.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception e) {
        log.error("Unhandled exception", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message));
    }
}