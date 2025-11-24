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
    public List<ScoredMovie> recommendForMovie(String movieId, int topN) {
        List<ScoredMovie> recommendations = new ArrayList<>();
        Movie movie;

        try {
            movie = movieDao.findById(movieId);
            if (movie == null) {
                System.out.println("Movie with id " + movieId + " wasnt found");
                return recommendations;
            }
        } catch (SQLException e) {
            System.out.println("Error accessing date base: " + e.getMessage());
            return recommendations;
        }

        List<ElasticsearchService.Hit> hits = new ArrayList<>();
        try {
            hits = searchService.search(movie.description, topN + 1);
        } catch (IOException e) {
            System.out.println("Error search in Elasticsearch: " + e.getMessage());
        }

        for (ElasticsearchService.Hit hit : hits) {
            if (hit.id.equals(movieId)) continue;

            Movie m;
            try {
                m = movieDao.findById(hit.id);
                if (m == null) continue;
            } catch (SQLException e) {
                System.out.println("Error retrieving a movie from date base: " + e.getMessage());
                continue;
            }

            double finalScore = hit.score;

            if (movie.genres != null && movie.genres.equals(m.genres)) finalScore += 0.2;
            if (movie.year == m.year) finalScore += 0.1;

            recommendations.add(new ScoredMovie(m, finalScore));
        }

        return recommendations;
    }
}
