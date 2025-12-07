package db;

import model.Movie;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PostgresMovieDao implements MovieDao {

    private final Connection connection;

    public PostgresMovieDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Movie findById(int id) throws SQLException {
        String sql = "SELECT id, title, release_year, genres, directors, actors, description FROM movies WHERE id = ?";
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
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
                while (rs.next()) list.add(mapRow(rs));
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
            st.setString(3, String.join(",", movie.genres));
            st.setString(4, String.join(",", movie.directors));
            st.setString(5, String.join(",", movie.actors));
            st.setString(6, movie.description);
            st.executeUpdate();
        }
    }

    @Override
    public void saveAll(List<Movie> movies) throws SQLException {
        for (Movie m : movies) save(m);
    }

    private Movie mapRow(ResultSet rs) throws SQLException {
        return new Movie(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getInt("release_year"),
                split(rs.getString("genres")),
                split(rs.getString("directors")),
                split(rs.getString("actors")),
                rs.getString("description")
        );
    }

    private List<String> split(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.asList(s.split(","));
    }
}
