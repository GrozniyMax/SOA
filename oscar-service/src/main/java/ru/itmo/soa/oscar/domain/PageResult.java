package ru.itmo.soa.oscar.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;

@Value
@AllArgsConstructor
public class PageResult<T> {

    List<T> items;

    long total;

    int page;

    int size;
}
