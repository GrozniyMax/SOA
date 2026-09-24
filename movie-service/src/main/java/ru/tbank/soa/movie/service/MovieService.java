package ru.tbank.soa.movie.service;

import ru.tbank.soa.movie.domain.Movie;
import ru.tbank.soa.movie.domain.MovieGenre;
import ru.tbank.soa.movie.domain.MovieSearchCriteria;
import ru.tbank.soa.movie.domain.PageResult;

/**
 * Бизнес-логика Movie Collection Service.
 * Работает только с доменными объектами; персистентность — исключительно через DAO.
 */
public interface MovieService {

    Movie addMovie(Movie movie);

    Movie getMovieById(long id);

    Movie updateMovie(long id, Movie movie);

    void deleteMovie(long id);

    PageResult<Movie> searchMovies(MovieSearchCriteria criteria);

    PageResult<Movie> getByDirectorGreaterThan(String director, int page, int size);

    Movie getMovieWithMaxTagline();

    PageResult<MovieGenre> getUniqueGenres(int page, int size);
}