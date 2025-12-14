package handlers;

import com.sun.net.httpserver.HttpExchange;
import db.MovieDao;
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
            sendJson(exchange, List.of());
            return;
        }

        try {
            List<model.Movie> movies = movieDao.findByDirectorName(name);
            sendJson(exchange, movies);
        } catch (SQLException e) {
            e.printStackTrace();
            sendJson(exchange, List.of());
        }
    }
}