package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.SearchResponse;
import search.RecommendationEngine;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class SearchHandler implements HttpHandler {

    private final RecommendationEngine engine;
    private final ObjectMapper mapper = new ObjectMapper();

    public SearchHandler(RecommendationEngine engine) {
        this.engine = engine;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, SearchResponse.error("Метод не підтримується"));
            return;
        }

        URI uri = exchange.getRequestURI();
        String rawQuery = uri.getRawQuery();
        String q = extractQueryParam(rawQuery, "q");
        if (q == null || q.isBlank()) {
            sendJson(exchange, SearchResponse.error("Порожній запит"));
            return;
        }

        try {
            Integer movieId = engine.getCandidateGenerator()
                    .getElasticsearchService()
                    .findIdByTitle(q)
                    .orElse(null);

            if (movieId == null) {
                sendJson(exchange, SearchResponse.error("Фільм не знайдено"));
                return;
            }

            SearchResponse resp = engine.recommendWithMain(String.valueOf(movieId), 5);
            sendJson(exchange, resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            sendJson(exchange, SearchResponse.error("Внутрішня помилка сервера"));
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
