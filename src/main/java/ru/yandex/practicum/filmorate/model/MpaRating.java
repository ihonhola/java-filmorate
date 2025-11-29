package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MpaRating {
    /*G("G", "У фильма нет возрастных ограничений"),
    PG("PG", "Детям рекомендуется смотреть фильм с родителями"),
    PG_13("PG-13", "Детям до 13 лет просмотр не желателен"),
    R("R", "Лицам до 17 лет просматривать фильм можно только в присутствии взрослого"),
    NC_17("NC-17", "Лицам до 18 лет просмотр запрещён"); */
    G(1L, "G"),
    PG(2L, "PG"),
    PG_13(3L, "PG-13"),
    R(4L, "R"),
    NC_17(5L, "NC-17");

    private final Long id;
    private final String name;

    MpaRating(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    @JsonValue
    public MpaResponse toJson() {
        return new MpaResponse(id, name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static MpaRating fromId(Long id) {
        for (MpaRating rating : values()) {
            if (rating.id.equals(id)) {
                return rating;
            }
        }
        throw new IllegalArgumentException("Unknown MPA id: " + id);
    }

    public static MpaRating fromCode(String code) {
        for (MpaRating rating : values()) {
            if (rating.name.equals(code)) {
                return rating;
            }
        }
        throw new IllegalArgumentException("Unknown MPA code: " + code);
    }

    // Вспомогательный класс для JSON представления
    public static class MpaResponse {
        private final Long id;
        private final String name;

        public MpaResponse(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}