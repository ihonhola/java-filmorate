package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
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

        if (films.containsKey(film.getId())) {
            Film oldFilm = films.get(film.getId());
            log.debug("Найден фильм для обновления: {}", oldFilm);

            oldFilm.setName(film.getName());
            oldFilm.setDescription(film.getDescription());
            oldFilm.setReleaseDate(film.getReleaseDate());
            oldFilm.setDuration(film.getDuration());

            log.info("Фильм с ID {} успешно обновлен. Новые данные: название: {}, дата релиза: {}, длительность: {}",
                    film.getId(), film.getName(), film.getReleaseDate(), film.getDuration());
            return oldFilm;
        }

        return null;
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
        if (removedFilm != null) {
            log.info("Фильм с ID {} успешно удален. Название: {}", id, removedFilm.getName());
        } else {
            log.warn("Попытка удаления несуществующего фильма с ID: {}", id);
        }
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
}