package ru.itmo.soa.oscar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.soa.oscar.client.MovieServiceClient;
import ru.itmo.soa.oscar.domain.HumiliationResult;
import ru.itmo.soa.oscar.domain.Movie;
import ru.itmo.soa.oscar.domain.MovieGenre;
import ru.itmo.soa.oscar.domain.Operator;
import ru.itmo.soa.oscar.domain.PageResult;
import ru.itmo.soa.oscar.repository.dao.OscarDao;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OscarServiceImpl implements OscarService {

    private final OscarDao dao;
    private final MovieServiceClient movieServiceClient;

    @Override
    public PageResult<Operator> getLosers(int page, int size) {
        Set<Long> awardedFilmIds = movieServiceClient.findAwardedMovieIds();
        return dao.findLosers(awardedFilmIds, page, size);
    }

    @Override
    public HumiliationResult humiliateDirectorsByGenre(MovieGenre genre) {
        Set<String> directors = movieServiceClient.findDirectorsByGenre(genre);

        int affectedDirectors = 0;
        int revokedOscars = 0;
        for (String director : directors) {
            List<Movie> movies = movieServiceClient.findAwardedMoviesByDirector(director);
            if (movies.isEmpty()) {
                continue;
            }
            for (Movie movie : movies) {
                movieServiceClient.revokeOscars(movie.id());
                revokedOscars += movie.oscarsCount();
            }
            affectedDirectors++;
        }

        return new HumiliationResult(genre, affectedDirectors, revokedOscars);
    }
}
