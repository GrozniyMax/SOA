package ru.itmo.soa.oscar.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import ru.itmo.soa.oscar.service.exception.MovieServiceBadGatewayException;
import ru.itmo.soa.oscar.service.exception.MovieServiceGatewayTimeoutException;
import ru.itmo.soa.oscar.service.exception.MovieServiceUnavailableException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return new MovieServiceBadGatewayException(
                "movie-service responded with status " + response.status()
                        + " on " + methodKey + ": " + body(response));
    }

    public RuntimeException decode(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof SocketTimeoutException
                || cause instanceof TimeoutException) {
            return new MovieServiceGatewayTimeoutException(
                    "Timeout while calling movie-service", throwable);
        }
        if (cause instanceof ConnectException) {
            return new MovieServiceUnavailableException(
                    "movie-service is unavailable (connection refused)", throwable);
        }
        return new MovieServiceBadGatewayException(
                "Unexpected error while calling movie-service", throwable);
    }

    private Throwable unwrap(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null
                && current.getCause() != current
                && !(current instanceof SocketTimeoutException)
                && !(current instanceof TimeoutException)
                && !(current instanceof ConnectException)) {
            current = current.getCause();
        }
        return current;
    }

    private String body(Response response) {
        if (response.body() == null) {
            return "";
        }
        try (var in = response.body().asInputStream()) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<unreadable body: " + e.getMessage() + ">";
        }
    }
}
