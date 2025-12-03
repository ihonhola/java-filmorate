package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FilmControllerTest {

    private FilmController filmController;
    private InMemoryFilmStorage filmStorage;
    private InMemoryUserStorage userStorage;
    private FilmService filmService;
    private Film validFilm;

    @BeforeEach
    void setUp() {
        filmStorage = new InMemoryFilmStorage();
        userStorage = new InMemoryUserStorage();
        filmService = new FilmService(filmStorage, userStorage, null, null);
        filmController = new FilmController(filmService);
        validFilm = new Film();
        validFilm.setName("Valid Film");
        validFilm.setDescription("Valid description");
        validFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        validFilm.setDuration(120);
    }

    @AfterEach
    void zero() {
        filmStorage.deleteAll();
        filmController = null;
        filmService = null;
        filmStorage = null;
        validFilm = null;
    }

    private Map<String, Object> filmToMap(Film film) {
        Map<String, Object> map = new HashMap<>();
        if (film.getId() != null) {
            map.put("id", film.getId());
        }
        map.put("name", film.getName());
        map.put("description", film.getDescription());
        map.put("releaseDate", film.getReleaseDate());
        map.put("duration", film.getDuration());
        return map;
    }

    @Test
    void createFilm_withValidData_shouldSucceed() {
        Film createdFilm = filmController.create(validFilm);

        assertNotNull(createdFilm);
        assertNotNull(createdFilm.getId());
        assertEquals("Valid Film", createdFilm.getName());
        assertEquals("Valid description", createdFilm.getDescription());
        assertEquals(LocalDate.of(2000, 1, 1), createdFilm.getReleaseDate());
        assertEquals(120, createdFilm.getDuration());
    }

    @Test
    void createFilm_withNullName_shouldThrowValidationException() {
        Film film = new Film();
        film.setName(null);
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Название не может быть пустым", exception.getMessage());
    }

    @Test
    void createFilm_withEmptyName_shouldThrowValidationException() {
        Film film = new Film();
        film.setName("");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Название не может быть пустым", exception.getMessage());
    }

    @Test
    void createFilm_withBlankName_shouldThrowValidationException() {
        Film film = new Film();
        film.setName("   ");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Название не может быть пустым", exception.getMessage());
    }

    @Test
    void createFilm_withLongDescription_shouldThrowValidationException() {
        Film film = new Film();
        film.setName("Valid Film");
        film.setDescription("Смысл в том, что его нет. И когда это понимаешь, всё становится смыслом. " +
                "Мир — это язык, а мы — слова, которые он говорит сам с собой." +
                "Все беды в мире происходят от того, что люди вечно суют нос не в свои дела. " +
                "И называют они это то братской любовью, то чувством долга."); // > 201 символа
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Описание не может быть больше 200 символов", exception.getMessage());
    }

    @Test
    void createFilm_withReleaseDateBeforeCinemaBirthday_shouldThrowValidationException() {
        Film film = new Film();
        film.setName("Valid Film");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(120);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Фильм не может выйти раньше дня рождения кино", exception.getMessage());
    }

    @Test
    void createFilm_withZeroDuration_shouldThrowValidationException() {
        Film film = new Film();
        film.setName("Valid Film");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(0);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Продолжительность фильма должна быть больше 0", exception.getMessage());
    }

    @Test
    void createFilm_withNegativeDuration_shouldThrowValidationException() {
        Film film = new Film();
        film.setName("Valid Film");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(-1);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Продолжительность фильма должна быть больше 0", exception.getMessage());
    }

    @Test
    void updateFilm_withValidData_shouldSucceed() {
        Film createdFilm = filmController.create(validFilm);
        Long filmId = createdFilm.getId();

        Film updateFilm = new Film();
        updateFilm.setId(filmId);
        updateFilm.setName("Updated Film");
        updateFilm.setDescription("Updated description");
        updateFilm.setReleaseDate(LocalDate.of(2020, 1, 1));
        updateFilm.setDuration(150);

        Film updatedFilm = filmController.update(updateFilm);

        assertNotNull(updatedFilm);
        assertEquals(filmId, updatedFilm.getId());
        assertEquals("Updated Film", updatedFilm.getName());
        assertEquals("Updated description", updatedFilm.getDescription());
        assertEquals(LocalDate.of(2020, 1, 1), updatedFilm.getReleaseDate());
        assertEquals(150, updatedFilm.getDuration());
    }

    @Test
    void updateFilm_withoutId_shouldThrowValidationException() {
        Film film = new Film();
        film.setName("Valid Film");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.update(film));
        assertEquals("Id должен быть указан", exception.getMessage());
    }

    @Test
    void updateFilm_nonExistentFilm_shouldThrowNotFoundException() {
        Film film = new Film();
        film.setId(999L);
        film.setName("Valid Film");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmController.update(film));
        assertEquals("Фильм с id = 999 не найден", exception.getMessage());
    }

    @Test
    void updateFilm_withInvalidData_shouldThrowValidationException() {
        Film createdFilm = filmController.create(validFilm);
        Long filmId = createdFilm.getId();

        Film updateFilm = new Film();
        updateFilm.setId(filmId);
        updateFilm.setName(""); // невалидное имя
        updateFilm.setDescription("Valid description");
        updateFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        updateFilm.setDuration(120);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.update(updateFilm));
        assertEquals("Название не может быть пустым", exception.getMessage());
    }

    @Test
    void findAll_shouldReturnAllFilms() {
        filmController.create(validFilm);

        Film film2 = new Film();
        film2.setName("Second Film");
        film2.setDescription("Second description");
        film2.setReleaseDate(LocalDate.of(2010, 1, 1));
        film2.setDuration(90);
        filmController.create(film2);

        List<Film> films = filmController.findAll();

        assertNotNull(films);
        assertEquals(2, films.size());
        assertTrue(films.stream().anyMatch(f -> f.getName().equals("Valid Film")));
        assertTrue(films.stream().anyMatch(f -> f.getName().equals("Second Film")));
    }
}