package ru.tbank.soa.oscar.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.tbank.soa.oscar.domain.MovieGenre;
import ru.tbank.soa.oscar.service.exception.MovieServiceBadGatewayException;
import ru.tbank.soa.oscar.service.exception.MovieServiceGatewayTimeoutException;
import ru.tbank.soa.oscar.service.exception.MovieServiceUnavailableException;

import java.net.ConnectException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Реализация {@link MovieServiceClient} поверх блокирующего WebClient,
 * вызывающего {@code POST /movies/search} movie-service.
 * Ошибки сети/статуса маппятся в доменные исключения (503/504/502).
 */
@Component
public class WebMovieServiceClient implements MovieServiceClient {

    private static final String SEARCH_PATH = "/movies/search";

    private final WebClient webClient;

    public WebMovieServiceClient(WebClient movieServiceWebClient) {
        this.webClient = movieServiceWebClient;
    }

    @Override
    public Set<String> findDirectorsByGenre(MovieGenre genre) {
        MovieSearchRequest body = new MovieSearchRequest(
                List.of(new Filter("genre", genre.name(), "eq")),
                null,
                0,
                100);

        try {
            MovieSearchResponse response = webClient.post()
                    .uri(SEARCH_PATH)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(errorBody -> new MovieServiceBadGatewayException(
                                            "movie-service responded with status " + clientResponse.statusCode()
                                                    + ": " + errorBody)))
                    .bodyToMono(MovieSearchResponse.class)
                    .block();

            if (response == null || response.items() == null || response.items().isEmpty()) {
                return Collections.emptySet();
            }
            return response.items().stream()
                    .map(MovieItem::director)
                    .filter(d -> d != null && d.name() != null)
                    .map(MovieDirector::name)
                    .collect(Collectors.toSet());
        } catch (MovieServiceBadGatewayException e) {
            throw e;
        } catch (Exception e) {
            throw mapError(e);
        }
    }

    private RuntimeException mapError(Exception e) {
        Throwable cause = unwrap(e);
        if (cause instanceof TimeoutException) {
            return new MovieServiceGatewayTimeoutException(
                    "Timeout while calling movie-service /movies/search", e);
        }
        if (cause instanceof ConnectException) {
            return new MovieServiceUnavailableException(
                    "movie-service is unavailable (connection refused)", e);
        }
        return new MovieServiceBadGatewayException(
                "Unexpected error while calling movie-service /movies/search", e);
    }

    private Throwable unwrap(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null
                && current.getCause() != current
                && !(current instanceof TimeoutException)
                && !(current instanceof ConnectException)) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * Запрос поиска фильмов.
     */
    private record MovieSearchRequest(List<Filter> filters, Object sort, int page, int size) {
    }

    private record Filter(String path, String value, String comparator) {
    }

    /**
     * Ответ поиска фильмов — захватываем только то, что нужно.
     */
    private record MovieSearchResponse(List<MovieItem> items) {
    }

    private record MovieItem(MovieDirector director) {
    }

    private record MovieDirector(String name) {
    }
}