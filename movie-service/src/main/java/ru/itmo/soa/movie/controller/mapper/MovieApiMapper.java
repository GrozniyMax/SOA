package ru.itmo.soa.movie.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.itmo.soa.movie.domain.Coordinates;
import ru.itmo.soa.movie.domain.DirectorsHumiliation;
import ru.itmo.soa.movie.domain.Filter;
import ru.itmo.soa.movie.domain.Movie;
import ru.itmo.soa.movie.domain.MovieGenre;
import ru.itmo.soa.movie.domain.MovieSearchCriteria;
import ru.itmo.soa.movie.domain.PageResult;
import ru.itmo.soa.movie.domain.Person;
import ru.itmo.soa.movie.dto.GenrePage;
import ru.itmo.soa.movie.dto.MovieFilter;
import ru.itmo.soa.movie.dto.MoviePage;
import ru.itmo.soa.movie.dto.MovieRequest;
import ru.itmo.soa.movie.dto.MovieSearchRequest;

import java.util.List;

/**
 * MapStruct-маппер между DTO из OpenAPI-спецификации и доменной моделью.
 * Вспомогательные поля домена, отсутствующие в DTO (id, creationDate), оставляются
 * незаполненными — их проставляет сервисный слой.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MovieApiMapper {

    Movie toDomain(MovieRequest request);

    Coordinates toCoordinates(ru.itmo.soa.movie.dto.Coordinates coordinates);

    Person toPerson(ru.itmo.soa.movie.dto.Person person);

    MovieSearchCriteria toDomain(MovieSearchRequest request);

    Filter toDomain(MovieFilter filter);

    ru.itmo.soa.movie.dto.Movie toDto(Movie movie);

    ru.itmo.soa.movie.dto.Coordinates toDto(Coordinates coordinates);

    ru.itmo.soa.movie.dto.Person toDto(Person person);

    MoviePage toMoviePage(PageResult<Movie> page);

    GenrePage toGenrePage(PageResult<MovieGenre> page);

    ru.itmo.soa.movie.dto.DirectorsHumiliation toDto(DirectorsHumiliation humiliation);

    default ru.itmo.soa.movie.dto.DirectorsHumiliationResult toDirectorsHumiliationResult(
            List<DirectorsHumiliation> humiliations) {
        return new ru.itmo.soa.movie.dto.DirectorsHumiliationResult(
                humiliations.stream().map(this::toDto).collect(java.util.stream.Collectors.toList()));
    }
}