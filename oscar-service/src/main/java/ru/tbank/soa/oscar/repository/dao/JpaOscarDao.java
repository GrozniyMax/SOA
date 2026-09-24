package ru.tbank.soa.oscar.repository.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.soa.oscar.domain.Award;
import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.domain.PageResult;
import ru.tbank.soa.oscar.repository.entity.AwardEntity;
import ru.tbank.soa.oscar.repository.entity.OperatorEntity;
import ru.tbank.soa.oscar.repository.jpa.AwardJpaRepository;
import ru.tbank.soa.oscar.repository.jpa.OperatorJpaRepository;
import ru.tbank.soa.oscar.repository.mapper.OscarEntityMapper;

import java.util.Collection;
import java.util.List;

/**
 * Реализация {@link OscarDao} поверх Spring Data JPA.
 * Сквозь этот слой наружу проходят только доменные объекты; JPA-детали скрыты.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaOscarDao implements OscarDao {

    private final OperatorJpaRepository operatorRepository;
    private final AwardJpaRepository awardRepository;
    private final OscarEntityMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<Operator> findLosers(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<OperatorEntity> result = operatorRepository.findLosers(PageRequest.of(safePage, safeSize));
        List<Operator> items = result.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(items, result.getTotalElements(), safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Award> findAwardsByDirectorIn(Collection<String> directors) {
        List<AwardEntity> entities = awardRepository.findByDirectorIn(directors);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void revokeOscars(Collection<Long> awardIds) {
        if (awardIds == null || awardIds.isEmpty()) {
            return;
        }
        int updated = awardRepository.revokeOscars(awardIds);
        log.info("Revoked Oscars for {} awards", updated);
    }
}