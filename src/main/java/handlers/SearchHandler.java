package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.ScoredMovie;
import search.RecommendationEngine;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SearchHandler implements HttpHandler {

    private final RecommendationEngine engine;
    private final ObjectMapper mapper = new ObjectMapper();

    public SearchHandler(RecommendationEngine engine) {
        this.engine = engine;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        URI uri = exchange.getRequestURI();
        String rawQuery = uri.getRawQuery();
        String q = extractQueryParam(rawQuery, "q");
        if (q == null || q.isBlank()) {
            sendJson(exchange, Collections.emptyList());
            return;
        }

        try {
            Integer movieId = engine.getCandidateGenerator()
                    .getElasticsearchService()
                    .findIdByTitle(q)
                    .orElse(null);

            if (movieId == null) {
                sendJson(exchange, Collections.emptyList());
                return;
            }

            // Повертаємо список: перший — main з score, далі recommendations
            List<ScoredMovie> moviesWithScores = engine.recommendWithMain(String.valueOf(movieId), 5);
            if (moviesWithScores == null || moviesWithScores.isEmpty()) {
                sendJson(exchange, Collections.emptyList());
                return;
            }

            ScoredMovie main = moviesWithScores.get(0);
            List<Map<String, Object>> recList = new ArrayList<>();
            for (int i = 1; i < moviesWithScores.size(); i++) {
                ScoredMovie s = moviesWithScores.get(i);
                Map<String, Object> m = new HashMap<>();
                m.put("movie", s.movie);
                m.put("score", s.score);
                recList.add(m);
            }

            Map<String, Object> responseObj = new HashMap<>();
            responseObj.put("movie", main.movie);
            responseObj.put("score", main.score);
            responseObj.put("recommendations", recList);

            sendJson(exchange, Collections.singletonList(responseObj));

        } catch (Exception ex) {
            ex.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
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
