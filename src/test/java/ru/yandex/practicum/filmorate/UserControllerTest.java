package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;


public class UserControllerTest {
    private User validUser;
    private UserController userController;
    private InMemoryUserStorage userStorage;
    private UserService userService;

    @BeforeEach
    void setUp() {
        validUser = new User();
        userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage, null);
        userController = new UserController(userService);
        validUser.setEmail("test@mail.ru");
        validUser.setLogin("validlogin");
        validUser.setName("Test User");
        validUser.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @AfterEach
    void zero() {
        userStorage.deleteAll();
        userController = null;
        validUser = null;
        userService = null;
        userStorage = null;
    }

    @Test
    void createUser_withValidData_shouldSucceed() throws Exception {
        User createdUser = userController.create(validUser);
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals("test@mail.ru", createdUser.getEmail());
        assertEquals("validlogin", createdUser.getLogin());
        assertEquals("Test User", createdUser.getName());
        assertEquals(LocalDate.of(1990, 1, 1), createdUser.getBirthday());
    }

    @Test
    void createUser_withNullEmail_shouldThrowValidationException() {
        User user = new User();
        user.setEmail(null);
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Почта не может быть пустой и должна содержать символ @", exception.getMessage());
    }

    @Test
    void createUser_withEmptyEmail_shouldThrowValidationException() {
        User user = new User();
        user.setEmail("");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Почта не может быть пустой и должна содержать символ @", exception.getMessage());
    }

    @Test
    void createUser_withEmailWithoutAt_shouldThrowValidationException() {
        User user = new User();
        user.setEmail("invalid-email");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Почта не может быть пустой и должна содержать символ @", exception.getMessage());
    }

    @Test
    void createUser_withNullLogin_shouldThrowValidationException() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin(null);
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Логин не может быть пустым и содержать пробелы", exception.getMessage());
    }

    @Test
    void createUser_withEmptyLogin_shouldThrowValidationException() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Логин не может быть пустым и содержать пробелы", exception.getMessage());
    }

    @Test
    void createUser_withLoginContainingSpaces_shouldThrowValidationException() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("login with spaces");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Логин не может быть пустым и содержать пробелы", exception.getMessage());
    }

    @Test
    void createUser_withFutureBirthday_shouldThrowValidationException() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Дата рождения не может быть в будущем", exception.getMessage());
    }

    @Test
    void createUser_withNullName_shouldSetLoginAsName() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("validlogin");
        user.setName(null);
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userController.create(user);

        assertEquals("validlogin", createdUser.getName());
    }

    @Test
    void createUser_withEmptyName_shouldSetLoginAsName() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("validlogin");
        user.setName("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userController.create(user);

        assertEquals("validlogin", createdUser.getName());
    }

    @Test
    void createUser_withBlankName_shouldSetLoginAsName() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("validlogin");
        user.setName("   ");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userController.create(user);

        assertEquals("validlogin", createdUser.getName());
    }

    @Test
    void updateUser_withValidData_shouldSucceed() {
        User createdUser = userController.create(validUser);
        Long userId = createdUser.getId();

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setEmail("updated@mail.ru");
        updateUser.setLogin("updatedlogin");
        updateUser.setName("Updated Name");
        updateUser.setBirthday(LocalDate.of(1995, 5, 5));

        User updatedUser = userController.update(updateUser);

        assertNotNull(updatedUser);
        assertEquals(userId, updatedUser.getId());
        assertEquals("updated@mail.ru", updatedUser.getEmail());
        assertEquals("updatedlogin", updatedUser.getLogin());
        assertEquals("Updated Name", updatedUser.getName());
        assertEquals(LocalDate.of(1995, 5, 5), updatedUser.getBirthday());
    }

    @Test
    void updateUser_withoutId_shouldThrowValidationException() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.update(user));
        assertEquals("Id должен быть указан", exception.getMessage());
    }

    @Test
    void updateUser_nonExistentUser_shouldThrowNotFoundException() {
        User user = new User();
        user.setId(999L);
        user.setEmail("test@mail.ru");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userController.update(user));
        assertEquals("Пользователь с id = 999 не найден", exception.getMessage());
    }

    @Test
    void updateUser_withInvalidEmail_shouldThrowValidationException() {
        User createdUser = userController.create(validUser);
        Long userId = createdUser.getId();

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setEmail("invalid-email");
        updateUser.setLogin("validlogin");
        updateUser.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.update(updateUser));
        assertEquals("Почта не может быть пустой и должна содержать символ @", exception.getMessage());
    }

    @Test
    void updateUser_withInvalidLogin_shouldThrowValidationException() {
        User createdUser = userController.create(validUser);
        Long userId = createdUser.getId();

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setEmail("test@mail.ru");
        updateUser.setLogin("invalid login");
        updateUser.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.update(updateUser));
        assertEquals("Логин не может быть пустым и содержать пробелы", exception.getMessage());
    }

    @Test
    void updateUser_withFutureBirthday_shouldThrowValidationException() {
        User createdUser = userController.create(validUser);
        Long userId = createdUser.getId();

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setEmail("test@mail.ru");
        updateUser.setLogin("validlogin");
        updateUser.setBirthday(LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.update(updateUser));
        assertEquals("Дата рождения не может быть в будущем", exception.getMessage());
    }

    @Test
    void updateUser_withNullName_shouldSetLoginAsName() {
        User createdUser = userController.create(validUser);
        Long userId = createdUser.getId();

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setEmail("test@mail.ru");
        updateUser.setLogin("newlogin");
        updateUser.setName(null);
        updateUser.setBirthday(LocalDate.of(1990, 1, 1));

        User updatedUser = userController.update(updateUser);

        assertEquals("newlogin", updatedUser.getName());
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        User user1 = userController.create(validUser);

        User user2 = new User();
        user2.setEmail("second@yandex.ru");
        user2.setLogin("secondlogin");
        user2.setBirthday(LocalDate.of(1985, 5, 5));
        userController.create(user2);

        Collection<User> users = userController.findAll();

        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.getEmail().equals("test@mail.ru")));
        assertTrue(users.stream().anyMatch(u -> u.getEmail().equals("second@yandex.ru")));
    }

    @Test
    void createUser_withNullUser_shouldThrowValidationException() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(null));
        assertNotNull(exception);
    }
}