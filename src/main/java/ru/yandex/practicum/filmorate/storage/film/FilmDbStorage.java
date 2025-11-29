package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;

import java.sql.*;
import java.sql.Date;
import java.util.*;

@Repository
@Qualifier("filmDbStorage")
@Slf4j
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Film> filmRowMapper = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getLong("film_id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        /*
        // Загружаем лайки
        Set<Long> likes = loadLikes(film.getId());
        film.setLikes(likes);

        // Загружаем жанры
        Set<Long> genresIds = loadGenreIds(film.getId());
        film.setGenres(genresIds);

        // Загружаем MPA рейтинг
        Long mpaId = loadMpaId(film.getId());
        film.setMpa(mpaId);
        */
        film.setLikes(loadLikes(film.getId()));
        film.setGenres(loadGenreIds(film.getId()));
        film.setMpa(loadMpaId(film.getId()));
        return film;
    };

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

    private Set<Long> loadLikes(Long filmId) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";
        /*return new HashSet<>(jdbcTemplate.query(sql, (rs, rowNum) ->
                rs.getLong("user_id"), filmId));*/
        try {
            List<Long> likes = jdbcTemplate.query(sql, (rs, rowNum) ->
                    rs.getLong("user_id"), filmId);
            log.debug("Загружено {} лайков для фильма {}", likes.size(), filmId);
            return new HashSet<>(likes);
        } catch (Exception e) {
            log.error("Ошибка при загрузке лайков для фильма {}: {}", filmId, e.getMessage());
            return new HashSet<>();
        }
    }

    /*private Set<Genre> loadGenres(Long filmId) {
        String sql = "SELECT g.genre_id, g.name FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id = ?";
        return new HashSet<>(jdbcTemplate.query(sql, (rs, rowNum) ->
                Genre.fromId(rs.getLong("genre_id")), filmId));
    }*/

    /*private MpaRating loadMpaRating(Long filmId) {
        String sql = "SELECT m.code FROM film_mpa fm " +
                "JOIN mpa_ratings m ON fm.mpa_id = m.mpa_id " +
                "WHERE fm.film_id = ?";
        try {
            String code = jdbcTemplate.queryForObject(sql, String.class, filmId);
            return MpaRating.fromCode(code);
        } catch (Exception e) {
            return null;
        }
    }*/

    /*private void saveMpaRating(Long filmId, MpaRating mpa) {
        String sql = "INSERT INTO film_mpa (film_id, mpa_id) VALUES (?, " +
                "(SELECT mpa_id FROM mpa_ratings WHERE code = ?))";
        jdbcTemplate.update(sql, filmId, mpa.getCode());
    }*/
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

    private void saveGenres(Long filmId, Set<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) return;

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        for (Long genreId : genreIds) {
            jdbcTemplate.update(sql, filmId, genreId);
        }
    }

    private void updateGenres(Long filmId, Set<Long> genreIds) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", filmId);

        if (genreIds != null && !genreIds.isEmpty()) {
            saveGenres(filmId, genreIds);
        }
    }

    private Set<Long> loadGenreIds(Long filmId) {
        String sql = "SELECT genre_id FROM film_genres WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.query(sql, (rs, rowNum) ->
                rs.getLong("genre_id"), filmId));
    }

    private Long loadMpaId(Long filmId) {
        String sql = "SELECT mpa_id FROM film_mpa WHERE film_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, filmId);
        } catch (Exception e) {
            return null;
        }
    }

    public Film loadFilmWithObjects(Long filmId) {
        Film film = getById(filmId);

        // Конвертируем ID в объекты для ответа
        if (film.getMpa() != null) {
            // Здесь можно добавить логику для возврата объекта Mpa
            // Но для простоты оставляем как есть
        }

        return film;
    }
}