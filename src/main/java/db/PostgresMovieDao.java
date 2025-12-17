package db;

import exceptions.DataAccessException;
import model.Movie;
import utils.MovieRowMapper;
import utils.StringListConverter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PostgresMovieDao implements MovieDao {

    private final Connection connection;
    private final MovieRowMapper mapper;
    private final StringListConverter converter;

    public PostgresMovieDao(Connection connection, MovieRowMapper mapper, StringListConverter converter) {
        this.connection = connection;
        this.mapper = mapper;
        this.converter = converter;
    }


    @Override
    public Movie findById(int id) {
        String sql = "SELECT id, title, release_year, genres, directors, actors, description FROM movies WHERE id = ?";
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) return null;
                return mapper.mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find movie with ID " + id, e);
        }
    }

    @Override
    public List<Movie> findAll(int limit) {
        String sql = "SELECT id, title, release_year, genres, directors, actors, description FROM movies LIMIT ?";
        List<Movie> list = new ArrayList<>();
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, limit);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapper.mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find all movies", e);
        }
        return list;
    }

    @Override
    public void save(Movie movie) {
        String sql = "INSERT INTO movies (title, release_year, genres, directors, actors, description) VALUES (?,?,?,?,?,?)";

        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, movie.title);
            st.setInt(2, movie.year);
            st.setString(3, converter.join(movie.genres));
            st.setString(4, converter.join(movie.directors));
            st.setString(5, converter.join(movie.actors));
            st.setString(6, movie.description);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save movie: " + movie.title, e);
        }
    }

    @Override
    public void saveAll(List<Movie> movies) {
        for (Movie m : movies) save(m);
    }

    @Override
    public List<Movie> findByDirectorName(String name) {
        String sql = """
        SELECT m.id, m.title, m.release_year, m.genres, m.directors, m.actors, m.description
        FROM movies m
        JOIN movie_directors md ON m.id = md.movie_id
        JOIN directors d ON md.director_id = d.id
        WHERE LOWER(d.name) = LOWER(?)
        ORDER BY m.release_year DESC
        LIMIT 100
    """;

        List<Movie> list = new ArrayList<>();
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, name);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapper.mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find movies by director: " + name, e);
        }
        return list;
    }

    @Override
    public List<Movie> findByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = ids.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format(
                "SELECT id, title, release_year, genres, directors, actors, description FROM movies WHERE id IN (%s)",
                placeholders
        );

        List<Movie> movies = new ArrayList<>();

        try (PreparedStatement st = connection.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                st.setInt(i + 1, ids.get(i));
            }

            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    movies.add(mapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find movies by IDs: " + ids, e);
        }
        return movies;
    }

}
