package ru.itmo.soa.movie.domain;

import java.time.LocalDate;

public enum MovieFieldPath {
    ID("id"),
    NAME("name"),
    COORDINATES_X("coordinates.x"),
    COORDINATES_Y("coordinates.y"),
    CREATION_DATE("creationDate"),
    OSCARS_COUNT("oscarsCount"),
    TOTAL_BOX_OFFICE("totalBoxOffice"),
    TAGLINE("tagline"),
    GENRE("genre"),
    DIRECTOR_NAME("director.name"),
    DIRECTOR_BIRTHDAY("director.birthday"),
    DIRECTOR_EYE_COLOR("director.eyeColor"),
    DIRECTOR_HAIR_COLOR("director.hairColor");

    private final String path;

    MovieFieldPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public boolean isString() {
        return this == NAME || this == TAGLINE || this == DIRECTOR_NAME;
    }

    public Object parse(String value) {
        return switch (this) {
            case ID -> Long.parseLong(value);
            case OSCARS_COUNT, COORDINATES_X -> Integer.parseInt(value);
            case COORDINATES_Y, TOTAL_BOX_OFFICE -> Float.parseFloat(value);
            case CREATION_DATE, DIRECTOR_BIRTHDAY -> LocalDate.parse(value);
            case GENRE -> MovieGenre.valueOf(value);
            case DIRECTOR_EYE_COLOR -> EyeColor.valueOf(value);
            case DIRECTOR_HAIR_COLOR -> HairColor.valueOf(value);
            case NAME, TAGLINE, DIRECTOR_NAME -> value;
        };
    }
}
