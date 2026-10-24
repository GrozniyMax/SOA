package ru.itmo.soa.oscar.config;

import feign.Contract;
import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Конфигурация Feign-клиента для movie-service. Специально НЕ помечена
 * {@code @Configuration}: используется только как per-client конфигурация
 * {@code @FeignClient(configuration = ...)}, чтобы бины не применялись глобально.
 *
 * <p>Connect timeout 3000 мс, read timeout 5000 мс, опциональный truststore
 * из {@link MovieServiceProperties}.</p>
 */
public class MovieFeignConfiguration {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 5000;

    /**
     * Сгенерированный Feign-интерфейс использует нативные аннотации Feign
     * ({@code @RequestLine}/{@code @Headers}), поэтому нужен дефолтный контракт
     * вместо {@code SpringMvcContract}.
     */
    @Bean
    public Contract feignContract() {
        return new Contract.Default();
    }

    /**
     * Таймауты: 3000 мс на соединение, 5000 мс на ответ.
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS,
                READ_TIMEOUT_MS, TimeUnit.MILLISECONDS, true);
    }

    /**
     * OkHttp-транспорт с опциональным TLS truststore.
     */
    @Bean
    public OkHttpClient okHttpClient(MovieServiceProperties props)
            throws GeneralSecurityException, IOException {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS))
                .readTimeout(Duration.ofMillis(READ_TIMEOUT_MS));
        if (StringUtils.hasText(props.getTruststore())) {
            TrustManagerFactory factory = trustManagerFactory(props);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, factory.getTrustManagers(), new SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) factory.getTrustManagers()[0]);
        }
        return builder.build();
    }

    @Bean
    public feign.okhttp.OkHttpClient feignOkHttpClient(OkHttpClient client) {
        return new feign.okhttp.OkHttpClient(client);
    }

    @Bean
    public FeignErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    /**
     * Транспортные IOException (таймаут, отказ соединения) Feign заворачивает в
     * {@link RetryableException} и отдаёт Retryer'у; переводим их в доменные
     * исключения без повторов.
     */
    @Bean
    public Retryer retryer(FeignErrorDecoder decoder) {
        return new Retryer() {
            @Override
            public void continueOrPropagate(RetryableException e) {
                throw decoder.decode(e);
            }

            @Override
            public Retryer clone() {
                return this;
            }
        };
    }

    private static TrustManagerFactory trustManagerFactory(MovieServiceProperties props)
            throws GeneralSecurityException, IOException {
        char[] password = props.getTruststorePassword() == null
                ? new char[0]
                : props.getTruststorePassword().toCharArray();
        KeyStore trustStore = KeyStore.getInstance(new File(props.getTruststore()), password);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trustStore);
        return factory;
    }
}
