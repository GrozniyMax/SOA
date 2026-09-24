package ru.tbank.soa.oscar.service;

import ru.tbank.soa.oscar.domain.HumiliationResult;
import ru.tbank.soa.oscar.domain.MovieGenre;
import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.domain.PageResult;

/**
 * Бизнес-логика Oscar Service.
 */
public interface OscarService {

    /**
     * Возвращает страницу операторов-"неудачников".
     */
    PageResult<Operator> getLosers(int page, int size);

    /**
     * "Унижение": отзывает оскары у всех фильмов указанного жанра по режиссёрам,
     * найденным в movie-service, у которых oscarsCount &gt; 0.
     */
    HumiliationResult humiliateDirectorsByGenre(MovieGenre genre);
}