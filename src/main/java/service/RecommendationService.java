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

    /**
     * Генерує рекомендації для заданого фільму за описом та додатковими фічами (жанр, рік)
     *
     * @param movieId - id фільму, для якого робимо рекомендації
     * @param topN    - кількість рекомендацій
     * @return список рекомендованих фільмів зі скором
     */
    public List<ScoredMovie> recommendForMovie(String movieId, int topN) throws SQLException, IOException {
        Movie movie = movieDao.findById(movieId);
        if (movie == null) return List.of();

        // Пошук у Elasticsearch за описом та отримання релевантних хітів з їхніми оцінками BM25
        List<ElasticsearchService.Hit> hits = searchService.search(movie.description, topN + 1); // +1 бо сам фільм теж може потрапити

        List<ScoredMovie> recommendations = new ArrayList<>();
        for (ElasticsearchService.Hit hit : hits) {
            // Виключаємо сам фільм
            if (!hit.id.equals(movieId)) {
                Movie m = movieDao.findById(hit.id);
                if (m != null) {
                    // Можна підкоригувати score
                    double finalScore = hit.score;
                    if (movie.genres != null && movie.genres.equals(m.genres)) finalScore += 0.2; // невеликий бонус за співпадіння жанру
                    if (movie.year == m.year) finalScore += 0.1;
                    // бонус за рік

                    recommendations.add(new ScoredMovie(m, finalScore));
                }
            }
        }
        return recommendations;
    }
}
