package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.GenreResponse;
import ru.yandex.practicum.filmorate.storage.film.GenreDbStorage;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/genres")
@RequiredArgsConstructor
@Slf4j
public class GenreController {

    private final GenreDbStorage genreStorage;

    @GetMapping
    public List<GenreResponse> findAll() {
        log.info("Получен запрос на получение всех жанров");
        return genreStorage.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public GenreResponse findById(@PathVariable Long id) {
        log.info("Получен запрос на получение жанра с ID: {}", id);
        Genre genre = genreStorage.findById(id)
                .orElseThrow(() -> new ru.yandex.practicum.filmorate.exceptions.NotFoundException(
                        "Жанр с id = " + id + " не найден"));
        return convertToResponse(genre);
    }

    private GenreResponse convertToResponse(Genre genre) {
        GenreResponse response = new GenreResponse();
        response.setId(genre.getId());
        response.setName(genre.getName());
        return response;
    }
}