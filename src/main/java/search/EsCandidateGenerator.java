package search;

import services.ElasticsearchService;

import java.util.List;

public class EsCandidateGenerator implements CandidateGenerator {

    private final ElasticsearchService es;

    public EsCandidateGenerator(ElasticsearchService es) {
        this.es = es;
    }

    public List<ElasticsearchService.HitResult> generate(String query, int size) {
            return es.search(query, size);
    }

    public ElasticsearchService getElasticsearchService() {
        return es;
    }
}