package search;

import services.ElasticsearchService;

import java.io.IOException;
import java.util.List;

public interface CandidateGenerator {
    List<ElasticsearchService.HitResult> generate(String query, int limit) throws IOException;
}