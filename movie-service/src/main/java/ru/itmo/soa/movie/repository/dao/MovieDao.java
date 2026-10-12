package ru.itmo.soa.movie.repository.dao;

import ru.itmo.soa.movie.domain.DirectorsHumiliation;
import ru.itmo.soa.movie.domain.Movie;
import ru.itmo.soa.movie.domain.MovieGenre;
import ru.itmo.soa.movie.domain.MovieSearchCriteria;
import ru.itmo.soa.movie.domain.PageResult;

import java.util.Collection;
import java.util.List;
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

    /**
     * В одной транзакции отзывает все Оскары у фильмов перечисленных режиссёров.
     * Возвращает по одной записи на каждого запрошенного режиссёра с суммой
     * отозванных Оскаров (0, если наград не было), сохраняя порядок запроса.
     */
    List<DirectorsHumiliation> humiliateDirectors(Collection<String> directors);
}
