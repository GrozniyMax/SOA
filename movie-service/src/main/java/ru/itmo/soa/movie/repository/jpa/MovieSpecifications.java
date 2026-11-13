package ru.itmo.soa.movie.repository.jpa;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ru.itmo.soa.movie.domain.Filter;
import ru.itmo.soa.movie.domain.FilterComparator;
import ru.itmo.soa.movie.domain.MovieFieldPath;
import ru.itmo.soa.movie.domain.MovieSearchCriteria;
import ru.itmo.soa.movie.repository.entity.MovieEntity;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class MovieSpecifications {

    private static final int MAX_PAGE_SIZE = 100;
    private static final char LIKE_ESCAPE = '\\';

    private MovieSpecifications() {
    }

    public static Specification<MovieEntity> from(MovieSearchCriteria criteria) {
        if (criteria.getFilters() == null || criteria.getFilters().isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> {
            Predicate[] predicates = criteria.getFilters().stream()
                    .map(filter -> toPredicate(root, cb, filter))
                    .toArray(Predicate[]::new);
            return cb.and(predicates);
        };
    }

    public static Pageable pageable(MovieSearchCriteria criteria) {
        int page = Math.max(criteria.getPage(), 0);
        int size = Math.min(Math.max(criteria.getSize(), 1), MAX_PAGE_SIZE);
        Sort sort = parseSort(criteria.getSort());
        return PageRequest.of(page, size, sort);
    }

    private static Predicate toPredicate(Root<MovieEntity> root, CriteriaBuilder cb, Filter filter) {
        MovieFieldPath fieldPath = filter.getPath();
        Path<Object> attributePath = resolvePath(root, fieldPath.getPath());
        Object parsedValue = fieldPath.parse(filter.getValue());
        FilterComparator comparator = filter.getComparator();

        switch (comparator) {
            case EQ:
                return cb.equal(attributePath, parsedValue);
            case GT:
                return cb.greaterThan(asComparable(attributePath), (Comparable) parsedValue);
            case LT:
                return cb.lessThan(asComparable(attributePath), (Comparable) parsedValue);
            case SUBSTRING:
                if (fieldPath.isString()) {
                    return cb.like(
                            cb.lower(attributePath.as(String.class)),
                            "%" + escapeLike(filter.getValue().toLowerCase(Locale.ROOT)) + "%",
                            LIKE_ESCAPE);
                }
                return cb.equal(attributePath, parsedValue);
            default:
                throw new IllegalArgumentException("Unsupported comparator: " + comparator);
        }
    }

    private static String escapeLike(String value) {
        return value
                .replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }

    @SuppressWarnings("unchecked")
    private static <T> Expression<T> asComparable(Expression<?> expression) {
        return (Expression<T>) expression;
    }

    private static Path<Object> resolvePath(Root<MovieEntity> root, String dotPath) {
        String[] segments = dotPath.split("\\.");
        Path<Object> path = root.get(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            path = path.get(segments[i]);
        }
        return path;
    }

    private static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = Arrays.stream(sort.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(MovieSpecifications::toOrder)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    private static Sort.Order toOrder(String fieldWithDirection) {
        String[] parts = fieldWithDirection.split(":");
        String fieldName = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : "asc";

        if (!isKnownSortField(fieldName)) {
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
