package service;

import db.MovieDao;
import model.Movie;
import model.ScoredMovie;
import search.ElasticsearchService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RecommendationService {

    private final MovieDao movieDao;
    private final ElasticsearchService searchService;

    public RecommendationService(MovieDao movieDao, ElasticsearchService searchService) {
        this.movieDao = movieDao;
        this.searchService = searchService;
    }

    public List<ScoredMovie> recommendForMovie(String movieId, int topN) throws SQLException, IOException {
        Movie movie = movieDao.findById(movieId);
        if (movie == null) return List.of();

        List<String> similarIds = searchService.search(movie.description, topN + 1); // +1, бо сам фільм включається
        List<ScoredMovie> recommendations = new ArrayList<>();
        for (String id : similarIds) {
            if (!id.equals(movieId)) {
                Movie m = movieDao.findById(id);
                if (m != null) {
                    recommendations.add(new ScoredMovie(m, 1.0)); // В реальному випадку можна ставити BM25 score
                }
            }
        }
        return recommendations;
    }
}
