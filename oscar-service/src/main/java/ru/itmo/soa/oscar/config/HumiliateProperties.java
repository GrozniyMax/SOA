package ru.itmo.soa.oscar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки операции отзыва Оскаров у режиссёров (префикс {@code movie.humiliate}).
 *
 * <p>Была record; в Boot 2.3 привязка JavaBean, дефолт задаётся значением поля.</p>
 */
@Data
@ConfigurationProperties(prefix = "movie.humiliate")
public class HumiliateProperties {

    private int batchSize = 10;
}
