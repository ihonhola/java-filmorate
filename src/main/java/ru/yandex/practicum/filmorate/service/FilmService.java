package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaService mpaService;
    private final GenreService genreService;
    private final LocalDate cinemaBirthday = LocalDate.of(1895, 12, 28);


    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       MpaService mpaService, GenreService genreService) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaService = mpaService;
        this.genreService = genreService;
    }

    public void addLike(Long filmId, Long userId) {
        Film film = getFilmById(filmId);

        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        filmStorage.addLike(filmId, userId);
        film.getLikes().add(userId);
        log.info("Пользователь с ID {} поставил лайк фильму с ID {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = getFilmById(filmId);

        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        filmStorage.removeLike(filmId, userId);
        film.getLikes().remove(userId);
        log.info("Пользователь с ID {} удалил лайк с фильма с ID {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
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
        validateFilm(film);
        // Валидация MPA
        if (film.getMpa() != null) {
            validateMpaExists(film.getMpa());
        }

        // Валидация жанров
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            validateGenreExists(film.getGenres());
        }

        return filmStorage.create(film);
    }

    public Film update(Film film) {
        validateFilm(film);

        if (film.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }

        if (!filmStorage.existsById(film.getId())) {
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }

        // Валидация MPA
        if (film.getMpa() != null) {
            validateMpaExists(film.getMpa());
        }

        // Валидация жанров
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            validateGenreExists(film.getGenres());
        }

        return filmStorage.update(film);
    }

    public Film getById(Long id) {
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

    private void validateMpaExists(MpaRating mpa) {
        mpaService.findById(mpa.getId());
    }

    private void validateGenreExists(Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        for (Genre genre : genres) {
            try {
                genreService.findById(genre.getId());
                } catch (NotFoundException e) {
                    throw new NotFoundException(("Жанр с id = " + genre.getId() + " не найден"));
            }
        }
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
}