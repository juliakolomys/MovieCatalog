package db;

import model.Movie;
import utils.StringListConverter;

import java.sql.ResultSet;
import java.sql.SQLException;


public class MovieRowMapper {

    private final StringListConverter converter;

    public MovieRowMapper(StringListConverter converter) {
        this.converter = converter;
    }

    public Movie mapRow(ResultSet rs) throws SQLException {
        return new Movie(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getInt("release_year"),
                converter.split(rs.getString("genres")),
                converter.split(rs.getString("directors")),
                converter.split(rs.getString("actors")),
                rs.getString("description")
        );
    }
}