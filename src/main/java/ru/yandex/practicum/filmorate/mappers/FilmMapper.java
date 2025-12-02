package ru.yandex.practicum.filmorate.mappers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dto.FilmResponse;
import ru.yandex.practicum.filmorate.dto.GenreResponse;
import ru.yandex.practicum.filmorate.dto.MpaResponse;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.service.GenreService;
import ru.yandex.practicum.filmorate.service.MpaService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FilmMapper {

    @Autowired
    private MpaService mpaService;

    @Autowired
    private GenreService genreService;

    public Film mapToFilm(Map<String, Object> request) {
        if (request == null) {
            throw new IllegalArgumentException("Request map cannot be null");
        }

        Film film = new Film();

        // Базовые поля
        film.setName((String) request.get("name"));
        film.setDescription((String) request.get("description"));

        if (request.get("releaseDate") != null) {
            film.setReleaseDate(LocalDate.parse(request.get("releaseDate").toString()));
        }

        if (request.get("duration") != null) {
            film.setDuration(Integer.valueOf(request.get("duration").toString()));
        }

        if (request.get("id") != null) {
            film.setId(Long.valueOf(request.get("id").toString()));
        }

        // Обрабатываем mpa
        if (request.get("mpa") != null) {
            MpaRating mpa = extractMpaFromRequest(request.get("mpa"));
            film.setMpa(mpa);
        }

        // Обрабатываем genres
        if (request.get("genres") != null && request.get("genres") instanceof List) {
            List<Object> genresList = (List<Object>) request.get("genres");
            film.setGenres(new HashSet<>());

            for (Object genreObj : genresList) {
                Genre genre = extractGenreFromRequest(genreObj);
                if (genre != null) {
                    film.getGenres().add(genre);
                }
            }
        }

        return film;
    }

    private MpaRating extractMpaFromRequest(Object mpaObj) {
        if (mpaObj instanceof Map) {
            Map<String, Object> mpaMap = (Map<String, Object>) mpaObj;
            if (mpaMap.get("id") != null) {
                Long mpaId = Long.valueOf(mpaMap.get("id").toString());
                return mpaService.findById(mpaId);
            }
        } else if (mpaObj instanceof Number) {
            Long mpaId = Long.valueOf(mpaObj.toString());
            return mpaService.findById(mpaId);
        }
        return null;
    }

    private Genre extractGenreFromRequest(Object genreObj) {
        if (genreObj instanceof Map) {
            Map<String, Object> genreMap = (Map<String, Object>) genreObj;
            if (genreMap.get("id") != null) {
                Long genreId = Long.valueOf(genreMap.get("id").toString());
                return genreService.findById(genreId);
            }
        } else if (genreObj instanceof Number) {
            Long genreId = Long.valueOf(genreObj.toString());
            return genreService.findById(genreId);
        }
        return null;
    }


    public FilmResponse mapToResponse(Film film) {
        if (film == null) {
            return null;
        }

        FilmResponse response = new FilmResponse();
        response.setId(film.getId());
        response.setName(film.getName());
        response.setDescription(film.getDescription());
        response.setReleaseDate(film.getReleaseDate());
        response.setDuration(film.getDuration());
        response.setLikes(film.getLikes());

        // Конвертируем ID в объекты для ответа
        if (film.getMpa() != null) {
            MpaResponse mpaResponse = new MpaResponse();
            mpaResponse.setId(film.getMpa().getId());
            mpaResponse.setName(film.getMpa().getName());
            response.setMpa(mpaResponse);
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<GenreResponse> genreResponses = new ArrayList<>();

            // Сортируем жанры по ID
            List<Genre> sortedGenres = film.getGenres().stream()
                    .sorted(Comparator.comparing(Genre::getId))
                    .collect(Collectors.toList());

            for (Genre genre : sortedGenres) {
                GenreResponse genreResponse = new GenreResponse();
                genreResponse.setId(genre.getId());
                genreResponse.setName(genre.getName());
                genreResponses.add(genreResponse);
            }
            response.setGenres(genreResponses);
        }

        return response;
    }

    public Map<String, Object> mapToRequest(Film film) {
        if (film == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> request = new HashMap<>();
        request.put("id", film.getId());
        request.put("name", film.getName());
        request.put("description", film.getDescription());
        request.put("releaseDate", film.getReleaseDate());
        request.put("duration", film.getDuration());

        if (film.getMpa() != null) {
            Map<String, Object> mpaMap = new HashMap<>();
            mpaMap.put("id", film.getMpa());
            request.put("mpa", mpaMap);
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Map<String, Object>> genresList = new ArrayList<>();
            for (Genre genre : film.getGenres()) {
                Map<String, Object> genreMap = new HashMap<>();
                genreMap.put("id", genre);
                genresList.add(genreMap);
            }
            request.put("genres", genresList);
        }

        return request;
    }
}