package ru.tbank.soa.movie.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import ru.tbank.soa.movie.domain.EyeColor;
import ru.tbank.soa.movie.domain.HairColor;

import java.time.LocalDate;

/**
 * JPA-представление персоны (режиссёра) — встраиваемый value-объект.
 * Перечисления ({@link EyeColor}, {@link HairColor}) переиспользуются из домена.
 */
@Embeddable
public class PersonEntity {

    @Column(nullable = false)
    private String name;

    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    private EyeColor eyeColor;

    @Enumerated(EnumType.STRING)
    private HairColor hairColor;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public EyeColor getEyeColor() {
        return eyeColor;
    }

    public void setEyeColor(EyeColor eyeColor) {
        this.eyeColor = eyeColor;
    }

    public HairColor getHairColor() {
        return hairColor;
    }

    public void setHairColor(HairColor hairColor) {
        this.hairColor = hairColor;
    }
}