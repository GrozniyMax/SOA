package ru.tbank.soa.oscar.client;

import ru.tbank.soa.oscar.domain.Movie;
import ru.tbank.soa.oscar.domain.MovieGenre;

import java.util.List;
import java.util.Set;

public interface MovieServiceClient {

    Set<String> findDirectorsByGenre(MovieGenre genre);

    List<Movie> findAwardedMoviesByDirector(String director);

    Set<Long> findAwardedMovieIds();

    void revokeOscars(long movieId);
}
