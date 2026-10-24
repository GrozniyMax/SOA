package ru.itmo.soa.movie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки movie-service (префикс {@code soa.movie}).
 *
 * <p>Была record с компактным конструктором; в Boot 2.3 привязка JavaBean,
 * дефолты задаются значениями полей.</p>
 */
@Data
@ConfigurationProperties(prefix = "soa.movie")
public class MovieProperties {

    private int defaultPageSize = 20;

    private int maxPageSize = 100;

    private int taglineMaxLength = 181;
}
