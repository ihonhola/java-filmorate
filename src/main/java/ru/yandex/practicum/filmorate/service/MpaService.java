package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MpaService {

    private final MpaDbStorage mpaStorage;

    public List<MpaRating> findAll() {
        log.info("Получен запрос на получение всех MPA рейтингов");
        return mpaStorage.findAll();
    }

    public MpaRating findById(Long id) {
        log.info("Получен запрос на получение MPA рейтинга с ID: {}", id);
        return mpaStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("MPA рейтинг с id = " + id + " не найден"));
    }
}