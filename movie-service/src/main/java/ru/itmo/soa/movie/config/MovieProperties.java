package ru.itmo.soa.movie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;

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

    /**
     * В лабораторной 2 (record) компактный конструктор заменял значения <= 0
     * на дефолты. После миграции на JavaBean привязка 0 проходила молча
     * (например, taglineMaxLength=0 отключал проверку слогана) — восстанавливаем семантику.
     */
    @PostConstruct
    void applyFallbacks() {
        if (defaultPageSize <= 0) defaultPageSize = 20;
        if (maxPageSize <= 0) maxPageSize = 100;
        if (taglineMaxLength <= 0) taglineMaxLength = 181;
    }
}
