package ru.tbank.soa.oscar.repository.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.domain.PageResult;
import ru.tbank.soa.oscar.repository.entity.OperatorEntity;
import ru.tbank.soa.oscar.repository.jpa.OperatorJpaRepository;
import ru.tbank.soa.oscar.repository.mapper.OscarEntityMapper;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaOscarDao implements OscarDao {

    private final OperatorJpaRepository operatorRepository;
    private final OscarEntityMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<Operator> findLosers(Collection<Long> awardedFilmIds, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by("id"));
        Page<OperatorEntity> result = awardedFilmIds == null || awardedFilmIds.isEmpty()
                ? operatorRepository.findAll(pageable)
                : operatorRepository.findWithoutFilms(awardedFilmIds, pageable);
        List<Operator> items = result.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(items, result.getTotalElements(), safePage, safeSize);
    }
}
