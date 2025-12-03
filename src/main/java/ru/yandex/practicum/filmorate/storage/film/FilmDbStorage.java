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

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper);

        // Загружаем связанные данные для всех фильмов
        loadRelatedDataForFilms(films);

        return films;
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

        // Сохраняем MPA рейтинг
        if (film.getMpa() != null) {
            saveMpaRating(filmId, film.getMpa());
        }

        // Сохраняем жанры
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveGenres(filmId, film.getGenres());
        }

        log.info("Фильм создан в БД с ID: {}", filmId);
        return film;
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
        return film;
    }

    @Override
    public Film getById(Long id) {
        String sql = "SELECT * FROM films WHERE film_id = ?";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, id);

        if (films.isEmpty()) {
            return null;
        }

        Film film = films.get(0);
        // Загружаем связанные данные для этого фильма
        loadRelatedDataForFilm(film);

        return film;
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

    // Загрузка связанных данных для одного фильма
    private void loadRelatedDataForFilm(Film film) {
        if (film == null) {
            return;
        }

        // Загружаем лайки
        film.setLikes(loadLikes(film.getId()));

        // Загружаем жанры
        film.setGenres(loadGenres(film.getId()));

        // Загружаем MPA
        film.setMpa(loadMpa(film.getId()));
    }

    // Загрузка связанных данных для списка фильмов
    private void loadRelatedDataForFilms(List<Film> films) {
        if (films == null || films.isEmpty()) {
            return;
        }

        // Собираем все ID фильмов
        Set<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toSet());

        // Загружаем все лайки одним запросом
        Map<Long, Set<Long>> allLikes = loadAllLikes(filmIds);

        // Загружаем все жанры одним запросом
        Map<Long, Set<Genre>> allGenres = loadAllGenres(filmIds);

        // Загружаем все MPA одним запросом
        Map<Long, MpaRating> allMpaRatings = loadAllMpaRatings(filmIds);

        // Устанавливаем связанные данные каждому фильму
        for (Film film : films) {
            film.setLikes(allLikes.getOrDefault(film.getId(), new HashSet<>()));
            film.setGenres(allGenres.getOrDefault(film.getId(), new HashSet<>()));
            film.setMpa(allMpaRatings.get(film.getId()));
        }
    }

    private Map<Long, Set<Long>> loadAllLikes(Set<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String sql = "SELECT film_id, user_id FROM film_likes WHERE film_id IN (" + placeholders + ")";

        Map<Long, Set<Long>> likesMap = new HashMap<>();

        jdbcTemplate.query(sql, filmIds.toArray(), rs -> {
            Long filmId = rs.getLong("film_id");
            Long userId = rs.getLong("user_id");
            likesMap.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        });

        return likesMap;
    }

    private Map<Long, Set<Genre>> loadAllGenres(Set<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String sql = "SELECT fg.film_id, g.genre_id, g.name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id IN (" + placeholders + ") " +
                "ORDER BY g.genre_id";

        Map<Long, Set<Genre>> genresMap = new HashMap<>();

        jdbcTemplate.query(sql, filmIds.toArray(), rs -> {
            Long filmId = rs.getLong("film_id");
            Long genreId = rs.getLong("genre_id");
            String genreName = rs.getString("name");

            Genre genre = new Genre(genreId, genreName);
            genresMap.computeIfAbsent(filmId, k -> new TreeSet<>(Comparator.comparing(Genre::getId)))
                    .add(genre);
        });

        return genresMap;
    }

    private Map<Long, MpaRating> loadAllMpaRatings(Set<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String sql = "SELECT fm.film_id, mr.mpa_id, mr.name, mr.description " +
                "FROM film_mpa fm " +
                "JOIN mpa_ratings mr ON fm.mpa_id = mr.mpa_id " +
                "WHERE fm.film_id IN (" + placeholders + ")";

        Map<Long, MpaRating> mpaMap = new HashMap<>();

        jdbcTemplate.query(sql, filmIds.toArray(), rs -> {
            Long filmId = rs.getLong("film_id");
            Long mpaId = rs.getLong("mpa_id");
            String name = rs.getString("name");
            String description = rs.getString("description");

            MpaRating mpa = new MpaRating(mpaId, name, description);
            mpaMap.put(filmId, mpa);
        });

        return mpaMap;
    }

    private Set<Long> loadLikes(Long filmId) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";
        List<Long> likes = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getLong("user_id"),
                filmId);
        return new HashSet<>(likes);
    }

    private Set<Genre> loadGenres(Long filmId) {
        String sql = "SELECT g.genre_id, g.name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id = ? ORDER BY g.genre_id";

        List<Genre> genres = jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    Genre genre = new Genre();
                    genre.setId(rs.getLong("genre_id"));
                    genre.setName(rs.getString("name"));
                    return genre;
                },
                filmId);

        return new TreeSet<>(Comparator.comparing(Genre::getId)) {{
            addAll(genres);
        }};
    }

    private MpaRating loadMpa(Long filmId) {
        String sql = "SELECT mr.mpa_id, mr.name, mr.description " +
                "FROM film_mpa fm " +
                "JOIN mpa_ratings mr ON fm.mpa_id = mr.mpa_id " +
                "WHERE fm.film_id = ?";

        try {
            return jdbcTemplate.queryForObject(sql,
                    (rs, rowNum) -> {
                        MpaRating mpa = new MpaRating();
                        mpa.setId(rs.getLong("mpa_id"));
                        mpa.setName(rs.getString("name"));
                        mpa.setDescription(rs.getString("description"));
                        return mpa;
                    },
                    filmId);
        } catch (Exception e) {
            log.warn("MPA рейтинг не найден для фильма {}: {}", filmId, e.getMessage());
            return null;
        }
    }
}