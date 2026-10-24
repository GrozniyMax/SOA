package ru.itmo.soa.oscar.config;

import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import org.junit.jupiter.api.Test;
import ru.itmo.soa.oscar.service.exception.MovieServiceBadGatewayException;
import ru.itmo.soa.oscar.service.exception.MovieServiceGatewayTimeoutException;
import ru.itmo.soa.oscar.service.exception.MovieServiceUnavailableException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class FeignErrorDecoderTest {

    private final FeignErrorDecoder decoder = new FeignErrorDecoder();

    @Test
    void timeoutMapsToGatewayTimeout() {
        assertThat(decoder.decode(new TimeoutException("read timed out")))
                .isInstanceOf(MovieServiceGatewayTimeoutException.class);
    }

    @Test
    void socketTimeoutMapsToGatewayTimeout() {
        assertThat(decoder.decode(new SocketTimeoutException("timeout")))
                .isInstanceOf(MovieServiceGatewayTimeoutException.class);
    }

    @Test
    void connectRefusedMapsToUnavailable() {
        assertThat(decoder.decode(new ConnectException("refused")))
                .isInstanceOf(MovieServiceUnavailableException.class);
    }

    @Test
    void unexpectedErrorMapsToBadGateway() {
        assertThat(decoder.decode(new IllegalStateException("boom")))
                .isInstanceOf(MovieServiceBadGatewayException.class);
    }

    @Test
    void retryableSocketTimeoutMapsToGatewayTimeout() {
        RetryableException e = retryable(new SocketTimeoutException("read timed out"));

        assertThat(decoder.decode(e)).isInstanceOf(MovieServiceGatewayTimeoutException.class);
    }

    @Test
    void retryableConnectRefusedMapsToUnavailable() {
        RetryableException e = retryable(new ConnectException("refused"));

        assertThat(decoder.decode(e)).isInstanceOf(MovieServiceUnavailableException.class);
    }

    @Test
    void retryerPropagatesDecodedDomainException() {
        Retryer retryer = new MovieFeignConfiguration().retryer(decoder);
        RetryableException e = retryable(new SocketTimeoutException("read timed out"));

        assertThatExceptionOfType(MovieServiceGatewayTimeoutException.class)
                .isThrownBy(() -> retryer.continueOrPropagate(e));
    }

    @Test
    void http500ResponseMapsToBadGateway() {
        feign.Response response = feign.Response.builder()
                .status(500)
                .reason("Internal Server Error")
                .request(feign.Request.create(
                        feign.Request.HttpMethod.GET, "/movies/1", java.util.Map.of(), null, null, null))
                .body("{\"message\":\"oops\"}", java.nio.charset.StandardCharsets.UTF_8)
                .build();

        Exception e = decoder.decode("MoviesApi#getMovie", response);

        assertThat(e).isInstanceOf(MovieServiceBadGatewayException.class);
        assertThat(e.getMessage()).contains("500").contains("MoviesApi#getMovie").contains("oops");
    }

    /** Как Feign заворачивает транспортные IOException при OkHttp-стеке (feign 10.x, Hoxton). */
    private static RetryableException retryable(Throwable cause) {
        return new RetryableException(
                -1,
                cause.getMessage(),
                Request.HttpMethod.GET,
                cause,
                (java.util.Date) null,
                Request.create(Request.HttpMethod.GET, "/movies/1", java.util.Map.of(), null, null, null));
    }
}
