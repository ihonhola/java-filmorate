package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.model.GenreResponse;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
    public List<FilmResponse> findAll() {
        return filmService.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public FilmResponse getById(@PathVariable Long id) {
        Film film = filmService.getById(id);
        return convertToResponse(film);
    }

    @PostMapping
    public FilmResponse create(@RequestBody Map<String, Object> request) {
        log.info("Получен запрос на добавление нового фильма: {}", request);

        Film film = convertMapToFilm(request);
        validateFilm(film);
        Film createdFilm = filmService.create(film);
        return convertToResponse(createdFilm);
    }

    @PutMapping
    public FilmResponse update(@RequestBody Map<String, Object> request) {
        log.info("Получен запрос на обновление фильма: {}", request);

        Film film = convertMapToFilm(request);
        validateFilm(film);
        Film updatedFilm = filmService.update(film);
        return convertToResponse(updatedFilm);
    }

    private Film convertMapToFilm(Map<String, Object> request) {
        Film film = new Film();

        // Базовые поля
        film.setName((String) request.get("name"));
        film.setDescription((String) request.get("description"));

        if (request.get("releaseDate") != null) {
            film.setReleaseDate(LocalDate.parse(request.get("releaseDate").toString()));
        }

        if (request.get("duration") != null) {
            film.setDuration(Integer.valueOf(request.get("duration").toString()));
        }

        if (request.get("id") != null) {
            film.setId(Long.valueOf(request.get("id").toString()));
        }

        // Обрабатываем mpa
        if (request.get("mpa") != null) {
            if (request.get("mpa") instanceof Map) {
                Map<String, Object> mpaMap = (Map<String, Object>) request.get("mpa");
                if (mpaMap.get("id") != null) {
                    film.setMpa(Long.valueOf(mpaMap.get("id").toString()));
                }
            } else if (request.get("mpa") instanceof Number) {
                // Если пришло просто число
                film.setMpa(Long.valueOf(request.get("mpa").toString()));
            }
        }

        // Обрабатываем genres
        if (request.get("genres") != null && request.get("genres") instanceof List) {
            List<Object> genresList = (List<Object>) request.get("genres");
            film.setGenres(new java.util.HashSet<>());

            for (Object genreObj : genresList) {
                if (genreObj instanceof Map) {
                    Map<String, Object> genreMap = (Map<String, Object>) genreObj;
                    if (genreMap.get("id") != null) {
                        film.getGenres().add(Long.valueOf(genreMap.get("id").toString()));
                    }
                } else if (genreObj instanceof Number) {
                    // Если пришло просто число
                    film.getGenres().add(Long.valueOf(genreObj.toString()));
                }
            }
        }

        return film;
    }

    // Метод конвертации Film в FilmResponse
    private FilmResponse convertToResponse(Film film) {
        FilmResponse response = new FilmResponse();
        response.setId(film.getId());
        response.setName(film.getName());
        response.setDescription(film.getDescription());
        response.setReleaseDate(film.getReleaseDate());
        response.setDuration(film.getDuration());
        response.setLikes(film.getLikes());

        // Конвертируем ID в объекты для ответа
        if (film.getMpa() != null) {
            MpaRating mpaEnum = MpaRating.fromId(film.getMpa());
            MpaResponse mpaResponse = new MpaResponse();
            mpaResponse.setId(mpaEnum.getId());
            mpaResponse.setName(mpaEnum.getName());
            response.setMpa(mpaResponse);
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<GenreResponse> genreResponses = new ArrayList<>();

            // Сортируем жанры по ID
            List<Long> sortedGenreIds = new ArrayList<>(film.getGenres());
            Collections.sort(sortedGenreIds);

            for (Long genreId : sortedGenreIds) {
                // Создаем GenreResponse с id и name
                Genre genreEnum = Genre.fromId(genreId); // Используйте ваш существующий метод
                GenreResponse genreResponse = new GenreResponse();
                genreResponse.setId(genreEnum.getId());
                genreResponse.setName(genreEnum.getName());
                genreResponses.add(genreResponse);
            }
            response.setGenres(genreResponses);
        }

        return response;
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

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            String errorMessage = "Описание не может быть больше 200 символов";
            log.warn("Ошибка валидации при добавлении фильма: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(cinemaBirthday)) {
            throw new ValidationException("Фильм не может выйти раньше дня рождения кино");
        }

        if (film.getDuration() != null && film.getDuration() <= 0) {
            String errorMessage = "Продолжительность фильма должна быть больше 0";
            log.warn("Ошибка валидации при добавлении фильма: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }
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