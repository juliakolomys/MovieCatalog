package db;


import model.Movie;

import java.sql.SQLException;
import java.util.List;

public interface MovieDao {

    Movie findById(String id) throws SQLException;

    List<Movie> findAll(int limit) throws SQLException;

    void save(Movie movie) throws SQLException;

    void saveAll(List<Movie> movies) throws SQLException;
}
