package ru.tbank.soa.oscar.client;

import ru.tbank.soa.oscar.domain.MovieGenre;

import java.util.Set;

/**
 * Клиент REST API movie-service. Возвращает только то, что нужно бизнес-слою.
 */
public interface MovieServiceClient {

    /**
     * Возвращает имена режиссёров, снявших хотя бы один фильм указанного жанра.
     */
    Set<String> findDirectorsByGenre(MovieGenre genre);
}