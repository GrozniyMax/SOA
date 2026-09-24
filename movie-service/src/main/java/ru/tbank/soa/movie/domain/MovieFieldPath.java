package ru.tbank.soa.movie.domain;

/**
 * Путь к полю сущности Movie, используемый в динамическом поиске
 * (POST /movies/search).
 */
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
}