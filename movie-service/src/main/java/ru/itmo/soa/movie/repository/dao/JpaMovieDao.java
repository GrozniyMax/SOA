package ru.itmo.soa.movie.repository.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.soa.movie.domain.Coordinates;
import ru.itmo.soa.movie.domain.DirectorsHumiliation;
import ru.itmo.soa.movie.domain.Movie;
import ru.itmo.soa.movie.domain.MovieGenre;
import ru.itmo.soa.movie.domain.MovieSearchCriteria;
import ru.itmo.soa.movie.domain.PageResult;
import ru.itmo.soa.movie.domain.Person;
import ru.itmo.soa.movie.repository.entity.CoordinatesEntity;
import ru.itmo.soa.movie.repository.entity.MovieEntity;
import ru.itmo.soa.movie.repository.entity.PersonEntity;
import ru.itmo.soa.movie.repository.jpa.CoordinatesJpaRepository;
import ru.itmo.soa.movie.repository.jpa.MovieJpaRepository;
import ru.itmo.soa.movie.repository.jpa.MovieSpecifications;
import ru.itmo.soa.movie.repository.jpa.PersonJpaRepository;
import ru.itmo.soa.movie.repository.mapper.MovieEntityMapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Реализация {@link MovieDao} поверх Spring Data JPA.
 * Сквозь этот слой наружу проходят только доменные объекты; JPA-детали скрыты.
 *
 * Координаты переиспользуются по значению (x, y) и ведут счётчик ссылок {@code refCount}:
 * при создании/обновлении фильма счётчик инкрементируется, при удалении/замене —
 * декрементируется; при достижении 0 координаты удаляются. Режиссёр переиспользуется по имени.
 *
 * Важно про порядок операций: FK movie.coordinates_id указывает на coordinates, поэтому
 * координаты можно удалить только после того, как ссылающийся фильм удалён/обновлён
 * (иначе нарушение внешнего ключа).
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaMovieDao implements MovieDao {

    private final MovieJpaRepository movieRepository;
    private final CoordinatesJpaRepository coordinatesRepository;
    private final PersonJpaRepository personRepository;
    private final MovieEntityMapper mapper;

    @Override
    @Transactional
    public Movie save(Movie movie) {
        MovieEntity entity = mapper.toEntity(movie);
        entity.setCoordinates(acquireCoordinates(movie.getCoordinates()));
        entity.setDirector(acquireDirector(movie.getDirector()));

        if (movie.getId() == 0L) {
            // Новая запись — обнуляем id, чтобы БД сгенерировала его через IDENTITY.
            entity.setId(null);
            return mapper.toDomain(movieRepository.save(entity));
        }

        // Обновление: сначала переводим FK фильма на новые координаты (flush),
        // затем освобождаем прежние координаты.
        Long oldCoordinatesId = movieRepository.findById(movie.getId())
                .map(m -> m.getCoordinates().getId())
                .orElse(null);
        entity.setId(movie.getId());
        MovieEntity saved = movieRepository.save(entity);
        movieRepository.flush();
        CoordinatesEntity newCoordinates = saved.getCoordinates();
        if (oldCoordinatesId != null && !oldCoordinatesId.equals(newCoordinates.getId())) {
            releaseCoordinates(oldCoordinatesId);
        }
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Movie> findById(long id) {
        return movieRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(long id) {
        return movieRepository.existsById(id);
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        Long coordinatesId = movieRepository.findById(id)
                .map(m -> m.getCoordinates().getId())
                .orElse(null);
        movieRepository.deleteById(id);
        movieRepository.flush();
        if (coordinatesId != null) {
            releaseCoordinates(coordinatesId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Movie> findWithMaxTagline() {
        return movieRepository.findFirstByTaglineIsNotNullOrderByTaglineDesc().map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Movie> search(MovieSearchCriteria criteria) {
        Page<MovieEntity> page = movieRepository.findAll(
                MovieSpecifications.from(criteria),
                MovieSpecifications.pageable(criteria));
        List<Movie> items = page.getContent().stream().map(mapper::toDomain).collect(Collectors.toList());
        return new PageResult<>(items, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Movie> findByDirectorGreaterThan(String director, int page, int size) {
        Pageable pageable = buildPageable(page, size);
        Page<MovieEntity> result = movieRepository.findByDirector_NameGreaterThan(director, pageable);
        List<Movie> items = result.getContent().stream().map(mapper::toDomain).collect(Collectors.toList());
        return new PageResult<>(items, result.getTotalElements(), result.getNumber(), result.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MovieGenre> findUniqueGenres(int page, int size) {
        List<MovieGenre> distinct = movieRepository.findDistinctGenres();
        long total = distinct.size();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int from = safePage * safeSize;
        List<MovieGenre> items = from >= distinct.size()
                ? List.of()
                : distinct.subList(from, Math.min(from + safeSize, distinct.size()));
        return new PageResult<>(items, total, safePage, safeSize);
    }

    @Override
    @Transactional
    public List<DirectorsHumiliation> humiliateDirectors(Collection<String> directors) {
        Map<String, Integer> revoked = movieRepository.sumOscarsByDirectors(directors).stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).intValue(), Integer::sum));
        movieRepository.nullOscarsByDirectors(directors);
        return directors.stream()
                .map(director -> new DirectorsHumiliation(director, revoked.getOrDefault(director, 0)))
                .collect(Collectors.toList());
    }

    /**
     * Возвращает сущность координат для значений (x, y): переиспользует существующую
     * (увеличивая счётчик ссылок) или создаёт новую с {@code refCount = 1}.
     */
    private CoordinatesEntity acquireCoordinates(Coordinates coordinates) {
        return coordinatesRepository.findByXAndY(coordinates.getX(), coordinates.getY())
                .map(existing -> {
                    existing.setRefCount(existing.getRefCount() + 1);
                    return coordinatesRepository.save(existing);
                })
                .orElseGet(() -> {
                    CoordinatesEntity created = new CoordinatesEntity();
                    created.setX(coordinates.getX());
                    created.setY(coordinates.getY());
                    created.setRefCount(1);
                    return coordinatesRepository.save(created);
                });
    }

    /**
     * Возвращает сущность режиссёра: переиспользует существующего по имени или создаёт нового.
     */
    private PersonEntity acquireDirector(Person director) {
        return personRepository.findByName(director.getName())
                .orElseGet(() -> {
                    PersonEntity created = new PersonEntity();
                    created.setName(director.getName());
                    created.setBirthday(director.getBirthday());
                    created.setEyeColor(director.getEyeColor());
                    created.setHairColor(director.getHairColor());
                    return personRepository.save(created);
                });
    }

    /**
     * Освобождает ссылку на координаты по их id: декрементирует {@code refCount}
     * и удаляет координаты, если счётчик достиг 0.
     */
    private void releaseCoordinates(Long coordinatesId) {
        coordinatesRepository.findById(coordinatesId).ifPresent(coordinates -> {
            coordinates.setRefCount(coordinates.getRefCount() - 1);
            if (coordinates.getRefCount() <= 0) {
                coordinatesRepository.delete(coordinates);
            } else {
                coordinatesRepository.save(coordinates);
            }
        });
    }

    private static Pageable buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize);
    }
}