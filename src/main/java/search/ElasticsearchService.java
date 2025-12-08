package search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import model.Movie;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ElasticsearchService implements AutoCloseable {

    public static class HitResult {
        public final int id;
        public final float score;

        public HitResult(int id, float score) {
            this.id = id;
            this.score = score;
        }
    }

    private final ElasticsearchClient client;
    private final RestClient restClient;
    private final String INDEX = "movies";

    public ElasticsearchService(String host, int port, String username, String password) {

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        this.restClient = RestClient.builder(new HttpHost(host, port, "http"))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        this.client = new ElasticsearchClient(transport);
    }

    public void indexMovie(Movie movie) throws IOException {
        IndexRequest<Movie> request = IndexRequest.of(i -> i
                .index(INDEX)
                .id(String.valueOf(movie.id))
                .document(movie)
        );
        client.index(request);
        System.out.println("Indexed movie: " + movie.title + " (ID=" + movie.id + ")");
    }


    public List<HitResult> search(String query, int size) throws IOException {

        SearchResponse<Movie> resp = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .multiMatch(m -> m
                                        .fields("title^3", "genres^2", "description")
                                        .query(query)
                                        .fuzziness("AUTO")
                                        .operator(Operator.Or)
                                )
                        )
                        .size(size),
                Movie.class
        );

        System.out.println("DEBUG: ES search query for '" + query + "' took " + resp.took() + " ms.");

        List<HitResult> results = new ArrayList<>();

        for (Hit<Movie> hit : resp.hits().hits()) {
            Double scoreObject = hit.score();
            float score = (scoreObject != null) ? scoreObject.floatValue() : 0.0f;
            int id = Integer.parseInt(hit.id());
            results.add(new HitResult(id, score));
        }

        return results;
    }

    public Optional<Integer> findIdByTitle(String titleQuery) throws IOException {

        SearchResponse<Movie> resp = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .matchPhrase(mp -> mp
                                        .field("title")
                                        .query(titleQuery)
                                )
                        )
                        .size(1),
                Movie.class
        );

        if (!resp.hits().hits().isEmpty()) {
            Movie movie = resp.hits().hits().get(0).source();
            if (movie != null) {
                return Optional.of(movie.id);
            }
        }
        return Optional.empty();
    }

    @Override
    public void close () throws IOException {
        if (restClient != null) {
            restClient.close();
        }
    }
}
