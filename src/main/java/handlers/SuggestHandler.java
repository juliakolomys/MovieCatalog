package handlers;

import com.sun.net.httpserver.HttpExchange;
import search.ElasticsearchService;
import model.SearchResponse;
import java.io.IOException;
import java.util.List;

public class SuggestHandler extends BaseHttpHandler {

    private final ElasticsearchService esService;


    public SuggestHandler(ElasticsearchService esService) {
        this.esService = esService;
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
            List<String> suggestions = esService.searchTitlesByPrefix(q, 10);

            sendJson(exchange, suggestions);

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, SearchResponse.error("Server inner error"));
        }
    }
}