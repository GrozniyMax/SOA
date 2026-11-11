package ru.itmo.soa.movie.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.LocalDate;

@Value
@AllArgsConstructor
public class Movie {

    long id;

    String name;

    Coordinates coordinates;

    LocalDate creationDate;

    Integer oscarsCount;

    Float totalBoxOffice;

    String tagline;

    MovieGenre genre;

    Person director;
}
