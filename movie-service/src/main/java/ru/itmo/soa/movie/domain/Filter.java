package ru.itmo.soa.movie.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class Filter {

    MovieFieldPath path;

    String value;

    FilterComparator comparator;
}
