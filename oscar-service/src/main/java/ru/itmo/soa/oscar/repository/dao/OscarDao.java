package ru.itmo.soa.oscar.repository.dao;

import ru.itmo.soa.oscar.domain.Operator;
import ru.itmo.soa.oscar.domain.PageResult;

import java.util.Collection;

public interface OscarDao {

    PageResult<Operator> findLosers(Collection<Long> awardedFilmIds, int page, int size);
}
