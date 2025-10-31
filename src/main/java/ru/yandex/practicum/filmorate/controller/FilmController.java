package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final FilmService filmService;
    private final LocalDate cinemaBirthday = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping
    public Collection<Film> findAll() {
        return filmService.findAll();
    }

    @GetMapping("/{id}")
    public Film getById(@PathVariable Long id) {
        return filmService.getById(id);
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        log.info("Получен запрос на добавление нового фильма: {}", film);

        validateFilm(film);

        return filmService.create(film);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        log.info("Получен запрос на удаление фильма с ID: {}", id);
        filmService.delete(id);
        log.info("Фильм с ID {} успешно удален", id);
    }

    @DeleteMapping
    public void deleteAll() {
        log.info("Получен запрос на удаление всех фильмов");
        filmService.deleteAll();
        log.info("Все фильмы успешно удалены");
    }

    private void validateFilm(Film film) {
        if (film == null) {
            String errorMessage = "Тело запроса не может быть пустым";
            log.warn("Ошибка валидации: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (film.getName() == null || film.getName().isBlank()) {
            String errorMessage = "Название не может быть пустым";
            log.warn("Ошибка валидации при добавлении фильма: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (film.getDescription().length() > 200) {
            String errorMessage = "Описание не может быть больше 200 символов";
            log.warn("Ошибка валидации при добавлении фильма: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (film.getReleaseDate().isBefore(cinemaBirthday)) {
            throw new ValidationException("Фильм не может выйти раньше дня рождения кино");
        }

        if (film.getDuration() <= 0) {
            String errorMessage = "Продолжительность фильма должна быть больше 0";
            log.warn("Ошибка валидации при добавлении фильма: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }
    }

    @PutMapping
    public Film update(@RequestBody Film newFilm) {
        log.info("Получен запрос на обновление фильма: {}", newFilm);
        validateFilm(newFilm);
        return filmService.update(newFilm);
    }

    @GetMapping("/popular")
    public List<Film> getPopularFilms(@RequestParam(defaultValue = "10") int count) {
        log.info("Получен запрос на получение {} популярных фильмов", count);
        return filmService.getPopularFilms(count);
    }

    @PutMapping("/{id}/like/{userId}")
    public Film addLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Получен запрос на добавление лайка фильму {} от пользователя {}", id, userId);
        return filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public Film removeLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Получен запрос на удаление лайка с фильма {} от пользователя {}", id, userId);
        return filmService.removeLike(id, userId);
    }
}
