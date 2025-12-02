package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User addFriend(Long userId, Long friendId) {
        User user = getUserById(userId);
        User friend = getUserById(friendId);

        // Если дружба уже существует
        if (user.getFriends().contains(friendId)) {
            throw new ValidationException("Пользователь уже в друзьях");
        }

        // Вызываем метод хранилища для добавления дружбы
        userStorage.addFriend(userId, friendId);

        // Добавляем неподтвержденную дружбу
        user.getFriends().add(friendId);

        log.info("Пользователь с ID {} отправил запрос на дружбу пользователю с ID {}", userId, friendId);
        return user;
    }

    public User removeFriend(Long userId, Long friendId) {
        // Проверяем существование обоих пользователей
        User user = getUserById(userId);
        getUserById(friendId);

        // Удаляем дружбу через хранилище
        userStorage.removeFriend(userId, friendId);

        // Удаляем из коллекции
        user.getFriends().remove(friendId);

        log.info("Удаление дружбы между пользователями {} и {} выполнено", userId, friendId);
        return user;
    }

    public List<User> getFriends(Long userId) {
        User user = getUserById(userId);

        return userStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        User user = getUserById(userId);
        User otherUser = getUserById(otherUserId);

        return userStorage.getCommonFriends(userId, otherUserId);
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
}