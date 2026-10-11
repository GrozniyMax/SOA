package ru.itmo.soa.movie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.soa.movie.api.MoviesApi;
import ru.itmo.soa.movie.controller.mapper.MovieApiMapper;
import ru.itmo.soa.movie.dto.GenrePage;
import ru.itmo.soa.movie.dto.Movie;
import ru.itmo.soa.movie.dto.MoviePage;
import ru.itmo.soa.movie.dto.MovieRequest;
import ru.itmo.soa.movie.dto.MovieSearchRequest;
import ru.itmo.soa.movie.service.MovieService;

import java.net.URI;

/**
 * REST-контроллер, реализующий сгенерированный из OpenAPI интерфейс {@link MoviesApi}.
 * Маппинг DTO ↔ домен выполняется через {@link MovieApiMapper}; бизнес-логика — в {@link MovieService}.
 */
@Validated
@RestController
@RequiredArgsConstructor
public class MoviesApiController implements MoviesApi {

    private final MovieService service;
    private final MovieApiMapper mapper;

    @Override
    public ResponseEntity<Movie> addMovie(MovieRequest movieRequest) {
        var created = service.addMovie(mapper.toDomain(movieRequest));
        return ResponseEntity.created(URI.create("/movies/" + created.id())).body(mapper.toDto(created));
    }

    @Override
    public ResponseEntity<Movie> getMovieById(Integer id) {
        return ResponseEntity.ok(mapper.toDto(service.getMovieById(id)));
    }

    @Override
    public ResponseEntity<Movie> updateMovie(Integer id, MovieRequest movieRequest) {
        return ResponseEntity.ok(mapper.toDto(service.updateMovie(id, mapper.toDomain(movieRequest))));
    }

    @Override
    public ResponseEntity<Void> deleteMovie(Integer id) {
        service.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Movie> getMovieWithMaxTagline() {
        return ResponseEntity.ok(mapper.toDto(service.getMovieWithMaxTagline()));
    }

    @Override
    public ResponseEntity<MoviePage> getMoviesByDirectorGreaterThan(String director, Integer page, Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return ResponseEntity.ok(mapper.toMoviePage(service.getByDirectorGreaterThan(director, p, s)));
    }

    @Override
    public ResponseEntity<GenrePage> getUniqueGenres(Integer page, Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return ResponseEntity.ok(mapper.toGenrePage(service.getUniqueGenres(p, s)));
    }

    @Override
    public ResponseEntity<MoviePage> searchMovies(MovieSearchRequest movieSearchRequest) {
        return ResponseEntity.ok(mapper.toMoviePage(service.searchMovies(mapper.toDomain(movieSearchRequest))));
    }
}