package ru.itmo.soa.oscar.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.itmo.soa.oscar.domain.Movie;
import ru.itmo.soa.oscar.domain.MovieGenre;
import ru.itmo.soa.oscar.service.exception.MovieServiceBadGatewayException;
import ru.itmo.soa.oscar.service.exception.MovieServiceGatewayTimeoutException;
import ru.itmo.soa.oscar.service.exception.MovieServiceUnavailableException;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Component
public class WebMovieServiceClient implements MovieServiceClient {

    private static final String SEARCH_PATH = "/movies/search";
    private static final String MOVIE_PATH = "/movies/{id}";
    private static final int PAGE_SIZE = 100;
    private static final String SORT = "id:asc";

    private final WebClient webClient;

    public WebMovieServiceClient(WebClient movieServiceWebClient) {
        this.webClient = movieServiceWebClient;
    }

    @Override
    public Set<String> findDirectorsByGenre(MovieGenre genre) {
        return searchAll(List.of(new Filter("genre", genre.name(), "eq"))).stream()
                .map(MovieItem::director)
                .filter(d -> d != null && d.name() != null)
                .map(MovieDirector::name)
                .collect(Collectors.toSet());
    }

    @Override
    public List<Movie> findAwardedMoviesByDirector(String director) {
        return searchAll(List.of(
                new Filter("director.name", director, "eq"),
                new Filter("oscarsCount", "0", "gt"))).stream()
                .map(item -> new Movie(item.id(), item.oscarsCount()))
                .toList();
    }

    @Override
    public Set<Long> findAwardedMovieIds() {
        return searchAll(List.of(new Filter("oscarsCount", "0", "gt"))).stream()
                .map(MovieItem::id)
                .collect(Collectors.toSet());
    }

    @Override
    public void revokeOscars(long movieId) {
        ObjectNode movie = call("GET " + MOVIE_PATH, retrieve(webClient.get()
                .uri(MOVIE_PATH, movieId))
                .bodyToMono(ObjectNode.class));
        if (movie == null) {
            throw new MovieServiceBadGatewayException("movie-service returned empty movie " + movieId);
        }
        movie.remove("id");
        movie.remove("creationDate");
        movie.putNull("oscarsCount");

        call("PUT " + MOVIE_PATH, retrieve(webClient.put()
                .uri(MOVIE_PATH, movieId)
                .bodyValue(movie))
                .toBodilessEntity());
    }

    private List<MovieItem> searchAll(List<Filter> filters) {
        List<MovieItem> items = new ArrayList<>();
        int page = 0;
        while (true) {
            MovieSearchResponse response = call("POST " + SEARCH_PATH, retrieve(webClient.post()
                    .uri(SEARCH_PATH)
                    .bodyValue(new MovieSearchRequest(filters, SORT, page, PAGE_SIZE)))
                    .bodyToMono(MovieSearchResponse.class));
            if (response == null || response.items() == null || response.items().isEmpty()) {
                return items;
            }
            items.addAll(response.items());
            if ((long) (page + 1) * PAGE_SIZE >= response.total()) {
                return items;
            }
            page++;
        }
    }

    private WebClient.ResponseSpec retrieve(WebClient.RequestHeadersSpec<?> request) {
        return request.retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(errorBody -> new MovieServiceBadGatewayException(
                                        "movie-service responded with status " + clientResponse.statusCode()
                                                + ": " + errorBody)));
    }

    private <T> T call(String operation, Mono<T> mono) {
        try {
            return mono.block();
        } catch (MovieServiceBadGatewayException e) {
            throw e;
        } catch (Exception e) {
            throw mapError(operation, e);
        }
    }

    private RuntimeException mapError(String operation, Exception e) {
        Throwable cause = unwrap(e);
        if (cause instanceof TimeoutException) {
            return new MovieServiceGatewayTimeoutException(
                    "Timeout while calling movie-service " + operation, e);
        }
        if (cause instanceof ConnectException) {
            return new MovieServiceUnavailableException(
                    "movie-service is unavailable (connection refused)", e);
        }
        return new MovieServiceBadGatewayException(
                "Unexpected error while calling movie-service " + operation, e);
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

    private record MovieSearchRequest(List<Filter> filters, String sort, int page, int size) {
    }

    private record Filter(String path, String value, String comparator) {
    }

    private record MovieSearchResponse(List<MovieItem> items, long total) {
    }

    private record MovieItem(Long id, Integer oscarsCount, MovieDirector director) {
    }

    private record MovieDirector(String name) {
    }
}
