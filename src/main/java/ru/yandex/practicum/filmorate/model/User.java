package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.time.LocalDate;

@Data
public class User {
    private Long id;

    @Email(message = "Email должен быть корректным адресом электронной почты")
    private String email;

    private String login;
    private String name;
    private LocalDate birthday;
}
