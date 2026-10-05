package ru.tbank.soa.oscar.service;

import ru.tbank.soa.oscar.domain.HumiliationResult;
import ru.tbank.soa.oscar.domain.MovieGenre;
import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.domain.PageResult;

public interface OscarService {

    PageResult<Operator> getLosers(int page, int size);

    HumiliationResult humiliateDirectorsByGenre(MovieGenre genre);
}