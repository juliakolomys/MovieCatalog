package search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper; // Для серіалізації/десеріалізації
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import model.Movie; // Припускаємо, що клас Movie існує
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
                restClient, new JacksonJsonpMapper()); // JacksonJsonpMapper потрібен для JSON

        this.client = new ElasticsearchClient(transport);
    }

    public void indexMovie(Movie movie) throws IOException {
        IndexResponse response = client.index(i -> i
                .index(INDEX)
                .id(String.valueOf(movie.id))
                .document(movie) // Movie повинен бути POJO
        );
    }

    public List<HitResult> search(String query, int size) throws IOException {

        SearchResponse<Movie> resp = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .match(t -> t
                                        .field("description")
                                        .query(query)
                                )
                        )
                        .size(size),
                Movie.class
        );

        List<HitResult> out = new ArrayList<>();

        for (Hit<Movie> hit : resp.hits().hits()) {

            Double scoreObject = hit.score();

            float score = (scoreObject != null) ? scoreObject.floatValue() : 0.0f;

            out.add(new HitResult(
                    Integer.parseInt(hit.id()),
                    score
            ));
        }
        return out;
    }

    @Override
    public void close () throws IOException {
        if (restClient != null) {
            restClient.close();
        }
    }
}