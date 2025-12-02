package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<MpaRating> mpaRowMapper = new RowMapper<MpaRating>() {
        @Override
        public MpaRating mapRow(ResultSet rs, int rowNum) throws SQLException {
            MpaRating mpa = new MpaRating();
            mpa.setId(rs.getLong("mpa_id"));
            mpa.setName(rs.getString("name"));
            mpa.setDescription(rs.getString("description"));
            return mpa;
        }
    };

    public List<MpaRating> findAll() {
        String sql = "SELECT * FROM mpa_ratings ORDER BY mpa_id";
        return jdbcTemplate.query(sql, mpaRowMapper);
    }

    public Optional<MpaRating> findById(Long id) {
        String sql = "SELECT * FROM mpa_ratings WHERE mpa_id = ?";
        List<MpaRating> ratings = jdbcTemplate.query(sql, mpaRowMapper, id);
        return ratings.isEmpty() ? Optional.empty() : Optional.of(ratings.get(0));
    }

    public Optional<MpaRating> findByName(String name) {
        String sql = "SELECT * FROM mpa_ratings WHERE name = ?";
        List<MpaRating> ratings = jdbcTemplate.query(sql, mpaRowMapper, name);
        return ratings.isEmpty() ? Optional.empty() : Optional.of(ratings.get(0));
    }
}