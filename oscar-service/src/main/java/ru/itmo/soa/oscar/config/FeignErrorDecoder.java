package ru.itmo.soa.oscar.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import ru.itmo.soa.oscar.service.exception.MovieServiceBadGatewayException;
import ru.itmo.soa.oscar.service.exception.MovieServiceGatewayTimeoutException;
import ru.itmo.soa.oscar.service.exception.MovieServiceUnavailableException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Маппинг ошибок Feign-клиента в доменные исключения oscar-service.
 *
 * <p>Семантика повторяет {@code WebMovieServiceClient.mapError/unwrap}: цепочка причин
 * раскручивается до первой значимой причины, останавливаясь на таймауте или ошибке
 * соединения.</p>
 *
 * <ul>
 *   <li>таймаут (SocketTimeoutException/TimeoutException/ReadTimeoutException) →
 *       {@link MovieServiceGatewayTimeoutException} (HTTP 504);</li>
 *   <li>ошибка соединения (ConnectException) →
 *       {@link MovieServiceUnavailableException} (HTTP 503);</li>
 *   <li>любой не-2xx ответ или неожиданная ошибка →
 *       {@link MovieServiceBadGatewayException} (HTTP 502).</li>
 * </ul>
 */
public class FeignErrorDecoder implements ErrorDecoder {

    /**
     * Вызывается Feign для не-2xx HTTP-ответов movie-service.
     */
    @Override
    public Exception decode(String methodKey, Response response) {
        return new MovieServiceBadGatewayException(
                "movie-service responded with status " + response.status()
                        + " on " + methodKey + ": " + body(response));
    }

    /**
     * RetryableException-aware вариант: таймауты и отказы соединения Feign заворачивает
     * в {@link feign.RetryableException} (внутри — исходный IOException), поэтому
     * отдельная точка входа для транспортных ошибок.
     */
    public RuntimeException decode(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof SocketTimeoutException
                || cause instanceof TimeoutException
                || isNettyReadTimeout(cause)) {
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

    /** {@code ReadTimeoutException} из reactor-netty доступен опционально. */
    private static boolean isNettyReadTimeout(Throwable cause) {
        return "io.netty.handler.timeout.ReadTimeoutException".equals(cause.getClass().getName());
    }

    /**
     * Раскручивает цепочку причин до первого значимого исключения — как в
     * {@code WebMovieServiceClient.unwrap}.
     */
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
