package search;

import java.io.IOException;
import java.util.List;

public interface CandidateGenerator {
    List<search.ElasticsearchService.HitResult> generate(String query, int limit) throws IOException;
}