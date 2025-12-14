package handlers;

import com.sun.net.httpserver.HttpExchange;
import db.MovieDao;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;


public class SuggestHandler extends BaseHttpHandler {

    private final MovieDao movieDao;

    public SuggestHandler(MovieDao movieDao) {
        this.movieDao = movieDao;
    }


    @Override
    protected void handleGet(HttpExchange exchange) throws IOException {
        String rawQuery = exchange.getRequestURI().getRawQuery();

        String q = extractQueryParam(rawQuery, "q");

        if (q == null || q.isBlank()) {
            sendJson(exchange, List.of());
            return;
        }

        try {
            List<model.Movie> all = movieDao.findAll(1000);
            List<String> suggestions = all.stream()
                    .map(m -> m.title)
                    .filter(t -> t != null && t.toLowerCase().startsWith(q.toLowerCase()))
                    .limit(10)
                    .collect(Collectors.toList());

            sendJson(exchange, suggestions);

        } catch (SQLException e) {
            e.printStackTrace();
            sendJson(exchange, List.of());
        }
    }
}