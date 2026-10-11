package ru.itmo.soa.oscar.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;

/**
 * Создаёт WebClient для вызова movie-service. Задаёт базовый URL и таймаут ответа,
 * чтобы у клиента мог возникнуть таймаут чтения (маппится в 504).
 */
@Configuration
public class WebClientConfig {

    @Bean(name = "movieServiceWebClient")
    public WebClient movieServiceWebClient(MovieServiceProperties props) throws GeneralSecurityException, IOException {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(5));
        if (StringUtils.hasText(props.truststore())) {
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(trustManagerFactory(props))
                    .build();
            httpClient = httpClient.secure(spec -> spec.sslContext(sslContext));
        }
        return WebClient.builder()
                .baseUrl(props.url())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private static TrustManagerFactory trustManagerFactory(MovieServiceProperties props)
            throws GeneralSecurityException, IOException {
        char[] password = props.truststorePassword() == null ? new char[0] : props.truststorePassword().toCharArray();
        KeyStore trustStore = KeyStore.getInstance(new File(props.truststore()), password);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trustStore);
        return factory;
    }
}
