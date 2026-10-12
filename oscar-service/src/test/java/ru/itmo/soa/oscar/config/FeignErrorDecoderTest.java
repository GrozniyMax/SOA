package ru.itmo.soa.oscar.config;

import org.junit.jupiter.api.Test;
import ru.itmo.soa.oscar.service.exception.MovieServiceBadGatewayException;
import ru.itmo.soa.oscar.service.exception.MovieServiceGatewayTimeoutException;
import ru.itmo.soa.oscar.service.exception.MovieServiceUnavailableException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeignErrorDecoderTest {

    private final FeignErrorDecoder decoder = new FeignErrorDecoder();

    @Test
    void timeoutMapsToGatewayTimeout() {
        assertThatThrownBy(() -> {
            throw decoder.decode(new TimeoutException("read timed out"));
        })
                .isInstanceOf(MovieServiceGatewayTimeoutException.class);
    }

    @Test
    void socketTimeoutMapsToGatewayTimeout() {
        assertThatThrownBy(() -> {
            throw decoder.decode(new SocketTimeoutException("timeout"));
        })
                .isInstanceOf(MovieServiceGatewayTimeoutException.class);
    }

    @Test
    void connectRefusedMapsToUnavailable() {
        assertThatThrownBy(() -> {
            throw decoder.decode(new ConnectException("refused"));
        })
                .isInstanceOf(MovieServiceUnavailableException.class);
    }

    @Test
    void unexpectedErrorMapsToBadGateway() {
        assertThatThrownBy(() -> {
            throw decoder.decode(new IllegalStateException("boom"));
        })
                .isInstanceOf(MovieServiceBadGatewayException.class);
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
}
