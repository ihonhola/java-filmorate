package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmorateApplicationTests {
    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;
    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;
    private final JdbcTemplate jdbcTemplate;

    @Test
    public void testFindUserById() {
        Optional<User> userOptional = Optional.ofNullable(userStorage.getById(1L));

        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(user ->
                        assertThat(user).hasFieldOrPropertyWithValue("id", 1L)
                );
    }

    @Test
    void testGetExistingUser() {
        User foundUser = userStorage.getById(1L);

        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(1L);
        assertThat(foundUser.getEmail()).isEqualTo("oblomov@example.com");
        assertThat(foundUser.getLogin()).isEqualTo("oblomov");
        assertThat(foundUser.getName()).isEqualTo("Обломов Илья Ильич");
    }

    @Test
    void testFindAllUsers() {
        Collection<User> users = userStorage.findAll();

        assertThat(users).hasSize(3);
        assertThat(users).extracting(User::getLogin)
                .contains("oblomov", "benjamin", "dr_garin");
    }

    @Test
    void testUserExists() {
        boolean exists1 = userStorage.existsById(1L);
        boolean exists2 = userStorage.existsById(2L);
        boolean exists3 = userStorage.existsById(3L);
        boolean notExists = userStorage.existsById(999L);

        assertThat(exists1).isTrue();
        assertThat(exists2).isTrue();
        assertThat(exists3).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void testCreateAndUpdateUser() {
        // Создаем нового пользователя
        User newUser = new User();
        newUser.setEmail("newuser@example.com");
        newUser.setLogin("newlogin");
        newUser.setName("New User");
        newUser.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = userStorage.create(newUser);
        Long newUserId = createdUser.getId();

        // Проверяем, что создался
        assertThat(createdUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(userStorage.existsById(newUserId)).isTrue();

        // Обновляем пользователя
        createdUser.setEmail("updated@example.com");
        createdUser.setName("Updated User");
        User updatedUser = userStorage.update(createdUser);

        // Проверяем обновление
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getName()).isEqualTo("Updated User");

        // Очищаем созданные данные
        userStorage.delete(newUserId);
    }

    @Test
    void testGetExistingFilm() {
        Film foundFilm = filmStorage.getById(1L);

        assertThat(foundFilm).isNotNull();
        assertThat(foundFilm.getId()).isEqualTo(1L);
        assertThat(foundFilm.getName()).isEqualTo("Кин-Дза-Дза");
        assertThat(foundFilm.getDescription()).contains("Антиутопическая комедия");
    }

    @Test
    void testFindAllFilms() {
        Collection<Film> films = filmStorage.findAll();

        assertThat(films).hasSize(3);
        assertThat(films).extracting(Film::getName)
                .contains("Кин-Дза-Дза", "Столетний старик, который вышел в окно и исчез", "Бивень");
    }

    @Test
    void testFilmExists() {
        boolean exists1 = filmStorage.existsById(1L);
        boolean exists2 = filmStorage.existsById(2L);
        boolean exists3 = filmStorage.existsById(3L);
        boolean notExists = filmStorage.existsById(999L);

        assertThat(exists1).isTrue();
        assertThat(exists2).isTrue();
        assertThat(exists3).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void testCreateAndUpdateFilm() {
        // Создаем новый фильм
        Film newFilm = new Film();
        newFilm.setName("New Film");
        newFilm.setDescription("New Film Description");
        newFilm.setReleaseDate(LocalDate.of(2023, 1, 1));
        newFilm.setDuration(100);

        Film createdFilm = filmStorage.create(newFilm);
        Long newFilmId = createdFilm.getId();

        // Проверяем, что создался
        assertThat(createdFilm.getName()).isEqualTo("New Film");
        assertThat(filmStorage.existsById(newFilmId)).isTrue();

        // Обновляем фильм
        createdFilm.setName("Updated Film");
        createdFilm.setDuration(120);
        Film updatedFilm = filmStorage.update(createdFilm);

        // Проверяем обновление
        assertThat(updatedFilm.getName()).isEqualTo("Updated Film");
        assertThat(updatedFilm.getDuration()).isEqualTo(120);

        // Очищаем созданные данные
        filmStorage.delete(newFilmId);
    }

    @Test
    void testFilmGenres() {
        // Проверяем, что у фильма есть жанры
        Film film = filmStorage.getById(1L); // Кин-Дза-Дза

        assertThat(film).isNotNull();
        assertThat(film.getGenres()).isNotEmpty();
    }

    @Test
    void testFilmMpa() {
        // Проверяем, что у фильма есть MPA рейтинг
        Film film = filmStorage.getById(1L); // Кин-Дза-Дза

        assertThat(film).isNotNull();
        assertThat(film.getMpa()).isNotNull();
    }

    @Test
    void testUserFriends() {
        // Проверяем, что у пользователя есть друзья
        User user = userStorage.getById(1L); // Обломов

        assertThat(user).isNotNull();
        assertThat(user.getFriends()).isNotEmpty();
    }

    @Test
    void testNonExistentData() {
        User nonExistentUser = userStorage.getById(999L);
        Film nonExistentFilm = filmStorage.getById(999L);

        assertThat(nonExistentUser).isNull();
        assertThat(nonExistentFilm).isNull();
    }

    @Test
    void testGetAllGenres() {
        List<Genre> genres = genreStorage.findAll();

        assertThat(genres).hasSize(6);
        assertThat(genres).extracting(Genre::getName)
                .contains("Комедия", "Драма", "Мультфильм", "Триллер", "Документальный", "Боевик");
    }

    @Test
    void testGetGenreById() {
        Optional<Genre> genre = genreStorage.findById(1L);

        assertThat(genre).isPresent();
        assertThat(genre.get().getName()).isEqualTo("Комедия");
    }

    @Test
    void testGetNonExistentGenre() {
        Optional<Genre> genre = genreStorage.findById(999L);

        assertThat(genre).isEmpty();
    }

    @Test
    void testGetAllMpaRatings() {
        List<MpaRating> ratings = mpaStorage.findAll();

        assertThat(ratings).hasSize(5);
        assertThat(ratings).extracting(MpaRating::getId)
                .containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void testGetMpaById() {
        Optional<MpaRating> mpa = mpaStorage.findById(1L);

        assertThat(mpa).isPresent();
        assertThat(mpa.get().getId()).isEqualTo(1L);
        assertThat(mpa.get().getName()).isEqualTo("G");
    }
}