package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import db.MovieDao;
import model.Movie;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class SuggestHandler implements HttpHandler {

    private final MovieDao movieDao;
    private final ObjectMapper mapper = new ObjectMapper();

    public SuggestHandler(MovieDao movieDao) {
        this.movieDao = movieDao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String rawQuery = exchange.getRequestURI().getRawQuery();
        String q = extractQueryParam(rawQuery, "q");
        if (q == null || q.isBlank()) {
            sendJson(exchange, List.of());
            return;
        }

        try {
            List<Movie> all = movieDao.findAll(1000);
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

    private String extractQueryParam(String rawQuery, String key) {
        if (rawQuery == null || rawQuery.isBlank()) return null;
        for (String part : rawQuery.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private void sendJson(HttpExchange exchange, Object obj) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(obj);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
