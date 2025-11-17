package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film addLike(Long filmId, Long userId) {
        Film film = getFilmById(filmId);

        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        film.getLikes().add(userId);

        log.info("Пользователь с ID {} поставил лайк фильму с ID {}", userId, filmId);
        return film;
    }

    public Film removeLike(Long filmId, Long userId) {
        Film film = getFilmById(filmId);

        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        if (!film.getLikes().remove(userId)) {
            log.warn("Пользователь с ID {} не ставил лайк фильму с ID {}", userId, filmId);
        } else {
            log.info("Пользователь с ID {} удалил лайк с фильма с ID {}", userId, filmId);
        }

        return film;
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.findAll().stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    private Film getFilmById(Long filmId) {
        Film film = filmStorage.getById(filmId);
        if (film == null) {
            throw new NotFoundException("Фильм с id = " + filmId + " не найден");
        }
        return film;
    }

    public List<Film> findAll() {
        return new ArrayList<>(filmStorage.findAll());
    }

    public Film create(Film film) {
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }

        if (!filmStorage.existsById(film.getId())) {
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }

        return filmStorage.update(film);
    }

    public Film getById(Long id) {
        return getFilmById(id);
    }

    public void delete(Long id) {
        getFilmById(id);
        filmStorage.delete(id);
        log.info("Фильм с ID {} успешно удален", id);
    }

    public void deleteAll() {
        filmStorage.deleteAll();
        log.info("Все фильмы успешно удалены");
    }

    public boolean existsById(Long id) {
        return filmStorage.existsById(id);
    }

    public Film addGenre(Long filmId, Genre genre) {
        Film film = getFilmById(filmId);
        film.getGenres().add(genre);
        log.info("Добавлен жанр {} фильму с ID {}", genre.getName(), filmId);
        return film;
    }

    public Film removeGenre(Long filmId, Genre genre) {
        Film film = getFilmById(filmId);
        film.getGenres().remove(genre);
        log.info("Удален жанр {} у фильма с ID {}", genre.getName(), filmId);
        return film;
    }

    public Film setMpa(Long filmId, MpaRating mpa) {
        Film film = getFilmById(filmId);
        film.setMpa(mpa);
        log.info("Установлен рейтинг {} для фильма с ID {}", mpa.getCode(), filmId);
        return film;
    }
}