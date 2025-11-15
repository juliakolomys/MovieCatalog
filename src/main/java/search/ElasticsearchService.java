package search;


import model.Movie;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ElasticsearchService {

    private final RestHighLevelClient client;
    private final Vectorizer vectorizer = new Vectorizer();
    private final String INDEX = "movies";

    public ElasticsearchService(String hostname, int port) {
        this.client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(hostname, port, "http"))
        );
    }

    // Індексування фільму в ES
    public void indexMovie(Movie movie) throws IOException {
        String content = movie.title + " " + movie.description;
        IndexRequest request = new IndexRequest(INDEX)
                .id(movie.id)
                .source("{\"title\":\"" + movie.title + "\", \"description\":\"" + movie.description + "\"}", XContentType.JSON);
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
    }

    // Пошук за текстом
    public List<String> search(String query, int size) throws IOException {
        SearchRequest searchRequest = new SearchRequest(INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.query(QueryBuilders
                        .matchQuery("description", query))
                .size(size);

        searchRequest.source(sourceBuilder);
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        List<String> results = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            results.add(hit.getId());
        }
        return results;
    }

    public void close() throws IOException {
        client.close();
    }
}
