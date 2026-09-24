package ru.tbank.soa.movie.repository.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.tbank.soa.movie.domain.MovieGenre;

import java.time.LocalDate;

/**
 * JPA-сущность фильма (таблица movie).
 * Перечисление {@link MovieGenre} переиспользуется из домена.
 */
@Entity
@Table(name = "movie")
public class MovieEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "coordinates_x")),
            @AttributeOverride(name = "y", column = @Column(name = "coordinates_y"))
    })
    private CoordinatesEntity coordinates;

    @Column(nullable = false)
    private LocalDate creationDate;

    private Integer oscarsCount;

    private Float totalBoxOffice;

    private String tagline;

    @Enumerated(EnumType.STRING)
    private MovieGenre genre;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "director_name")),
            @AttributeOverride(name = "birthday", column = @Column(name = "director_birthday")),
            @AttributeOverride(name = "eyeColor", column = @Column(name = "director_eye_color")),
            @AttributeOverride(name = "hairColor", column = @Column(name = "director_hair_color"))
    })
    private PersonEntity director;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CoordinatesEntity getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(CoordinatesEntity coordinates) {
        this.coordinates = coordinates;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public Integer getOscarsCount() {
        return oscarsCount;
    }

    public void setOscarsCount(Integer oscarsCount) {
        this.oscarsCount = oscarsCount;
    }

    public Float getTotalBoxOffice() {
        return totalBoxOffice;
    }

    public void setTotalBoxOffice(Float totalBoxOffice) {
        this.totalBoxOffice = totalBoxOffice;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public MovieGenre getGenre() {
        return genre;
    }

    public void setGenre(MovieGenre genre) {
        this.genre = genre;
    }

    public PersonEntity getDirector() {
        return director;
    }

    public void setDirector(PersonEntity director) {
        this.director = director;
    }
}