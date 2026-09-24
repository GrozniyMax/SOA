package ru.tbank.soa.movie.repository.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.soa.movie.domain.Movie;
import ru.tbank.soa.movie.domain.MovieGenre;
import ru.tbank.soa.movie.domain.MovieSearchCriteria;
import ru.tbank.soa.movie.domain.PageResult;
import ru.tbank.soa.movie.repository.entity.MovieEntity;
import ru.tbank.soa.movie.repository.jpa.MovieJpaRepository;
import ru.tbank.soa.movie.repository.jpa.MovieSpecifications;
import ru.tbank.soa.movie.repository.mapper.MovieEntityMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация {@link MovieDao} поверх Spring Data JPA.
 * Сквозь этот слой наружу проходят только доменные объекты; JPA-детали скрыты.
 */
@Repository
public class JpaMovieDao implements MovieDao {

    private final MovieJpaRepository repository;
    private final MovieEntityMapper mapper;

    public JpaMovieDao(MovieJpaRepository repository, MovieEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Movie save(Movie movie) {
        MovieEntity entity = mapper.toEntity(movie);
        if (movie.id() == 0L) {
            // Новая запись — обнуляем id, чтобы БД сгенерировала его через IDENTITY.
            entity.setId(null);
        }
        MovieEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Movie> findById(long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(long id) {
        return repository.existsById(id);
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Movie> findWithMaxTagline() {
        return repository.findFirstByTaglineIsNotNullOrderByTaglineDesc().map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Movie> search(MovieSearchCriteria criteria) {
        Page<MovieEntity> page = repository.findAll(
                MovieSpecifications.from(criteria),
                MovieSpecifications.pageable(criteria));
        List<Movie> items = page.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(items, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Movie> findByDirectorGreaterThan(String director, int page, int size) {
        Pageable pageable = buildPageable(page, size);
        Page<MovieEntity> result = repository.findByDirector_NameGreaterThan(director, pageable);
        List<Movie> items = result.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(items, result.getTotalElements(), result.getNumber(), result.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MovieGenre> findUniqueGenres(int page, int size) {
        List<MovieGenre> distinct = new ArrayList<>(repository.findDistinctGenres());
        long total = distinct.size();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int from = safePage * safeSize;
        List<MovieGenre> items = from >= distinct.size()
                ? List.of()
                : distinct.subList(from, Math.min(from + safeSize, distinct.size()));
        return new PageResult<>(items, total, safePage, safeSize);
    }

    private static Pageable buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return org.springframework.data.domain.PageRequest.of(safePage, safeSize);
    }
}