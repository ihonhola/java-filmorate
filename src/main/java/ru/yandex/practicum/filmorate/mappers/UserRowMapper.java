package ru.yandex.practicum.filmorate.mappers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRowMapper implements RowMapper<User> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());

        // Загружаем друзей
        user.setFriends(loadFriends(user.getId()));

        return user;
    }

    private Set<Long> loadFriends(Long userId) {
        String sql = "SELECT friend_id FROM friendships WHERE user_id = ? AND status = 'CONFIRMED'";
        return new HashSet<>(jdbcTemplate.query(sql, (rs, rowNum) ->
                rs.getLong("friend_id"), userId));
    }
}