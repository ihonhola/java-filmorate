package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MpaRating {
    private Long id;
    private String name;
    private String description;

    public MpaRating(Long id) {
        this.id = id;
    }

    public MpaRating(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public MpaRating(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}