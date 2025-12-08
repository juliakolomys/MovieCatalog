package search;

import java.io.IOException;
import java.util.List;
import search.ElasticsearchService.HitResult;

public class EsCandidateGenerator implements CandidateGenerator {

    private final ElasticsearchService es;

    public EsCandidateGenerator(ElasticsearchService es) {
        this.es = es;
    }



    @Override
    public List<HitResult> generate(String query, int limit) throws IOException {
        return es.search(query, limit);
    }

    public ElasticsearchService getElasticsearchService() {
        return es;
    }
}