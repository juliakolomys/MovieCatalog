import db.MovieDao;
import db.PostgresMovieDao;
import model.Movie;
import model.ScoredMovie;
import search.ElasticsearchService;
import service.RecommendationService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class AppMain {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/moviedb";
        String user = "postgres";
        String password = "password";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            MovieDao movieDao = new PostgresMovieDao(conn);

            ElasticsearchService esService = new ElasticsearchService("localhost", 9200);


            List<Movie> allMovies = movieDao.findAll(100);
            for (Movie movie : allMovies) {
                esService.indexMovie(movie);
            }

            RecommendationService recService = new RecommendationService(movieDao, esService);


            String movieId = "1";
            List<ScoredMovie> recommendations = recService.recommendForMovie(movieId, 5);

            System.out.println("Рек для фільму з ID " + movieId + ":");
            for (ScoredMovie sm : recommendations) {
                System.out.println(sm);
            }

            esService.close();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}