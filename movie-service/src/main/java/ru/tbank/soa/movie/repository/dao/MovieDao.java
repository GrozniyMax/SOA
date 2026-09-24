package ru.tbank.soa.movie.repository.dao;

import ru.tbank.soa.movie.domain.Movie;
import ru.tbank.soa.movie.domain.MovieGenre;
import ru.tbank.soa.movie.domain.MovieSearchCriteria;
import ru.tbank.soa.movie.domain.PageResult;

import java.util.Optional;

/**
 * Единственная точка входа в персистентность для сервисного слоя.
 * Принимает и возвращает только доменные объекты, полностью скрывая JPA/DB-детали.
 */
public interface MovieDao {

    /**
     * Сохраняет фильм. Если {@code movie.id() == 0L} — создаётся новая запись
     * (id генерируется БД); если {@code movie.id() > 0L} — обновление.
     *
     * @return сохранённый доменный фильм (с автогенерированным id при создании)
     */
    Movie save(Movie movie);

    Optional<Movie> findById(long id);

    boolean existsById(long id);

    void deleteById(long id);

    /**
     * Фильм с максимальным (лексикографически) ненулевым слоганом.
     */
    Optional<Movie> findWithMaxTagline();

    PageResult<Movie> search(MovieSearchCriteria criteria);

    PageResult<Movie> findByDirectorGreaterThan(String director, int page, int size);

    PageResult<MovieGenre> findUniqueGenres(int page, int size);
}