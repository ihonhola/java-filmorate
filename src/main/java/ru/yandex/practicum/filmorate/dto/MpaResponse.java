package ru.yandex.practicum.filmorate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MpaResponse {
    private Long id;
    private String name;

    public MpaResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}