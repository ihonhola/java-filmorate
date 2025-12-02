package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.*;
import java.sql.Date;
import java.util.*;

@Repository
@Qualifier("filmDbStorage")
@Slf4j
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate, FilmRowMapper filmRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.filmRowMapper = filmRowMapper;
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT * FROM films";
        return jdbcTemplate.query(sql, filmRowMapper);
    }

    @Override
    public Film create(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, java.sql.Date.valueOf(film.getReleaseDate()));
            stmt.setInt(4, film.getDuration());
            return stmt;
        }, keyHolder);

        Long filmId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        film.setId(filmId);

        // Сохраняем MPA рейтинг (теперь передаем Long mpaId)
        if (film.getMpa() != null) {
            saveMpaRating(filmId, film.getMpa());
        }

        // Сохраняем жанры (теперь передаем Set<Long> genreIds)
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveGenres(filmId, film.getGenres());
        }

        log.info("Фильм создан в БД с ID: {}", filmId);
        return getById(filmId);
        //return film;
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ? WHERE film_id = ?";

        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getId());

        // Обновляем MPA рейтинг
        updateMpaRating(film.getId(), film.getMpa());

        // Обновляем жанры
        updateGenres(film.getId(), film.getGenres());

        log.info("Фильм обновлен в БД с ID: {}", film.getId());
        return getById(film.getId());
    }

    @Override
    public Film getById(Long id) {
        String sql = "SELECT * FROM films WHERE film_id = ?";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, id);
        return films.isEmpty() ? null : films.get(0);
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM films WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public void delete(Long id) {
        // Сначала удаляем связанные данные
        jdbcTemplate.update("DELETE FROM film_likes WHERE film_id = ?", id);
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", id);
        jdbcTemplate.update("DELETE FROM film_mpa WHERE film_id = ?", id);

        // Затем удаляем фильм
        String sql = "DELETE FROM films WHERE film_id = ?";
        jdbcTemplate.update(sql, id);
        log.info("Фильм удален из БД с ID: {}", id);
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM film_mpa");
        jdbcTemplate.update("DELETE FROM films");
        log.info("Все фильмы удалены из БД");
    }



    private void saveMpaRating(Long filmId, Long mpaId) {
        if (mpaId == null) return;

        // Теперь просто вставляем mpa_id напрямую
        String sql = "INSERT INTO film_mpa (film_id, mpa_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, mpaId);
    }

    private void updateMpaRating(Long filmId, Long mpaId) {
        // Удаляем старый рейтинг
        jdbcTemplate.update("DELETE FROM film_mpa WHERE film_id = ?", filmId);

        // Сохраняем новый
        if (mpaId != null) {
            saveMpaRating(filmId, mpaId);
        }
    }

    private void saveGenres(Long filmId, Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        for (Genre genre : genres) {
            jdbcTemplate.update(sql, filmId, genre.getId());
        }
    }

    private void updateGenres(Long filmId, Set<Genre> genres) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", filmId);

        if (genres != null && !genres.isEmpty()) {
            saveGenres(filmId, genres);
        }
    }

    @Override
    public void addLike(Long filmId, Long userId) {
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
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String deleteSql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        int deletedRows = jdbcTemplate.update(deleteSql, filmId, userId);

        if (deletedRows > 0) {
            log.info("Лайк удален из БД: film_id={}, user_id={}", filmId, userId);
        } else {
            log.warn("Лайк не найден для удаления: film_id={}, user_id={}", filmId, userId);
        }
    }



    private void saveMpaRating(Long filmId, MpaRating mpa) {
        if (mpa == null || mpa.getId() == null) {
            return;
        }

        String sql = "INSERT INTO film_mpa (film_id, mpa_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, mpa.getId());
    }

    private void updateMpaRating(Long filmId, MpaRating mpa) {
        jdbcTemplate.update("DELETE FROM film_mpa WHERE film_id = ?", filmId);

        if (mpa != null && mpa.getId() != null) {
            saveMpaRating(filmId, mpa);
        }
    }
}