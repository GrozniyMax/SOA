package ru.itmo.soa.movie.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class DirectorsHumiliation {

    String director;

    int revokedOscars;
}
