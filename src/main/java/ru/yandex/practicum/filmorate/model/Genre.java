package ru.yandex.practicum.filmorate.model;

public enum Genre {
    COMEDY(1L, "Комедия"),
    DRAMA(2L, "Драма"),
    CARTOON(3L, "Мультфильм"),
    THRILLER(4L, "Триллер"),
    DOCUMENTARY(5L, "Документальный"),
    ACTION(6L, "Боевик");

    private final Long id;
    private final String name;

    Genre(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static Genre fromId(Long id) {
        for (Genre genre : values()) {
            if (genre.id.equals(id)) {
                return genre;
            }
        }
        throw new IllegalArgumentException("Unknown genre id: " + id);
    }
}