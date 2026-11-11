package ru.itmo.soa.oscar.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class OperatorFilm {

    Long id;

    Long operatorId;

    Long filmId;
}
