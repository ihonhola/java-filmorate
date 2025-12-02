package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.service.MpaService;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
@Slf4j
public class MpaController {

    private final MpaService mpaService;

    @GetMapping
    public List<MpaRating> findAll() {
        log.info("Получен запрос на получение всех MPA рейтингов");
        List<MpaRating> ratings = mpaService.findAll();
        log.info("Найдено {} MPA рейтингов", ratings.size());
        return ratings;
    }

    @GetMapping("/{id}")
    public MpaRating findById(@PathVariable Long id) {
        log.info("Получен запрос на получение MPA рейтинга с ID: {}", id);
        return mpaService.findById(id);
    }
}