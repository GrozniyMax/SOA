package ru.itmo.soa.movie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.soa.movie.config.MovieProperties;
import ru.itmo.soa.movie.domain.DirectorsHumiliation;
import ru.itmo.soa.movie.domain.Filter;
import ru.itmo.soa.movie.domain.Movie;
import ru.itmo.soa.movie.domain.MovieGenre;
import ru.itmo.soa.movie.domain.MovieSearchCriteria;
import ru.itmo.soa.movie.domain.PageResult;
import ru.itmo.soa.movie.repository.dao.MovieDao;
import ru.itmo.soa.movie.service.exception.InvalidMovieDataException;
import ru.itmo.soa.movie.service.exception.InvalidSearchFilterException;
import ru.itmo.soa.movie.service.exception.MovieNotFoundException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

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
                movie.getName(),
                movie.getCoordinates(),
                LocalDate.now(),
                movie.getOscarsCount(),
                movie.getTotalBoxOffice(),
                movie.getTagline(),
                movie.getGenre(),
                movie.getDirector());
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
                existing.getId(),
                movie.getName(),
                movie.getCoordinates(),
                existing.getCreationDate(),
                movie.getOscarsCount(),
                movie.getTotalBoxOffice(),
                movie.getTagline(),
                movie.getGenre(),
                movie.getDirector());
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
        criteria.getFilters().forEach(this::validate);
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

    @Override
    public List<DirectorsHumiliation> humiliateDirectors(Collection<String> directors) {
        return dao.humiliateDirectors(directors);
    }

    private void validate(Filter filter) {
        if (filter.getValue() == null) {
            throw new InvalidSearchFilterException("filter value for " + filter.getPath().getPath() + " must not be null");
        }
        try {
            filter.getPath().parse(filter.getValue());
        } catch (RuntimeException e) {
            throw new InvalidSearchFilterException(
                    "invalid value '" + filter.getValue() + "' for filter " + filter.getPath().getPath());
        }
    }

    private void validate(Movie movie) {
        if (movie.getName() == null || movie.getName().isBlank()) {
            throw new InvalidMovieDataException("name must not be null or blank");
        }
        if (movie.getCoordinates() == null) {
            throw new InvalidMovieDataException("coordinates must not be null");
        }
        if (movie.getGenre() == null) {
            throw new InvalidMovieDataException("genre must not be null");
        }
        if (movie.getDirector() == null) {
            throw new InvalidMovieDataException("director must not be null");
        }
        if (movie.getTagline() != null && movie.getTagline().length() > properties.getTaglineMaxLength()) {
            throw new InvalidMovieDataException("tagline length must not exceed " + properties.getTaglineMaxLength());
        }
        if (movie.getOscarsCount() != null && movie.getOscarsCount() <= 0) {
            throw new InvalidMovieDataException("oscarsCount must be greater than 0");
        }
        if (movie.getTotalBoxOffice() != null && movie.getTotalBoxOffice() <= 0) {
            throw new InvalidMovieDataException("totalBoxOffice must be greater than 0");
        }
    }
}