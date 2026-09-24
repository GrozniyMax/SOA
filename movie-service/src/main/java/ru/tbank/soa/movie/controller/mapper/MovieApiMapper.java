package ru.tbank.soa.movie.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.tbank.soa.movie.domain.Coordinates;
import ru.tbank.soa.movie.domain.Filter;
import ru.tbank.soa.movie.domain.Movie;
import ru.tbank.soa.movie.domain.MovieGenre;
import ru.tbank.soa.movie.domain.MovieSearchCriteria;
import ru.tbank.soa.movie.domain.PageResult;
import ru.tbank.soa.movie.domain.Person;
import ru.tbank.soa.movie.dto.GenrePage;
import ru.tbank.soa.movie.dto.MovieFilter;
import ru.tbank.soa.movie.dto.MoviePage;
import ru.tbank.soa.movie.dto.MovieRequest;
import ru.tbank.soa.movie.dto.MovieSearchRequest;

/**
 * MapStruct-маппер между DTO из OpenAPI-спецификации и доменной моделью.
 * Вспомогательные поля домена, отсутствующие в DTO (id, creationDate), оставляются
 * незаполненными — их проставляет сервисный слой.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MovieApiMapper {

    Movie toDomain(MovieRequest request);

    Coordinates toCoordinates(ru.tbank.soa.movie.dto.Coordinates coordinates);

    Person toPerson(ru.tbank.soa.movie.dto.Person person);

    MovieSearchCriteria toDomain(MovieSearchRequest request);

    Filter toDomain(MovieFilter filter);

    ru.tbank.soa.movie.dto.Movie toDto(Movie movie);

    ru.tbank.soa.movie.dto.Coordinates toDto(Coordinates coordinates);

    ru.tbank.soa.movie.dto.Person toDto(Person person);

    MoviePage toMoviePage(PageResult<Movie> page);

    GenrePage toGenrePage(PageResult<MovieGenre> page);
}