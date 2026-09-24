package ru.tbank.soa.oscar.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Создаёт WebClient для вызова movie-service. Задаёт базовый URL и таймаут ответа,
 * чтобы у клиента мог возникнуть таймаут чтения (маппится в 504).
 */
@Configuration
public class WebClientConfig {

    @Bean(name = "movieServiceWebClient")
    public WebClient movieServiceWebClient(MovieServiceProperties props) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(5));
        return WebClient.builder()
                .baseUrl(props.url())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}