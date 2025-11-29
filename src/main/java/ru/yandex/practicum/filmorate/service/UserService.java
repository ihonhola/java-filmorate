package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private final UserStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage,
                       JdbcTemplate jdbcTemplate) {
        this.userStorage = userStorage;
        this.jdbcTemplate = jdbcTemplate;
    }
    /*public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }*/

    /*public User addFriend(Long userId, Long friendId) {
        User user = getUserById(userId);
        User friend = getUserById(friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        log.info("Пользователь с ID {} добавил в друзья пользователя с ID {}", userId, friendId);
        return user;
    } */

    public User addFriend(Long userId, Long friendId) {
        User user = getUserById(userId);
        User friend = getUserById(friendId);

        // Если дружба уже существует
        if (user.getFriends().contains(friendId)) {
            throw new ValidationException("Пользователь уже в друзьях");
        }

        // Добавляем неподтвержденную дружбу
        //user.getFriends().put(friendId, FriendshipStatus.PENDING);
        // У друга тоже добавляем неподтвержденную дружбу
        // friend.getFriends().put(userId, FriendshipStatus.PENDING);
        user.getFriends().add(friendId);

        // Сохраняем в БД
        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'CONFIRMED')";
        jdbcTemplate.update(sql, userId, friendId);

        log.info("Пользователь с ID {} отправил запрос на дружбу пользователю с ID {}", userId, friendId);
        return user;
    }

    public User removeFriend(Long userId, Long friendId) {
        // Проверяем существование обоих пользователей
        User user = getUserById(userId);
        getUserById(friendId);

        // Удаляем из коллекции
        user.getFriends().remove(friendId);

        // Удаляем из БД
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);

        log.info("Удаление дружбы между пользователями {} и {} выполнено", userId, friendId);
        return user;
    }

    public List<User> getFriends(Long userId) {
        User user = getUserById(userId);

        return user.getFriends().stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        User user = getUserById(userId);
        User otherUser = getUserById(otherUserId);

        return user.getFriends().stream()
                .filter(friendId -> otherUser.getFriends().contains(friendId))
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    private User getUserById(Long userId) {
        User user = userStorage.getById(userId);
        if (user == null) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        return user;
    }

    public List<User> findAll() {
        return new ArrayList<>(userStorage.findAll());
    }

    public User create(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            log.debug("Имя пользователя не указано, установлен логин: {}", user.getLogin());
            user.setName(user.getLogin());
        }
        return userStorage.create(user);
    }

    public User update(User user) {
        if (user.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }

        if (!userStorage.existsById(user.getId())) {
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            log.debug("Имя пользователя с ID {} не указано, установлен логин: {}",
                    user.getId(), user.getLogin());
            user.setName(user.getLogin());
        }

        return userStorage.update(user);
    }

    public User getById(Long id) {
        return getUserById(id);
    }

    public void delete(Long id) {
        getUserById(id);
        userStorage.delete(id);
        log.info("Пользователь с ID {} успешно удален", id);
    }

    public void deleteAll() {
        userStorage.deleteAll();
        log.info("Все пользователи успешно удалены");
    }

    public boolean existsById(Long id) {
        return userStorage.existsById(id);
    }

    /*public List<User> getFriendRequests(Long userId) {
        User user = getUserById(userId);

        return user.getFriends().entrySet().stream()
                .filter(entry -> entry.getValue() == FriendshipStatus.PENDING)
                .map(Map.Entry::getKey)
                .map(this::getUserById)
                .collect(Collectors.toList());
    }*/

    /*public User confirmFriend(Long userId, Long friendId) {
        User user = getUserById(userId);
        User friend = getUserById(friendId);

        // Обновляем статус дружбы в БД
        String sql = "UPDATE friendships SET status = ? WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, FriendshipStatus.CONFIRMED.toString(), userId, friendId);
        jdbcTemplate.update(sql, FriendshipStatus.CONFIRMED.toString(), friendId, userId);

        // Обновляем объекты
        user.getFriends().put(friendId, FriendshipStatus.CONFIRMED);
        friend.getFriends().put(userId, FriendshipStatus.CONFIRMED);

        log.info("Пользователь с ID {} подтвердил дружбу с пользователем с ID {}", userId, friendId);
        return user;
    }*/
}