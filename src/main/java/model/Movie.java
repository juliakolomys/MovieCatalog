package model;

import java.util.List;
import java.util.Objects;

public class Movie {
    public final String id;
    public final String title;
    public final int year;
    public final List<String> genres;
    public final List<String> directors;
    public final List<String> actors;
    public final String description;

    public Movie(String id, String title, int year,
                 List<String> genres, List<String> directors,
                 List<String> actors, String description) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.genres = genres;
        this.directors = directors;
        this.actors = actors;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id:'" + id + '\'' +
                ", title:'" + title + '\'' +
                ", year " + year +
                ", genres:" + genres +
                ", directors:" + directors +
                ", actors:" + actors +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movie)) return false;
        Movie movie = (Movie) o;
        return Objects.equals(id, movie.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
