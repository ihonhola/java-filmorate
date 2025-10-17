package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

        private final Map<Long, Film> films = new HashMap<>();
        final LocalDate cinemaBirthday = LocalDate.of(1895, 12, 28);

        @GetMapping
        public Collection<Film>  findAll(){
            log.info("Получен запрос на получение всех фильмов. Текущее количество: {}", films.size());
            return films.values();
        }

        @PostMapping
        public Film create(@RequestBody Film film) {
            log.info("Получен запрос на добавление нового фильма: {}", film);

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
                String errorMessage = "Фильм не может выйти раньше дня рождения кино";
                log.warn("Ошибка валидации при добавлении фильма: {}", errorMessage);
                throw new ValidationException(errorMessage);
            }
            if (film.getDuration() <= 0) {
                String errorMessage = "Продолжительность фильма должна быть больше 0";
                log.warn("Ошибка валидации при добавлении фильма: {}", errorMessage);
                throw new ValidationException(errorMessage);
            }

            film.setId(getNextId());
            film.setName(film.getName());
            film.setDescription(film.getDescription());
            film.setReleaseDate(film.getReleaseDate());
            film.setDuration(film.getDuration());
            films.put(film.getId(), film);

            log.info("Фильм успешно добавлен с ID: {}. Название: {}, Дата релиза: {}, Длительность: {}",
                    film.getId(), film.getName(), film.getReleaseDate(), film.getDuration());

            return film;
        }

        private long getNextId() {
            long currentMaxId = films.keySet()
                    .stream()
                    .mapToLong(id -> id)
                    .max()
                    .orElse(0);
            long nextId = ++currentMaxId;
            log.debug("Сгенерирован новый ID: {}", nextId);
            return nextId;
        }

        @PutMapping
        public Film update (@RequestBody Film newFilm) {
            log.info("Получен запрос на обновление фильма: {}", newFilm);

            if (newFilm.getId() == null) {
                String errorMessage = "Id должен быть указан";
                log.warn("Ошибка валидации при обновлении фильма: {}", errorMessage);
                throw new ValidationException(errorMessage);
            }
            if (films.containsKey(newFilm.getId())) {
                Film oldFilm = films.get(newFilm.getId());
                log.debug("Найден фильм для обновления: {}", oldFilm);
                if (newFilm.getName() == null || newFilm.getName().isBlank()) {
                    String errorMessage = "Название не может быть пустым";
                    log.warn("Ошибка валидации при обновлении фильма с ID {}: {}", newFilm.getId(), errorMessage);
                    throw new ValidationException(errorMessage);
                }
                if (newFilm.getDescription().length() > 200) {
                    String errorMessage = "Описание не может быть больше 200 символов";
                    log.warn("Ошибка валидации при обновлении фильма с ID {}: {}", newFilm.getId(), errorMessage);
                    throw new ValidationException(errorMessage);
                }
                if (newFilm.getReleaseDate().isBefore(cinemaBirthday)) {
                    String errorMessage = "Фильм не может выйти раньше дня рождения кино";
                    log.warn("Ошибка валидации при обновлении фильма с ID {}: {}", newFilm.getId(), errorMessage);
                    throw new ValidationException(errorMessage);
                }
                if (newFilm.getDuration() <= 0) {
                    String errorMessage = "Продолжительность фильма должна быть больше 0";
                    log.warn("Ошибка валидации при обновлении фильма с ID {}: {}", newFilm.getId(), errorMessage);
                    throw new ValidationException(errorMessage);
                }

                oldFilm.setName(newFilm.getName());
                oldFilm.setDescription(newFilm.getDescription());
                oldFilm.setReleaseDate(newFilm.getReleaseDate());
                oldFilm.setDuration(newFilm.getDuration());
                log.info("Фильм с ID {} успешно обновлен. Новые данные: название: {}, дата релиза: {}, длительность: {}",
                        newFilm.getId(), newFilm.getName(), newFilm.getReleaseDate(), newFilm.getDuration());
                return oldFilm;
            }

            String errorMessage = "Фильм с id = " + newFilm.getId() + " не найден";
            log.warn("Ошибка при обновлении фильма: {}", errorMessage);
            throw new NotFoundException(errorMessage);
        }
}
