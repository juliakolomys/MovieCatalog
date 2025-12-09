import com.sun.net.httpserver.HttpServer;
import db.MovieDao;
import db.PostgresMovieDao;
import handlers.StaticFileHandler;
import handlers.SuggestHandler;
import model.Movie;
import search.ElasticsearchService;
import search.EsCandidateGenerator;
import search.RecommendationEngine;
import handlers.SearchHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.concurrent.Executors;

public class WebServerMain {
    public static void main(String[] args) throws IOException {

        int PORT = 8080;
        Connection conn = null;
        ElasticsearchService es = null;
        HttpServer server = null;

        try {
            conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/movieCatalog",
                    "app_user",
                    "12345678"
            );

            es = new ElasticsearchService("localhost", 9200, "julia", "meAdmin34");

            MovieDao movieDao = new PostgresMovieDao(conn);
            EsCandidateGenerator gen = new EsCandidateGenerator(es);
            RecommendationEngine engine = new RecommendationEngine(movieDao, gen);


            List<Movie> allMovies = movieDao.findAll(100000);
            System.out.println("Starting indexing " + allMovies.size() + " movies...");
            for (Movie movie : allMovies) es.indexMovie(movie);
            System.out.println("Indexing complete.");

            server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
            server.createContext("/api/search", new SearchHandler(engine));
            server.createContext("/", new StaticFileHandler("frontend"));
            server.createContext("/api/suggest", new SuggestHandler(movieDao));


            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("Web Server started on port " + PORT);
            System.out.println("Open: http://localhost:" + PORT + "/index.html");

        } catch (Exception e) {
            e.printStackTrace();
            if (server != null) server.stop(0);
        }

        HttpServer finalServer = server;
        ElasticsearchService finalEs = es;
        Connection finalConn = conn;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            if (finalServer != null) finalServer.stop(0);
            try { if (finalEs != null) finalEs.close(); } catch (Exception ignored) {}
            try { if (finalConn != null) finalConn.close(); } catch (Exception ignored) {}
        }));
    }
}