package ru.itmo.soa.movie.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.LocalDate;

@Value
@AllArgsConstructor
public class Person {

    String name;

    LocalDate birthday;

    EyeColor eyeColor;

    HairColor hairColor;
}
