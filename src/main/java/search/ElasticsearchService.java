package search;

import model.Movie;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.*;

public class ElasticsearchService {

    public static class Hit {
        public final String id;
        public final float score;

        public Hit(String id, float score) {
            this.id = id;
            this.score = score;
        }
    }

    private final RestHighLevelClient client;
    private final String INDEX = "movies";

    public ElasticsearchService(String host, int port) {
        this.client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(host, port, "http"))
        );
    }

    public void indexMovie(Movie movie) throws IOException {
        Map<String, Object> json = new HashMap<>();
        json.put("title", movie.title);
        json.put("description", movie.description);
        json.put("year", movie.year);
        json.put("genres", movie.genres);

        IndexRequest req = new IndexRequest(INDEX)
                .id(movie.id)
                .source(json);

        client.index(req, RequestOptions.DEFAULT);
    }

    // пошук з BM25
    public List<Hit> search(String query, int size) throws IOException {
        SearchRequest req = new SearchRequest(INDEX);

        SearchSourceBuilder builder = new SearchSourceBuilder()
                .query(QueryBuilders.matchQuery("description", query))
                .size(size);

        req.source(builder);

        SearchResponse resp = client.search(req, RequestOptions.DEFAULT);

        List<Hit> out = new ArrayList<>();
        for (SearchHit h : resp.getHits().getHits()) {
            out.add(new Hit(h.getId(), h.getScore())); // ← BM25 score
        }
        return out;
    }

    public void close() throws IOException {
        client.close();
    }
}
