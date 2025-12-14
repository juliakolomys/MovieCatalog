import com.sun.net.httpserver.HttpServer;
import db.MovieDao;
import db.PostgresMovieDao;
import db.MovieRowMapper;
import utils.StringListConverter;
import handlers.DirectorHandler;
import handlers.StaticFileHandler;
import handlers.SuggestHandler;
import handlers.SearchHandler;
import model.Movie;
import search.ElasticsearchService;
import search.EsCandidateGenerator;
import search.RecommendationEngine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.concurrent.Executors;

public class WebServerMain {

    private static final int PORT = 8080;


    private static final String DB_URL = "jdbc:postgresql://localhost:5432/movieCatalog";
    private static final String DB_USER = "app_user";
    private static final String DB_PASSWORD = "12345678";

    private static final String ES_HOST = "localhost";
    private static final int ES_PORT = 9200;
    private static final String ES_USER = "julia";
    private static final String ES_PASSWORD = "meAdmin34";

    public static void main(String[] args) {
        Connection conn = null;
        ElasticsearchService es = null;
        HttpServer server = null;

        try {

            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            es = new ElasticsearchService(ES_HOST, ES_PORT, ES_USER, ES_PASSWORD);


            StringListConverter converter = new StringListConverter();
            MovieRowMapper movieRowMapper = new MovieRowMapper(converter);


            MovieDao movieDao = new PostgresMovieDao(conn, movieRowMapper, converter);

            EsCandidateGenerator gen = new EsCandidateGenerator(es);
            RecommendationEngine engine = new RecommendationEngine(movieDao, gen);


            System.out.println("Starting indexing...");
            indexAllMovies(movieDao, es);
            System.out.println("Indexing complete.");


            server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);


            server.createContext("/api/search", new SearchHandler(engine));


            server.createContext("/api/suggest", new SuggestHandler(es));

            server.createContext("/api/director", new DirectorHandler(movieDao));
            server.createContext("/", new StaticFileHandler("frontend"));


            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("Web Server started on port " + PORT);
            System.out.println("Open: http://localhost:" + PORT + "/index.html");

        } catch (Exception e) {
            System.err.println("Critical startup error:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            HttpServer finalServer = server;
            ElasticsearchService finalEs = es;
            Connection finalConn = conn;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down...");
                if (finalServer != null) finalServer.stop(0);
                try { if (finalEs != null) finalEs.close(); } catch (Exception ignored) {}
                try { if (finalConn != null) finalConn.close(); } catch (Exception ignored) {}
                System.out.println("Shutdown complete.");
            }));
        }
    }

    private static void indexAllMovies(MovieDao movieDao, ElasticsearchService es) throws Exception {
        List<Movie> allMovies = movieDao.findAll(100000);
        int indexedCount = 0;
        for (Movie movie : allMovies) {
            es.indexMovie(movie);
            indexedCount++;
            if (indexedCount % 100 == 0) {
                System.out.println("Indexed " + indexedCount + " of " + allMovies.size() + " movies.");
            }
        }
        System.out.println("Total indexed: " + indexedCount);
    }
}