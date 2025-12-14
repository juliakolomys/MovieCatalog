package db;

import model.Movie;
import utils.StringListConverter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
    public Movie findById(int id) throws SQLException {
        String sql = "SELECT id, title, release_year, genres, directors, actors, description FROM movies WHERE id = ?";
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) return null;
                return mapper.mapRow(rs);
            }
        }
    }

    @Override
    public List<Movie> findAll(int limit) throws SQLException {
        String sql = "SELECT id, title, release_year, genres, directors, actors, description FROM movies LIMIT ?";
        List<Movie> list = new ArrayList<>();
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, limit);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapper.mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public void save(Movie movie) throws SQLException {
        String sql = "INSERT INTO movies (title, release_year, genres, directors, actors, description) VALUES (?,?,?,?,?,?)";

        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, movie.title);
            st.setInt(2, movie.year);
            st.setString(3, converter.join(movie.genres));
            st.setString(4, converter.join(movie.directors));
            st.setString(5, converter.join(movie.actors));
            st.setString(6, movie.description);
            st.executeUpdate();
        }
    }

    @Override
    public void saveAll(List<Movie> movies) throws SQLException {
        for (Movie m : movies) save(m);
    }

    @Override
    public List<Movie> findByDirectorName(String name) throws SQLException {
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
        }
        return list;
    }
}