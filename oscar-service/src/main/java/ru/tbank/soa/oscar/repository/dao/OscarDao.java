package ru.tbank.soa.oscar.repository.dao;

import ru.tbank.soa.oscar.domain.Award;
import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.domain.PageResult;

import java.util.Collection;
import java.util.List;

/**
 * Единственная граница персистентности, которую использует сервис.
 * Работает только с доменными объектами; все JPA/БД-детали скрыты.
 */
public interface OscarDao {

    /**
     * Возвращает страницу операторов-"неудачников" — операторов без фильмов с oscarsCount > 0.
     */
    PageResult<Operator> findLosers(int page, int size);

    /**
     * Возвращает оскарные записи, режиссёр которых входит в указанный набор.
     */
    List<Award> findAwardsByDirectorIn(Collection<String> directors);

    /**
     * Обнуляет количество оскаров у указанных записей.
     */
    void revokeOscars(Collection<Long> awardIds);
}