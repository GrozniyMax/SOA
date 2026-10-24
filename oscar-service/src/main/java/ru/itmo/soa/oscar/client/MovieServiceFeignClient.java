package ru.itmo.soa.oscar.client;

import feign.Headers;
import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;
import ru.itmo.soa.movie.dto.DirectorsHumiliateRequest;
import ru.itmo.soa.movie.dto.DirectorsHumiliationResult;
import ru.itmo.soa.movie.dto.MoviePage;
import ru.itmo.soa.movie.dto.MovieSearchRequest;
import ru.itmo.soa.oscar.config.MovieFeignConfiguration;

/**
 * Feign-клиент movie-service, собранный вручную поверх сгенерированных DTO
 * (спецификация movie-service.yaml, генератор feign).
 *
 * <p>Hoxton-версия spring-cloud-openfeign поддерживает только «одноуровневое
 * наследование»: {@code @FeignClient}-интерфейс не может наследовать методы
 * у родительского интерфейса (сгенерированный {@code MoviesApi} сам наследует
 * {@code ApiClient.Api}). Поэтому нужные методы переобъявлены здесь напрямую
 * с теми же нативными feign-аннотациями, что и в сгенерированном коде;
 * контракт — {@link feign.Contract.Default} из {@link MovieFeignConfiguration}.</p>
 *
 * <p>Базовый URL задаётся настройкой {@code movie.service.url}. Используется
 * для всех межсервисных вызовов movie-service (поиск фильмов и отзыв Оскаров
 * у режиссёров).</p>
 */
@FeignClient(
        name = "movie-service",
        url = "${movie.service.url}",
        configuration = MovieFeignConfiguration.class)
public interface MovieServiceFeignClient {

    @RequestLine("POST /movies/search")
    @Headers({
            "Content-Type: application/json",
            "Accept: application/json",
    })
    MoviePage searchMovies(MovieSearchRequest movieSearchRequest);

    @RequestLine("POST /movies/directors/humiliate")
    @Headers({
            "Content-Type: application/json",
            "Accept: application/json",
    })
    DirectorsHumiliationResult humiliateDirectors(DirectorsHumiliateRequest directorsHumiliateRequest);
}
