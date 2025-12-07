import db.MovieDao;
import db.PostgresMovieDao;
import model.ScoredMovie;
import search.ElasticsearchService;
import search.EsCandidateGenerator;
import service.RecommendationEngine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class AppMain {
    public static void main(String[] args) throws Exception {

        String USERNAME = "julia";
        String PASSWORD = "meAdmin34";
        int movieId = 2;

        // Використовуємо try-with-resources для автоматичного закриття conn та es
        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/movieCatalog",
                "app_user",
                "12345678"
        );
             ElasticsearchService es = new ElasticsearchService("localhost", 9200, USERNAME, PASSWORD))
        {

            MovieDao movieDao = new PostgresMovieDao(conn);
            EsCandidateGenerator gen = new EsCandidateGenerator(es);
            RecommendationEngine engine = new RecommendationEngine(movieDao, gen);

            List<ScoredMovie> recs = engine.recommend(String.valueOf(movieId), 10);

            System.out.println("Recommendations:");
            for (ScoredMovie r : recs) {
                System.out.printf("- %s (score %.3f)%n", r.movie.title, r.score);
            }

        } catch (Exception e) {
            System.err.println("An error occurred during application execution:");
            e.printStackTrace();
        }

    }
}