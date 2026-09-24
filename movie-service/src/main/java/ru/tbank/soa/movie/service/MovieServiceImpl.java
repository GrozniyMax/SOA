package ru.tbank.soa.movie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.soa.movie.config.MovieProperties;
import ru.tbank.soa.movie.domain.Movie;
import ru.tbank.soa.movie.domain.MovieGenre;
import ru.tbank.soa.movie.domain.MovieSearchCriteria;
import ru.tbank.soa.movie.domain.PageResult;
import ru.tbank.soa.movie.repository.dao.MovieDao;
import ru.tbank.soa.movie.service.exception.InvalidMovieDataException;
import ru.tbank.soa.movie.service.exception.MovieNotFoundException;

import java.time.LocalDate;

/**
 * Реализация бизнес-логики Movie Collection Service.
 * Вся бизнес-логика здесь; персистентность — только через {@link MovieDao},
 * настройки — через {@link MovieProperties}. Методы принимают/возвращают доменные объекты.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {

    private final MovieDao dao;
    private final MovieProperties properties;

    @Override
    public Movie addMovie(Movie movie) {
        Movie toSave = new Movie(
                0L,
                movie.name(),
                movie.coordinates(),
                LocalDate.now(), // генерируется автоматически
                movie.oscarsCount(),
                movie.totalBoxOffice(),
                movie.tagline(),
                movie.genre(),
                movie.director());
        validate(toSave);
        return dao.save(toSave);
    }

    @Override
    @Transactional(readOnly = true)
    public Movie getMovieById(long id) {
        return dao.findById(id).orElseThrow(() -> new MovieNotFoundException(id));
    }

    @Override
    public Movie updateMovie(long id, Movie movie) {
        Movie existing = dao.findById(id).orElseThrow(() -> new MovieNotFoundException(id));
        Movie toSave = new Movie(
                existing.id(),
                movie.name(),
                movie.coordinates(),
                existing.creationDate(), // не меняется при обновлении
                movie.oscarsCount(),
                movie.totalBoxOffice(),
                movie.tagline(),
                movie.genre(),
                movie.director());
        validate(toSave);
        return dao.save(toSave);
    }

    @Override
    public void deleteMovie(long id) {
        if (!dao.existsById(id)) {
            throw new MovieNotFoundException(id);
        }
        dao.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Movie> searchMovies(MovieSearchCriteria criteria) {
        return dao.search(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Movie> getByDirectorGreaterThan(String director, int page, int size) {
        return dao.findByDirectorGreaterThan(director, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Movie getMovieWithMaxTagline() {
        return dao.findWithMaxTagline()
                .orElseThrow(() -> new MovieNotFoundException("no movie with non-empty tagline"));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MovieGenre> getUniqueGenres(int page, int size) {
        return dao.findUniqueGenres(page, size);
    }

    /**
     * Проверка ограничений целостности доменных данных (защита на границе бизнес-слоя;
     * основная валидация выполняется аннотациями в контроллере).
     */
    private void validate(Movie movie) {
        if (movie.name() == null || movie.name().isBlank()) {
            throw new InvalidMovieDataException("name must not be null or blank");
        }
        if (movie.coordinates() == null) {
            throw new InvalidMovieDataException("coordinates must not be null");
        }
        if (movie.genre() == null) {
            throw new InvalidMovieDataException("genre must not be null");
        }
        if (movie.director() == null) {
            throw new InvalidMovieDataException("director must not be null");
        }
        if (movie.tagline() != null && movie.tagline().length() > properties.taglineMaxLength()) {
            throw new InvalidMovieDataException("tagline length must not exceed " + properties.taglineMaxLength());
        }
        if (movie.oscarsCount() != null && movie.oscarsCount() <= 0) {
            throw new InvalidMovieDataException("oscarsCount must be greater than 0");
        }
        if (movie.totalBoxOffice() != null && movie.totalBoxOffice() <= 0) {
            throw new InvalidMovieDataException("totalBoxOffice must be greater than 0");
        }
    }
}