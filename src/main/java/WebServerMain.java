import com.sun.net.httpserver.HttpServer;
import db.MovieDao;
import db.MovieRowMapper;
import db.PostgresMovieDao;
import handlers.DirectorHandler;
import handlers.StaticFileHandler;
import handlers.SuggestHandler;
import model.Movie;
import search.ElasticsearchService;
import search.EsCandidateGenerator;
import search.RecommendationEngine;
import handlers.SearchHandler;
import utils.StringListConverter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.concurrent.Executors;

public class WebServerMain {
    private static void initializeAndRun(int port, Connection conn, ElasticsearchService es) throws Exception {

        StringListConverter converter = new StringListConverter();
        MovieRowMapper movieRowMapper = new MovieRowMapper(converter);
        MovieDao movieDao = new PostgresMovieDao(conn, movieRowMapper, converter);


        EsCandidateGenerator gen = new EsCandidateGenerator(es);
        RecommendationEngine engine = new RecommendationEngine(movieDao, gen);

        List<Movie> allMovies = movieDao.findAll(100000);
        System.out.println("Starting indexing " + allMovies.size() + " movies...");
        for (Movie movie : allMovies) {
            es.indexMovie(movie);
        }
        System.out.println("Indexing complete.");


        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);


        server.createContext("/api/search", new SearchHandler(engine));
        server.createContext("/", new StaticFileHandler("frontend"));
        server.createContext("/api/suggest", new SuggestHandler(movieDao));
        server.createContext("/api/director", new DirectorHandler(movieDao));


        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("Web Server started on port " + port);
        System.out.println("Open: http://localhost:" + port + "/index.html");


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            server.stop(0);
            try { if (es != null) es.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
            System.out.println("Shutdown complete.");
        }));
    }


    public static void main(String[] args) {

        int PORT = 8080;
        String DB_URL = "jdbc:postgresql://localhost:5432/movieCatalog";
        String DB_USER = "app_user";
        String DB_PASSWORD = "12345678";

        String ES_HOST = "localhost";
        int ES_PORT = 9200;
        String ES_USER = "julia";
        String ES_PASSWORD = "meAdmin34";

        Connection conn = null;
        ElasticsearchService es = null;

        try {

            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            es = new ElasticsearchService(ES_HOST, ES_PORT, ES_USER, ES_PASSWORD);

            initializeAndRun(PORT, conn, es);

        } catch (Exception e) {
            System.err.println("Critical startup error:");
            e.printStackTrace();


            try { if (es != null) es.close(); } catch (IOException ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
            System.exit(1);
        }
    }
}