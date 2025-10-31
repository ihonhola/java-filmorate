package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
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

        if (user.getName() == null || user.getName().isBlank()) {
            log.debug("Имя пользователя не указано, установлен логин: {}", user.getLogin());
            user.setName(user.getLogin());
        }

        user.setId(getNextId());
        users.put(user.getId(), user);

        log.info("Пользователь успешно создан с ID: {}. Email: {}, Логин: {}",
                user.getId(), user.getEmail(), user.getLogin());

        return user;
    }

    @Override
    public User update(User user) {
        log.info("Получен запрос на обновление пользователя: {}", user);

        if (users.containsKey(user.getId())) {
            User oldUser = users.get(user.getId());
            log.debug("Найден пользователь для обновления: {}", oldUser);

            oldUser.setEmail(user.getEmail());
            oldUser.setLogin(user.getLogin());

            if (user.getName() == null || user.getName().isBlank()) {
                log.debug("Имя пользователя с ID {} не указано, установлен логин: {}",
                        user.getId(), user.getLogin());
                oldUser.setName(user.getLogin());
            } else {
                oldUser.setName(user.getName());
            }

            oldUser.setBirthday(user.getBirthday());
            log.info("Пользователь с ID {} успешно обновлен. Новые данные: Email: {}, Логин: {}",
                    user.getId(), user.getEmail(), user.getLogin());
            return oldUser;
        }

        return null;
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
        User removedUser = users.remove(id);
        if (removedUser != null) {
            log.info("Пользователь с ID {} успешно удален. Логин: {}", id, removedUser.getLogin());
        } else {
            log.warn("Попытка удаления несуществующего пользователя с ID: {}", id);
        }
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
}