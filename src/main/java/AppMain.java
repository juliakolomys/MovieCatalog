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

        Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/moviedb",
                "postgres",
                "password"
        );

        MovieDao movieDao = new PostgresMovieDao(conn);

        ElasticsearchService es = new ElasticsearchService("localhost", 9200);
        EsCandidateGenerator gen = new EsCandidateGenerator(es);

        RecommendationEngine engine = new RecommendationEngine(movieDao, gen);

        String movieId = "tt0111161"; // Shawshank

        List<ScoredMovie> recs = engine.recommend(movieId, 10);

        System.out.println("Recommendations:");
        for (ScoredMovie r : recs) {
            System.out.printf("- %s (score %.3f)%n", r.movie.title, r.score);
        }

        es.close();
        conn.close();
    }
}
