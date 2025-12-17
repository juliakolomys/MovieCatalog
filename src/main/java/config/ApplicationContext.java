package config;

import com.sun.net.httpserver.HttpServer;
import db.MovieDao;
import utils.MovieRowMapper;
import db.PostgresMovieDao;
import handlers.*;
import model.Movie;
import services.ElasticsearchService;
import search.EsCandidateGenerator;
import services.RecommendationEngine;
import utils.StringListConverter;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Executors;

public class ApplicationContext implements AutoCloseable {

    private final DatabaseConnector dbConnector;
    private final ElasticsearchService esService;
    private final HttpServer httpServer;

    private final int serverPort;

    public ApplicationContext() throws Exception {
        ConfigurationLoader config = new ConfigurationLoader();

        String dbUrl = config.getString("db.url");
        String dbUser = config.getString("db.user");
        String dbPassword = config.getString("db.password");

        String esHost = config.getString("es.host");
        int esPort = config.getInt("es.port");
        String esUser = config.getString("es.user");
        String esPassword = config.getString("es.password");

        this.serverPort = config.getInt("server.port");
        int threadPoolSize = config.getInt("server.threads");

        this.dbConnector = new DatabaseConnector(dbUrl, dbUser, dbPassword);
        Connection conn = this.dbConnector.getConnection();
        this.esService = new ElasticsearchService(esHost, esPort, esUser, esPassword);

        StringListConverter converter = new StringListConverter();
        MovieRowMapper movieRowMapper = new MovieRowMapper(converter);
        MovieDao movieDao = new PostgresMovieDao(conn, movieRowMapper, converter);

        EsCandidateGenerator gen = new EsCandidateGenerator(esService);
        RecommendationEngine engine = new RecommendationEngine(movieDao, gen);

        indexAllMovies(movieDao, esService);

        this.httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", serverPort), 0);
        this.httpServer.setExecutor(Executors.newFixedThreadPool(threadPoolSize));

        setupHandlers(httpServer, engine, esService, movieDao);
    }

    private void setupHandlers(HttpServer server, RecommendationEngine engine, ElasticsearchService es, MovieDao movieDao) {
        server.createContext("/api/search", new SearchHandler(engine));
        server.createContext("/api/suggest", new SuggestHandler(es));
        server.createContext("/api/director", new DirectorHandler(movieDao));
        server.createContext("/", new StaticFileHandler("frontend"));
    }

    private void indexAllMovies(MovieDao movieDao, ElasticsearchService es) throws Exception {
        System.out.println("Starting indexing...");
        List<Movie> allMovies = movieDao.findAll(100);
        int indexedCount = 0;
        for (Movie movie : allMovies) {
            es.indexMovie(movie);
            indexedCount++;
            if (indexedCount % 10 == 0) {
                System.out.println("Indexed " + indexedCount + " of " + allMovies.size() + " movies.");
            }
        }
        System.out.println("Indexing complete. Total indexed: " + indexedCount);
    }

    public void startServer() {
        httpServer.start();
        System.out.println("Web Server started on port " + serverPort);
        System.out.println("Open: http://localhost:" + serverPort + "/index.html");
    }

    @Override
    public void close() {
        System.out.println("Shutting down application context...");
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (esService != null) {
            esService.close();
        }

        if (dbConnector != null) {
            dbConnector.close();
        }
        System.out.println("Shutdown complete.");
    }
}