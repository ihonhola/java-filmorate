package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Repository
@Qualifier("userDbStorage")
@Slf4j
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper userRowMapper;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate, UserRowMapper userRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRowMapper = userRowMapper;
    }

    @Override
    public Collection<User> findAll() {
        String sql = "SELECT * FROM users";
        List<User> users = jdbcTemplate.query(sql, userRowMapper);

        // Загружаем друзей для всех пользователей
        loadFriendsForUsers(users);
        return users;
    }

    @Override
    public User create(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getName());
            stmt.setDate(4, Date.valueOf(user.getBirthday()));
            return stmt;
        }, keyHolder);

        Long userId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        user.setId(userId);

        log.info("Пользователь создан в БД с ID: {}", userId);
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";

        jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId());

        log.info("Пользователь обновлен в БД с ID: {}", user.getId());
        return user;
    }

    @Override
    public User getById(Long id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, id);

        if (users.isEmpty()) {
            return null;
        }

        User user = users.get(0);

        // Загружаем друзей для этого пользователя
        user.setFriends(loadFriends(user.getId()));

        return user;
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public void delete(Long id) {
        // Удаляем связанные данные
        jdbcTemplate.update("DELETE FROM film_likes WHERE user_id = ?", id);
        jdbcTemplate.update("DELETE FROM friendships WHERE user_id = ? OR friend_id = ?", id, id);

        // Удаляем пользователя
        String sql = "DELETE FROM users WHERE user_id = ?";
        jdbcTemplate.update(sql, id);
        log.info("Пользователь удален из БД с ID: {}", id);
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM friendships");
        jdbcTemplate.update("DELETE FROM users");
        log.info("Все пользователи удалены из БД");
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        // Проверяем, не существует ли уже дружба
        String checkSql = "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?";
        Integer existing = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId);

        if (existing == null || existing == 0) {
            String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'CONFIRMED')";
            jdbcTemplate.update(sql, userId, friendId);
            log.info("Дружба добавлена: user_id={}, friend_id={}", userId, friendId);
        } else {
            log.info("Дружба уже существует: user_id={}, friend_id={}", userId, friendId);
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        int deletedRows = jdbcTemplate.update(sql, userId, friendId);

        if (deletedRows > 0) {
            log.info("Дружба удалена: user_id={}, friend_id={}", userId, friendId);
        } else {
            log.warn("Дружба не найдена для удаления: user_id={}, friend_id={}", userId, friendId);
        }
    }

    @Override
    public List<User> getFriends(Long userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f ON u.user_id = f.friend_id " +
                "WHERE f.user_id = ? AND f.status = 'CONFIRMED'";

        List<User> friends = jdbcTemplate.query(sql, userRowMapper, userId);

        // Загружаем друзей для каждого друга
        loadFriendsForUsers(friends);

        return friends;
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f1 ON u.user_id = f1.friend_id " +
                "JOIN friendships f2 ON u.user_id = f2.friend_id " +
                "WHERE f1.user_id = ? AND f2.user_id = ? " +
                "AND f1.status = 'CONFIRMED' AND f2.status = 'CONFIRMED'";

        List<User> commonFriends = jdbcTemplate.query(sql, userRowMapper, userId, otherUserId);

        // Загружаем друзей для общих друзей
        loadFriendsForUsers(commonFriends);

        return commonFriends;
    }

    private Set<Long> loadFriends(Long userId) {
        String sql = "SELECT friend_id FROM friendships WHERE user_id = ? AND status = 'CONFIRMED'";
        List<Long> friendIds = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getLong("friend_id"),
                userId);
        return new HashSet<>(friendIds);
    }

    // Загружаем друзей для списка пользователей
    private void loadFriendsForUsers(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        // Собираем все ID пользователей
        Set<Long> userIds = new HashSet<>();
        for (User user : users) {
            userIds.add(user.getId());
        }

        // Загружаем все связи дружбы одним запросом
        Map<Long, Set<Long>> allFriends = loadAllFriends(userIds);

        // Устанавливаем друзей каждому пользователю
        for (User user : users) {
            user.setFriends(allFriends.getOrDefault(user.getId(), new HashSet<>()));
        }
    }

    // Загружаем всех друзей для набора пользователей
    private Map<Long, Set<Long>> loadAllFriends(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));
        String sql = "SELECT user_id, friend_id FROM friendships WHERE user_id IN (" + placeholders + ") AND status = 'CONFIRMED'";

        Map<Long, Set<Long>> friendsMap = new HashMap<>();

        jdbcTemplate.query(sql, userIds.toArray(), rs -> {
            Long userId = rs.getLong("user_id");
            Long friendId = rs.getLong("friend_id");
            friendsMap.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        });

        return friendsMap;
    }
}