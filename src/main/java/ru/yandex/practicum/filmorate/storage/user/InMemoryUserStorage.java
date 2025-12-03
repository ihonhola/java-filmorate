package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Qualifier("inMemoryUserStorage")
@Slf4j
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1;

    @Override
    public Collection<User> findAll() {
        log.info("Получен запрос на получение всех пользователей. Текущее количество: {}", users.size());
        return users.values();
    }

    @Override
    public User create(User user) {
        log.info("Получен запрос на создание нового пользователя: {}", user);
        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Пользователь успешно создан с ID: {}. Email: {}, Логин: {}",
                user.getId(), user.getEmail(), user.getLogin());
        return user;
    }

    @Override
    public User update(User user) {
        log.info("Получен запрос на обновление пользователя: {}", user);

        User oldUser = users.get(user.getId());
        log.debug("Найден пользователь для обновления: {}", oldUser);

        oldUser.setEmail(user.getEmail());
        oldUser.setLogin(user.getLogin());
        oldUser.setName(user.getName());
        oldUser.setBirthday(user.getBirthday());

        log.info("Пользователь с ID {} успешно обновлен. Новые данные: Email: {}, Логин: {}",
                    user.getId(), user.getEmail(), user.getLogin());

        return oldUser;
    }

    @Override
    public User getById(Long id) {
        return users.get(id);
    }

    @Override
    public boolean existsById(Long id) {
        return users.containsKey(id);
    }

    @Override
    public void delete(Long id) {
        User removedUser = users.get(id);
        // Сначала очищаем связи у друзей
        for (Long friendId : removedUser.getFriends()) {
            User friend = users.get(friendId);
            if (friend != null) {
                friend.getFriends().remove(id);
            }
        }
        // Потом удаляем пользователя
        users.remove(id);
        log.info("Пользователь с ID {} успешно удален. Логин: {}", id, removedUser.getLogin());
    }

    @Override
    public void deleteAll() {
        int count = users.size();
        users.clear();
        log.info("Все пользователи удалены. Удалено {} записей", count);
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        long nextId = ++currentMaxId;
        log.debug("Сгенерирован новый ID: {}", nextId);
        return nextId;
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        User user = users.get(userId);
        User friend = users.get(friendId);

        if (user != null && friend != null) {
            user.getFriends().add(friendId);
            friend.getFriends().add(userId); // Для двусторонней дружбы
            log.info("Пользователь {} добавил в друзья пользователя {}", userId, friendId);
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        User user = users.get(userId);
        User friend = users.get(friendId);

        if (user != null) {
            user.getFriends().remove(friendId);
        }
        if (friend != null) {
            friend.getFriends().remove(userId);
        }
        log.info("Пользователь {} удалил из друзей пользователя {}", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        User user = users.get(userId);
        if (user == null) {
            return Collections.emptyList();
        }

        return user.getFriends().stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        User user = users.get(userId);
        User otherUser = users.get(otherUserId);

        if (user == null || otherUser == null) {
            return Collections.emptyList();
        }

        return user.getFriends().stream()
                .filter(friendId -> otherUser.getFriends().contains(friendId))
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}