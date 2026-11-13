package ru.itmo.soa.oscar.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class Operator {

    Long id;

    String name;
}
