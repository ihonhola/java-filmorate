package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;

public interface UserStorage {
    Collection<User> findAll();

    User create(User user);

    User update(User user);

    User getById(Long id);

    boolean existsById(Long id);

    User delete(Long id);

    void deleteAll();
}