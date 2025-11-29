package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GenreResponse {
    private Long id;
    private String name;

    public GenreResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static GenreResponse fromId(Long id) {
        Genre genre = Genre.fromId(id); // Ваш существующий метод
        return new GenreResponse(genre.getId(), genre.getName());
    }
}