package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@Qualifier("inMemoryFilmStorage")
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private long nextId = 1;

    @Override
    public Collection<Film> findAll() {
        log.info("Получен запрос на получение всех фильмов. Текущее количество: {}", films.size());
        return films.values();
    }

    @Override
    public Film create(Film film) {
        log.info("Получен запрос на добавление нового фильма: {}", film);
        film.setId(getNextId());
        films.put(film.getId(), film);

        log.info("Фильм успешно добавлен с ID: {}. Название: {}, Дата релиза: {}, Длительность: {}",
                film.getId(), film.getName(), film.getReleaseDate(), film.getDuration());

        return film;
    }

    @Override
    public Film update(Film film) {
        log.info("Получен запрос на обновление фильма: {}", film);

        Film oldFilm = films.get(film.getId());
        log.debug("Найден фильм для обновления: {}", oldFilm);

        oldFilm.setName(film.getName());
        oldFilm.setDescription(film.getDescription());
        oldFilm.setReleaseDate(film.getReleaseDate());
        oldFilm.setDuration(film.getDuration());

        // Обновляем лайки
        if (film.getLikes() != null) {
            oldFilm.setLikes(film.getLikes());
        }

        // Обновляем жанры
        if (film.getGenres() != null) {
            oldFilm.setGenres(film.getGenres());
        }

        // Обновляем MPA
        if (film.getMpa() != null) {
            oldFilm.setMpa(film.getMpa());
        }

        log.info("Фильм с ID {} успешно обновлен. Новые данные: название: {}, дата релиза: {}, длительность: {}",
                film.getId(), film.getName(), film.getReleaseDate(), film.getDuration());
        return oldFilm;
        }

    @Override
    public Film getById(Long id) {
        return films.get(id);
    }

    @Override
    public boolean existsById(Long id) {
        return films.containsKey(id);
    }

    @Override
    public void delete(Long id) {
        Film removedFilm = films.remove(id);
        log.info("Фильм с ID {} успешно удален. Название: {}", id, removedFilm.getName());
    }

    @Override
    public void deleteAll() {
        int count = films.size();
        films.clear();
        log.info("Все фильмы удалены. Удалено {} записей", count);
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

    @Override
    public void addLike(Long filmId, Long userId) {
        Film film = films.get(filmId);
        if (film != null) {
            film.getLikes().add(userId);
            log.info("Лайк добавлен к фильму {} от пользователя {}", filmId, userId);
        } else {
            log.warn("Фильм с ID {} не найден для добавления лайка", filmId);
        }
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        Film film = films.get(filmId);
        if (film != null) {
            if (film.getLikes().remove(userId)) {
                log.info("Лайк удален с фильма {} от пользователя {}", filmId, userId);
            } else {
                log.warn("Лайк от пользователя {} не найден у фильма {}", userId, filmId);
            }
        } else {
            log.warn("Фильм с ID {} не найден для удаления лайка", filmId);
        }
    }
}