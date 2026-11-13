package ru.itmo.soa.movie.repository.dao;

import ru.itmo.soa.movie.domain.DirectorsHumiliation;
import ru.itmo.soa.movie.domain.Movie;
import ru.itmo.soa.movie.domain.MovieGenre;
import ru.itmo.soa.movie.domain.MovieSearchCriteria;
import ru.itmo.soa.movie.domain.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MovieDao {

    Movie save(Movie movie);

    Optional<Movie> findById(long id);

    boolean existsById(long id);

    void deleteById(long id);

    Optional<Movie> findWithMaxTagline();

    PageResult<Movie> search(MovieSearchCriteria criteria);

    PageResult<Movie> findByDirectorGreaterThan(String director, int page, int size);

    PageResult<MovieGenre> findUniqueGenres(int page, int size);

    List<DirectorsHumiliation> humiliateDirectors(Collection<String> directors);
}
