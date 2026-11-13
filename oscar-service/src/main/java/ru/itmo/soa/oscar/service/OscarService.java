package ru.itmo.soa.oscar.service;

import ru.itmo.soa.oscar.domain.HumiliationResult;
import ru.itmo.soa.oscar.domain.MovieGenre;
import ru.itmo.soa.oscar.domain.Operator;
import ru.itmo.soa.oscar.domain.PageResult;

public interface OscarService {

    PageResult<Operator> getLosers(int page, int size);

    HumiliationResult humiliateDirectorsByGenre(MovieGenre genre);
}