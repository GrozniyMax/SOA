package ru.itmo.soa.oscar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "movie.humiliate")
public class HumiliateProperties {

    private int batchSize = 10;
}
