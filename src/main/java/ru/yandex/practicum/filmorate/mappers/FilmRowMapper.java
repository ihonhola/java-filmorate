package ru.yandex.practicum.filmorate.mappers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;

@Component
@RequiredArgsConstructor
@Slf4j
public class FilmRowMapper implements RowMapper<Film> {

    private final MpaDbStorage mpaDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getLong("film_id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        // Загружаем лайки
        film.setLikes(loadLikes(film.getId()));

        // Загружаем жанры (отсортированные)
        film.setGenres(loadGenres(film.getId()));

        // Загружаем MPA
        film.setMpa(loadMpa(film.getId()));

        return film;
    }

    private Set<Long> loadLikes(Long filmId) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";

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

    private MpaRating loadMpa(Long filmId) {
        String sql = "SELECT mr.mpa_id, mr.name, mr.description " +
                "FROM film_mpa fm " +
                "JOIN mpa_ratings mr ON fm.mpa_id = mr.mpa_id " +
                "WHERE fm.film_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new RowMapper<MpaRating>() {
                @Override
                public MpaRating mapRow(ResultSet rs, int rowNum) throws SQLException {
                    MpaRating mpa = new MpaRating();
                    mpa.setId(rs.getLong("mpa_id"));
                    mpa.setName(rs.getString("name"));
                    mpa.setDescription(rs.getString("description"));
                    return mpa;
                }
            }, filmId);
        } catch (Exception e) {
            log.warn("MPA рейтинг не найден для фильма {}: {}", filmId, e.getMessage());
            return null;
        }
    }

    private Set<Genre> loadGenres(Long filmId) {
        String sql = "SELECT g.genre_id, g.name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id = ? ORDER BY g.genre_id";
        try {
            List<Genre> genres = jdbcTemplate.query(sql, new RowMapper<Genre>() {
                @Override
                public Genre mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Genre genre = new Genre();
                    genre.setId(rs.getLong("genre_id"));
                    genre.setName(rs.getString("name"));
                    return genre;
                }
            }, filmId);
            return new TreeSet<>(Comparator.comparing(Genre::getId)) {{
                addAll(genres);
            }};
        } catch (Exception e) {
            log.warn("Жанры не найдены для фильма {}: {}", filmId, e.getMessage());
            return new HashSet<>();
        }
    }
}