package handlers;

import com.sun.net.httpserver.HttpExchange;
import model.SearchResponse;
import search.RecommendationEngine;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

public class SearchHandler extends BaseHttpHandler {

    private final RecommendationEngine engine;

    public SearchHandler(RecommendationEngine engine) {
        this.engine = engine;
    }

    @Override
    protected void handleGet(HttpExchange exchange) throws IOException {

        URI uri = exchange.getRequestURI();
        String rawQuery = uri.getRawQuery();

        String q = extractQueryParam(rawQuery, "q");

        if (q == null || q.isBlank()) {
            sendJson(exchange, SearchResponse.error("Empty query"));
            return;
        }

        try {
            Optional<Integer> movieIdOptional = engine.getCandidateGenerator()
                    .getElasticsearchService()
                    .findIdByTitle(q);

            if (movieIdOptional.isEmpty()) {
                sendJson(exchange, SearchResponse.error("Movie not found"));
                return;
            }

            Integer movieId = movieIdOptional.get();

            SearchResponse resp = engine.recommendWithMain(String.valueOf(movieId), 5);
            sendJson(exchange, resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            sendJson(exchange, SearchResponse.error("Server inner error"));
        }
    }
}