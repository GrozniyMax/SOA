package ru.itmo.soa.oscar.client;

import feign.Headers;
import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;
import ru.itmo.soa.movie.dto.DirectorsHumiliateRequest;
import ru.itmo.soa.movie.dto.DirectorsHumiliationResult;
import ru.itmo.soa.movie.dto.MoviePage;
import ru.itmo.soa.movie.dto.MovieSearchRequest;
import ru.itmo.soa.oscar.config.MovieFeignConfiguration;

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
