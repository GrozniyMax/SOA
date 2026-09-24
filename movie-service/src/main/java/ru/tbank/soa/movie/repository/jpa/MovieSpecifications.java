package ru.tbank.soa.movie.repository.jpa;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ru.tbank.soa.movie.domain.EyeColor;
import ru.tbank.soa.movie.domain.Filter;
import ru.tbank.soa.movie.domain.FilterComparator;
import ru.tbank.soa.movie.domain.HairColor;
import ru.tbank.soa.movie.domain.MovieFieldPath;
import ru.tbank.soa.movie.domain.MovieGenre;
import ru.tbank.soa.movie.domain.MovieSearchCriteria;
import ru.tbank.soa.movie.repository.entity.MovieEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Построение Criteria API-спецификаций ({@link Specification}) для динамического поиска фильмов
 * (POST /movies/search). Прячет детали JPA-мета-модели за доменными {@link MovieSearchCriteria}.
 */
public final class MovieSpecifications {

    private static final int MAX_PAGE_SIZE = 100;

    private MovieSpecifications() {
    }

    /**
     * Строит предикат AND по всем фильтрам критериев. Пустой список фильтров
     * возвращает конъюнкцию (всегда истинный предикат) — матчатся все строки.
     */
    public static Specification<MovieEntity> from(MovieSearchCriteria criteria) {
        if (criteria.filters() == null || criteria.filters().isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> {
            Predicate[] predicates = criteria.filters().stream()
                    .map(filter -> toPredicate(root, cb, filter))
                    .toArray(Predicate[]::new);
            return cb.and(predicates);
        };
    }

    /**
     * Строит {@link Pageable} из критериев (page/size/sort).
     */
    public static Pageable pageable(MovieSearchCriteria criteria) {
        int page = Math.max(criteria.page(), 0);
        int size = Math.min(Math.max(criteria.size(), 1), MAX_PAGE_SIZE);
        Sort sort = parseSort(criteria.sort());
        return PageRequest.of(page, size, sort);
    }

    private static Predicate toPredicate(jakarta.persistence.criteria.Root<MovieEntity> root,
                                         jakarta.persistence.criteria.CriteriaBuilder cb,
                                         Filter filter) {
        MovieFieldPath fieldPath = filter.path();
        Path<Object> attributePath = resolvePath(root, fieldPath.getPath());
        Object parsedValue = parseValue(fieldPath, filter.value());
        FilterComparator comparator = filter.comparator();

        switch (comparator) {
            case EQ -> {
                return cb.equal(attributePath, parsedValue);
            }
            case GT -> {
                return cb.greaterThan(asComparable(attributePath), (Comparable) parsedValue);
            }
            case LT -> {
                return cb.lessThan(asComparable(attributePath), (Comparable) parsedValue);
            }
            case SUBSTRING -> {
                if (isStringField(fieldPath)) {
                    return cb.like(attributePath.as(String.class), "%" + filter.value() + "%");
                }
                // Для нестроковых полей SUBSTRING не имеет смысла — аккуратно сводим к равенству.
                return cb.equal(attributePath, parsedValue);
            }
            default -> throw new IllegalArgumentException("Unsupported comparator: " + comparator);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Expression<T> asComparable(Expression<?> expression) {
        return (Expression<T>) expression;
    }

    private static Path<Object> resolvePath(jakarta.persistence.criteria.Root<MovieEntity> root, String dotPath) {
        Path<Object> path = root.get(dotPath.split("\\.")[0]);
        String[] segments = dotPath.split("\\.");
        for (int i = 1; i < segments.length; i++) {
            path = path.get(segments[i]);
        }
        return path;
    }

    private static boolean isStringField(MovieFieldPath fieldPath) {
        return switch (fieldPath) {
            case NAME, TAGLINE, DIRECTOR_NAME -> true;
            default -> false;
        };
    }

    private static Object parseValue(MovieFieldPath fieldPath, String value) {
        return switch (fieldPath) {
            case ID, OSCARS_COUNT -> Long.parseLong(value);
            case COORDINATES_X -> Integer.parseInt(value);
            case COORDINATES_Y, TOTAL_BOX_OFFICE -> Float.parseFloat(value);
            case CREATION_DATE, DIRECTOR_BIRTHDAY -> LocalDate.parse(value);
            case GENRE -> MovieGenre.valueOf(value);
            case DIRECTOR_EYE_COLOR -> EyeColor.valueOf(value);
            case DIRECTOR_HAIR_COLOR -> HairColor.valueOf(value);
            default -> value; // NAME, TAGLINE, DIRECTOR_NAME — строки
        };
    }

    private static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = Arrays.stream(sort.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(MovieSpecifications::toOrder)
                .filter(java.util.Objects::nonNull)
                .toList();

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    private static Sort.Order toOrder(String fieldWithDirection) {
        String[] parts = fieldWithDirection.split(":");
        String fieldName = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : "asc";

        if (!isKnownSortField(fieldName)) {
            // Неизвестное поле сортировки аккуратно игнорируем.
            return null;
        }

        Sort.Direction sortDirection =
                "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return new Sort.Order(sortDirection, fieldName);
    }

    private static boolean isKnownSortField(String fieldName) {
        return EnumSet.allOf(MovieFieldPath.class).stream()
                .anyMatch(p -> p.getPath().equals(fieldName));
    }
}