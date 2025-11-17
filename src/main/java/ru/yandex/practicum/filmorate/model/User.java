package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
//import java.util.HashSet;
//import java.util.Set;

@Data
public class User {
    private Long id;

    @NotBlank(message = "Email не может быть пустым")
    @NotNull(message = "Email не может быть пустым")
    @Email(message = "Email должен быть корректным адресом электронной почты")
    private String email; //оставил ошибки в контроллерах, чтобы пройти тесты в Postman

    @NotBlank(message = "Логин не может быть пустым")
    @NotNull (message = "Email не может быть пустым")
    private String login;

    private String name;

    @PastOrPresent(message = "Дата рождения не может быть в будущем")
    private LocalDate birthday;

    //private Set<Long> friends = new HashSet<>();
    private Map<Long, FriendshipStatus> friends = new HashMap<>();
}
