package ru.tbank.soa.oscar.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA-сущность связи оператора с фильмом (таблица operator_film).
 */
@Entity
@Table(name = "operator_film")
@Getter
@Setter
public class OperatorFilmEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private OperatorEntity operator;

    @Column(name = "film_id", nullable = false)
    private Long filmId;
}