package ru.tbank.soa.oscar.repository.dao;

import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.domain.PageResult;

import java.util.Collection;

public interface OscarDao {

    PageResult<Operator> findLosers(Collection<Long> awardedFilmIds, int page, int size);
}
