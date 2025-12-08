package handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.ScoredMovie;
import search.RecommendationEngine;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

    public class SearchHandler implements HttpHandler {

        private final RecommendationEngine engine;
        private final ObjectMapper mapper = new ObjectMapper();

        public SearchHandler(RecommendationEngine engine) {
            this.engine = engine;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            if (!exchange.getRequestMethod().equals("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            URI uri = exchange.getRequestURI();
            String query = extractQueryParam(uri, "q");

            if (query == null || query.isBlank()) {
                sendResponse(exchange, 400, "Query parameter 'q' is missing.");
                return;
            }

            try {
                List<ScoredMovie> results;

                Optional<Integer> initialMovieId = engine.getElasticsearchService().findIdByTitle(query);

                if (initialMovieId.isPresent()) {
                    results = engine.recommend(String.valueOf(initialMovieId.get()), 10);
                } else {
                    results = List.of();
                }

                String jsonResponse = mapper.writeValueAsString(results);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                sendResponse(exchange, 200, jsonResponse);

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }

        private String extractQueryParam(URI uri, String paramName) {
            String query = uri.getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith(paramName + "=")) {
                        try {
                            return java.net.URLDecoder.decode(param.substring(paramName.length() + 1), "UTF-8");
                        } catch (Exception ignored) {}
                    }
                }
            }
            return null;
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            long length = response != null ? response.length() : 0;
            exchange.sendResponseHeaders(statusCode, length);
            if (response != null) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes("UTF-8"));
                }
            }
        }
    }