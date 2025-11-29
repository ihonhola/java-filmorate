package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.MpaResponse;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
@Slf4j
public class MpaController {

    private final MpaDbStorage mpaStorage;

    @GetMapping
    public List<MpaRating.MpaResponse> findAll() {
        log.info("Получен запрос на получение всех MPA рейтингов");
        List<MpaRating.MpaResponse> ratings = mpaStorage.findAll().stream()
                .map(MpaRating::toJson)
                .collect(Collectors.toList());
        log.info("Найдено {} MPA рейтингов", ratings.size());
        return ratings;
    }

    @GetMapping("/{id}")
    public MpaRating.MpaResponse findById(@PathVariable Long id) {
        log.info("Получен запрос на получение MPA рейтинга с ID: {}", id);
        MpaRating rating = mpaStorage.findById(id)
                .orElseThrow(() -> new ru.yandex.practicum.filmorate.exceptions.NotFoundException(
                        "MPA рейтинг с id = " + id + " не найден"));
        return rating.toJson();
    }

    private MpaResponse convertToResponse(MpaRating rating) {
        MpaResponse response = new MpaResponse();
        response.setId(rating.getId());
        response.setName(rating.getName());
        return response;
    }
}