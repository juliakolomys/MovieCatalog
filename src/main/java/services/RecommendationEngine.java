package services;

import db.MovieDao;
import exceptions.DataAccessException;
import exceptions.InvalidInputException;
import exceptions.SearchServiceException;
import model.Movie;
import model.ScoredMovie;
import model.SearchResponse;
import search.EsCandidateGenerator;

import java.util.*;
import java.util.stream.Collectors;

public class RecommendationEngine {

    private final MovieDao movieDao;
    private final EsCandidateGenerator candidateGenerator;

    public RecommendationEngine(MovieDao movieDao, EsCandidateGenerator candidateGenerator) {
        this.movieDao = movieDao;
        this.candidateGenerator = candidateGenerator;
    }

    public SearchResponse recommendWithMain(String stringId, int limit) {
        int initialMovieId;

        try {
            initialMovieId = Integer.parseInt(stringId);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Movie ID must be a valid number: " + stringId);
        }

        Movie mainMovie = movieDao.findById(initialMovieId);

        if (mainMovie == null) {
            return SearchResponse.error("Movie not found");
        }

        String primaryQuery = buildPrimaryQuery(mainMovie);
        int candidatePool = Math.max(10, limit * 4);
        List<ElasticsearchService.HitResult> hits;

        try {
            hits = candidateGenerator.generate(primaryQuery, candidatePool);
        } catch (SearchServiceException e) {
            System.err.println("ES Search Error: " + e.getMessage());
            throw e;
        }

        double mainScore = extractMainScore(hits, initialMovieId, mainMovie);

        Set<Integer> candidateIdsToLoad = new LinkedHashSet<>();

        if (hits != null) {
            for (ElasticsearchService.HitResult hit : hits) {
                if (hit.id != initialMovieId) { //окрім головного
                    candidateIdsToLoad.add(hit.id);
                }
            }
        }


        List<Movie> recommendedMovies = Collections.emptyList();
        try {
            if (!candidateIdsToLoad.isEmpty()) {
                recommendedMovies = movieDao.findByIds(new ArrayList<>(candidateIdsToLoad));
            }
        } catch (DataAccessException e) {
            System.err.println("WARNING: Failed to load recommended movies from DB: " + e.getMessage());
            recommendedMovies = Collections.emptyList();
        }

        Map<Integer, Movie> movieMap = recommendedMovies.stream()
                .collect(Collectors.toMap(m -> m.id, m -> m));

        List<ScoredMovie> recommendations = new ArrayList<>();

        if (hits != null) {
            for (ElasticsearchService.HitResult hit : hits) {
                if (hit.id == initialMovieId) continue;

                Movie m = movieMap.get(hit.id);

                if (m != null) {
                    recommendations.add(new ScoredMovie(m, hit.score));
                }

                if (recommendations.size() >= limit) break;
            }
        }

        if (recommendations.size() < limit) {
            handleFallback(mainMovie, candidatePool, limit, recommendations, movieMap);
        }

        return SearchResponse.ok(mainMovie, mainScore, recommendations);
    }


    private String buildPrimaryQuery(Movie mainMovie) {
        StringBuilder sb = new StringBuilder();
        if (mainMovie.title != null && !mainMovie.title.isBlank()) {
            sb.append(mainMovie.title).append(" ");
        }
        if (mainMovie.genres != null && !mainMovie.genres.isEmpty()) {
            sb.append(String.join(" ", mainMovie.genres)).append(" ");
        }
        String primaryQuery = sb.toString().trim();
        if (primaryQuery.isBlank()) {
            primaryQuery = (mainMovie.description == null) ? "" : mainMovie.description;
        }
        return primaryQuery;
    }

    private double extractMainScore(List<ElasticsearchService.HitResult> hits, int initialMovieId, Movie mainMovie) {
        double mainScore = 0.0;

        if (hits != null) {
            for (ElasticsearchService.HitResult hit : hits) {
                if (hit.id == initialMovieId) {
                    mainScore = hit.score;
                    break;
                }
            }
        }

        if (mainScore == 0.0 && mainMovie.title != null && !mainMovie.title.isBlank()) {
            try {
                List<ElasticsearchService.HitResult> titleHits = candidateGenerator.generate(mainMovie.title, 5);
                if (titleHits != null && !titleHits.isEmpty()) {
                    Optional<ElasticsearchService.HitResult> mainHit = titleHits.stream()
                            .filter(th -> th.id == initialMovieId)
                            .findFirst();

                    if (mainHit.isPresent()) {
                        mainScore = mainHit.get().score;
                    } else if (mainScore == 0.0) {
                        mainScore = titleHits.get(0).score;
                    }
                }
            } catch (SearchServiceException ex) {
                System.err.println("DEBUG: title-search fallback failed: " + ex.getMessage());
            }
        }
        return mainScore;
    }

    private void handleFallback(Movie mainMovie, int candidatePool, int limit, List<ScoredMovie> recommendations, Map<Integer, Movie> existingMovies) {
        String fallbackQuery = (mainMovie.title == null ? "" : mainMovie.title + " ") +
                (mainMovie.genres == null ? "" : String.join(" ", mainMovie.genres));

        try {
            List<ElasticsearchService.HitResult> more = candidateGenerator.generate(fallbackQuery, candidatePool);

            if (more != null) {

                Set<Integer> newIdsToLoad = more.stream()
                        .map(hit -> hit.id)
                        .filter(id -> id != mainMovie.id && !existingMovies.containsKey(id))
                        .collect(Collectors.toSet());

                if (!newIdsToLoad.isEmpty()) {
                    try {
                        List<Movie> newMovies = movieDao.findByIds(new ArrayList<>(newIdsToLoad));
                        newMovies.forEach(m -> existingMovies.put(m.id, m));
                    } catch (DataAccessException e) {
                        System.err.println("WARNING: Failed to load fallback movies from DB: " + e.getMessage());
                    }
                }

                for (ElasticsearchService.HitResult hit : more) {
                    if (recommendations.size() >= limit) break;
                    if (hit.id == mainMovie.id) continue;

                    Movie m = existingMovies.get(hit.id);
                    if (m != null) {
                        boolean already = recommendations.stream().anyMatch(sm -> sm.movie.id == hit.id);
                        if (!already) {
                            recommendations.add(new ScoredMovie(m, hit.score));
                        }
                    }
                }
            }
        } catch (SearchServiceException ex) {
            System.err.println("DEBUG: fallback generate failed: " + ex.getMessage());
        }
    }

    public MovieDao getMovieDao() {
        return movieDao;
    }

    public EsCandidateGenerator getCandidateGenerator() {
        return candidateGenerator;
    }
}