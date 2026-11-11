package ru.itmo.soa.oscar.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class HumiliationResult {

    MovieGenre genre;

    int affectedDirectors;

    int revokedOscars;
}
