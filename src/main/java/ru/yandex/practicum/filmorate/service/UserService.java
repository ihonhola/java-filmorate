package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

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
        if (user.getFriends().containsKey(friendId)) {
            throw new ValidationException("Пользователь уже в друзьях");
        }

        // Добавляем неподтвержденную дружбу
        user.getFriends().put(friendId, FriendshipStatus.PENDING);
        // У друга тоже добавляем неподтвержденную дружбу
        friend.getFriends().put(userId, FriendshipStatus.PENDING);

        log.info("Пользователь с ID {} отправил запрос на дружбу пользователю с ID {}", userId, friendId);
        return user;
    }

    public User removeFriend(Long userId, Long friendId) {
        User user = getUserById(userId);
        User friend = getUserById(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        log.info("Пользователь с ID {} удалил из друзей пользователя с ID {}", userId, friendId);
        return user;
    }

    public List<User> getFriends(Long userId) {
        User user = getUserById(userId);

        return user.getFriends().keySet().stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        User user = getUserById(userId);
        User otherUser = getUserById(otherUserId);

        return user.getFriends().keySet().stream()
                .filter(friendId -> otherUser.getFriends().containsKey(friendId))
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

    public User confirmFriend(Long userId, Long friendId) {
        User user = getUserById(userId);
        User friend = getUserById(friendId);

        // Проверяем, что есть запрос на дружбу
        if (!user.getFriends().containsKey(friendId) ||
                user.getFriends().get(friendId) != FriendshipStatus.PENDING) {
            throw new ValidationException("Запрос на дружбу не найден");
        }

        // Подтверждаем дружбу у обоих пользователей
        user.getFriends().put(friendId, FriendshipStatus.CONFIRMED);
        friend.getFriends().put(userId, FriendshipStatus.CONFIRMED);

        log.info("Пользователь с ID {} подтвердил дружбу с пользователем с ID {}", userId, friendId);
        return user;
    }

    public List<User> getFriendRequests(Long userId) {
        User user = getUserById(userId);

        return user.getFriends().entrySet().stream()
                .filter(entry -> entry.getValue() == FriendshipStatus.PENDING)
                .map(Map.Entry::getKey)
                .map(this::getUserById)
                .collect(Collectors.toList());
    }
}