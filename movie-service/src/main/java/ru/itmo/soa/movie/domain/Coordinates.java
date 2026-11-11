package ru.itmo.soa.movie.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class Coordinates {

    int x;

    float y;
}
