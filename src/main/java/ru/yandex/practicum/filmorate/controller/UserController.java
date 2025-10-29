package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final Map<Long, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> findAll() {
        log.info("Получен запрос на получение всех пользователей. Текущее количество: {}", users.size());
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        log.info("Получен запрос на создание нового пользователя: {}", user);

        validateUser(user);

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

    private void validateUser(User user) {
        if (user == null) {
            String errorMessage = "Тело запроса не может быть пустым";
            log.warn("Ошибка валидации: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            String errorMessage = "Почта не может быть пустой и должна содержать символ @";
            log.warn("Ошибка валидации при создании пользователя: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            String errorMessage = "Логин не может быть пустым и содержать пробелы";
            log.warn("Ошибка валидации при создании пользователя: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (user.getBirthday().isAfter(LocalDate.now())) {
            String errorMessage = "Дата рождения не может быть в будущем";
            log.warn("Ошибка валидации при создании пользователя: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }
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

    @PutMapping
    public User update(@RequestBody User newUser) {
        log.info("Получен запрос на обновление пользователя: {}", newUser);

        if (newUser.getId() == null) {
            String errorMessage = "Id должен быть указан";
            log.warn("Ошибка валидации при обновлении пользователя: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (users.containsKey(newUser.getId())) {
            User oldUser = users.get(newUser.getId());
            log.debug("Найден пользователь для обновления: {}", oldUser);

            validateUser(newUser);

            oldUser.setEmail(newUser.getEmail());
            oldUser.setLogin(newUser.getLogin());

            if (newUser.getName() == null || newUser.getName().isBlank()) {
                log.debug("Имя пользователя с ID {} не указано, установлен логин: {}",
                        newUser.getId(), newUser.getLogin());
                oldUser.setName(newUser.getLogin());
            } else {
                oldUser.setName(newUser.getName());
            }

            oldUser.setBirthday(newUser.getBirthday());
            log.info("Пользователь с ID {} успешно обновлен. Новые данные: Email: {}, Логин: {}",
                    newUser.getId(), newUser.getEmail(), newUser.getLogin());
            return oldUser;
        }

        String errorMessage = "Пользователь с id = " + newUser.getId() + " не найден";
        log.warn("Ошибка при обновлении пользователя: {}", errorMessage);
        throw new NotFoundException(errorMessage);
    }
}
