package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       JdbcTemplate jdbcTemplate) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    /*public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }*/

    public Film addLike(Long filmId, Long userId) {
        Film film = getFilmById(filmId);

        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        // Проверяем, не поставил ли уже лайк
        String checkSql = "SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND user_id = ?";
        Integer existingLikes = jdbcTemplate.queryForObject(checkSql, Integer.class, filmId, userId);

        if (existingLikes == null || existingLikes == 0) {
            // Сохраняем лайк в БД
            String insertSql = "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)";
            jdbcTemplate.update(insertSql, filmId, userId);
            log.info("Лайк сохранен в БД: film_id={}, user_id={}", filmId, userId);
        } else {
            log.info("Лайк уже существует: film_id={}, user_id={}", filmId, userId);
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
        /*return filmStorage.findAll().stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());*/
        List<Film> allFilms = new ArrayList<>(filmStorage.findAll());

        // Логируем для отладки
        log.info("Все фильмы для популярности:");
        for (Film film : allFilms) {
            log.info("Фильм ID: {}, Название: {}, Лайков: {}",
                    film.getId(), film.getName(), film.getLikes().size());
        }

        List<Film> popularFilms = allFilms.stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());

        log.info("Популярные фильмы (count={}):", count);
        for (Film film : popularFilms) {
            log.info("Фильм ID: {}, Лайков: {}", film.getId(), film.getLikes().size());
        }

        return popularFilms;
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
        // Валидация MPA
        if (film.getMpa() != null) {
            validateMpaExists(film.getMpa());
        }

        // Валидация жанров
        if (film.getGenres() != null) {
            for (Long genreId : film.getGenres()) {
                validateGenreExists(genreId);
            }
        }
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
        //return getFilmById(id);
        return filmStorage.getById(id);
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


    /*public Film addGenre(Long filmId, Genre genre) {
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
    }*/

    /*public FilmResponse convertToResponse(Film film) {
        FilmResponse response = new FilmResponse();
        response.setId(film.getId());
        response.setName(film.getName());
        response.setDescription(film.getDescription());
        response.setReleaseDate(film.getReleaseDate());
        response.setDuration(film.getDuration());
        response.setLikes(film.getLikes());

        // Конвертируем ID в объекты
        if (film.getMpa() != null) {
            MpaObject mpaObject = new MpaObject();
            mpaObject.setId(film.getMpa());
            response.setMpa(mpaObject);
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<GenreObject> genreObjects = new HashSet<>();
            for (Long genreId : film.getGenres()) {
                GenreObject genreObject = new GenreObject();
                genreObject.setId(genreId);
                genreObjects.add(genreObject);
            }
            response.setGenres(genreObjects);
        }

        return response;
    }*/

    private void validateMpaExists(Long mpaId) {
        try {
            MpaRating.fromId(mpaId); // Этот метод выбросит исключение если id не найден
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("MPA рейтинг с id = " + mpaId + " не найден");
        }
    }

    private void validateGenreExists(Long genreId) {
        try {
            Genre.fromId(genreId); // Этот метод выбросит исключение если id не найден
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Жанр с id = " + genreId + " не найден");
        }
    }
}