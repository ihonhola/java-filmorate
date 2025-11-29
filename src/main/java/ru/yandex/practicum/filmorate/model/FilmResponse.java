package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class FilmResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private Integer duration;
    private Set<Long> likes = new HashSet<>();
    private List<GenreResponse> genres = new ArrayList<>();
    private MpaResponse mpa;
}

/*@Data
class GenreObject {
    private Long id;

    public GenreObject(Long id) {
        this.id = id;
    }
}

@Data
class MpaObject {
    private Long id;

    public MpaObject(Long id) {
        this.id = id;
    }
} */