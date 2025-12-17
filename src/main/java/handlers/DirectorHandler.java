package handlers;

import com.sun.net.httpserver.HttpExchange;
import db.MovieDao;
import exceptions.InvalidInputException;
import model.Movie;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DirectorHandler extends BaseHttpHandler {

    private final MovieDao movieDao;

    public DirectorHandler(MovieDao movieDao) {
        this.movieDao = movieDao;
    }

    @Override
    protected void handleGet(HttpExchange exchange) throws IOException {

        String rawQuery = exchange.getRequestURI().getRawQuery();

        String name = extractQueryParam(rawQuery, "name");

        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Director name is required");
        }

        List<Movie> movies = movieDao.findByDirectorName(name);
        sendJson(exchange, movies);
    }
}