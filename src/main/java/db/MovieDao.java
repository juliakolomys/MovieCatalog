package db;

import model.Movie;
import java.util.List;

public interface MovieDao {

    Movie findById(int id);

    List<Movie> findAll(int limit);

    void save(Movie movie);

    void saveAll(List<Movie> movies);

    List<Movie> findByDirectorName(String name);

    List<Movie> findByIds(List<Integer> ids);
}


