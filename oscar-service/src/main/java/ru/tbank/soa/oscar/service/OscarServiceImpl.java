package ru.tbank.soa.oscar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.soa.oscar.client.MovieServiceClient;
import ru.tbank.soa.oscar.domain.Award;
import ru.tbank.soa.oscar.domain.HumiliationResult;
import ru.tbank.soa.oscar.domain.MovieGenre;
import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.domain.PageResult;
import ru.tbank.soa.oscar.repository.dao.OscarDao;

import java.util.List;
import java.util.Set;

/**
 * Реализация {@link OscarService}. Общается только с {@link OscarDao} и
 * {@link MovieServiceClient} — никаких JPA-репозиториев и DTO/контроллеров.
 */
@Service
@RequiredArgsConstructor
public class OscarServiceImpl implements OscarService {

    private final OscarDao dao;
    private final MovieServiceClient movieServiceClient;

    @Override
    @Transactional(readOnly = true)
    public PageResult<Operator> getLosers(int page, int size) {
        return dao.findLosers(page, size);
    }

    @Override
    @Transactional
    public HumiliationResult humiliateDirectorsByGenre(MovieGenre genre) {
        Set<String> directors = movieServiceClient.findDirectorsByGenre(genre);
        List<Award> awards = dao.findAwardsByDirectorIn(directors);
        List<Award> toRevoke = awards.stream()
                .filter(a -> a.oscarsCount() > 0)
                .toList();

        long affectedDirectors = toRevoke.stream()
                .map(Award::director)
                .distinct()
                .count();
        int revokedOscars = toRevoke.stream()
                .mapToInt(Award::oscarsCount)
                .sum();

        if (!toRevoke.isEmpty()) {
            dao.revokeOscars(toRevoke.stream().map(Award::id).toList());
        }

        return new HumiliationResult(genre, (int) affectedDirectors, revokedOscars);
    }
}