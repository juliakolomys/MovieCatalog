package search;

import db.MovieDao;
import model.Movie;
import model.ScoredMovie;
import search.ElasticsearchService.HitResult;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecommendationEngine {

    private final MovieDao movieDao;
    private final EsCandidateGenerator candidateGenerator;


    public interface CandidateGenerator {
        List<HitResult> generate(String query, int limit) throws IOException;
    }

    public RecommendationEngine(MovieDao movieDao, EsCandidateGenerator candidateGenerator) {
        this.movieDao = movieDao;
        this.candidateGenerator = candidateGenerator;
    }

    public List<ScoredMovie> recommend(String stringId, int limit) throws IOException, SQLException {

        int initialMovieId = Integer.parseInt(stringId);

        Optional<Movie> initialMovie = Optional.ofNullable(movieDao.findById(initialMovieId));

        if (initialMovie.isEmpty()) {
            return List.of();
        }

        String query = String.join(" ", initialMovie.get().genres) + " " + initialMovie.get().description;

        List<HitResult> hits = candidateGenerator.generate(query, limit * 2);

        List<ScoredMovie> recommendations = new ArrayList<>();

        for (HitResult hit : hits) {


            int movieId = hit.id;
            String hitIdString = String.valueOf(movieId);

            if (hitIdString.equals(stringId)) {
                continue;
            }

            Optional<Movie> movie = Optional.ofNullable(movieDao.findById(movieId));

            if (movie.isPresent()) {
                recommendations.add(new ScoredMovie(movie.get(), hit.score));
            }

            if (recommendations.size() >= limit) {
                break;
            }
        }

        return recommendations;
    }

    public EsCandidateGenerator getCandidateGenerator() {
        return candidateGenerator;
    }

    public ElasticsearchService getElasticsearchService() {
        return candidateGenerator.getElasticsearchService();
    }
}