package services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.PrefixQuery;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import exceptions.SearchServiceException;
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
        public final String title;
        public HitResult(int id, float score, String title) {
            this.id = id;
            this.score = score;
            this.title = title;
        }
    }

    private final ElasticsearchClient client;
    private final RestClient restClient;
    private static final String INDEX = "movies";

    public ElasticsearchService(String host, int port, String username, String password) {
        try {
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
            System.out.println("Elasticsearch client connected successfully");

        } catch (Exception e) {
            throw new SearchServiceException("Failed to initialize Elasticsearch client connection.", e);
        }
    }

    public void indexMovie(Movie movie) {
        IndexRequest<Movie> request = IndexRequest.of(i -> i
                .index(INDEX)
                .id(String.valueOf(movie.id))
                .document(movie)
        );
        try {
            client.index(request);
        } catch (IOException e) {
            throw new SearchServiceException("Failed to index movie: " + movie.title + " (ID=" + movie.id + ")", e);
        }
    }


    public List<HitResult> search(String query, int size) {
        try {
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

            List<HitResult> results = new ArrayList<>();

            for (Hit<Movie> hit : resp.hits().hits()) {
                Double scoreObject = hit.score();
                float score = (scoreObject != null) ? scoreObject.floatValue() : 0.0f;
                int id = Integer.parseInt(hit.id());
                String title = (hit.source() != null) ? hit.source().title : "Unknown Title";
                results.add(new HitResult(id, score, title));
            }

            return results;
        } catch (IOException e) {
            throw new SearchServiceException("Elasticsearch search failed for query: " + query, e);
        }
    }

    public List<String> searchTitlesByPrefix(String prefix, int size) {
        try {
            PrefixQuery prefixQuery = PrefixQuery.of(p -> p
                    .field("title.keyword")
                    .value(prefix.toLowerCase())
            );

            SearchResponse<Movie> resp = client.search(s -> s
                            .index(INDEX)
                            .query(q -> q.prefix(prefixQuery))
                            .source(src -> src.filter(f -> f.includes("title")))
                            .size(size),
                    Movie.class
            );

            List<String> titles = new ArrayList<>();
            for (Hit<Movie> hit : resp.hits().hits()) {
                if (hit.source() != null && hit.source().title != null) {
                    titles.add(hit.source().title);
                }
            }
            return titles;
        } catch (IOException e) {
            throw new SearchServiceException("Elasticsearch prefix search failed for: " + prefix, e);
        }
    }

    public Optional<Integer> findIdByTitle(String titleQuery) {
        try {
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
        } catch (IOException e) {
            throw new SearchServiceException("Elasticsearch findIdByTitle failed for: " + titleQuery, e);
        }
    }

    @Override
    public void close() {
        if (restClient != null) {
            try {
                restClient.close();
                System.out.println("Elasticsearch RestClient closed successfully");
            } catch (IOException e) {
                System.err.println("Error closing Elasticsearch RestClient: " + e.getMessage());
            }
        }
    }
}