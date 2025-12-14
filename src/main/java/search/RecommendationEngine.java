package search;

import db.MovieDao;
import model.Movie;
import model.ScoredMovie;
import model.SearchResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RecommendationEngine {

    private final MovieDao movieDao;
    private final EsCandidateGenerator candidateGenerator;

    public RecommendationEngine(MovieDao movieDao, EsCandidateGenerator candidateGenerator) {
        this.movieDao = movieDao;
        this.candidateGenerator = candidateGenerator;
    }

    public SearchResponse recommendWithMain(String stringId, int limit) throws IOException, SQLException {
        int initialMovieId = Integer.parseInt(stringId);
        Movie mainMovie = movieDao.findById(initialMovieId);
        if (mainMovie == null) {
            return SearchResponse.error("Movie not found");
        }

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

        int candidatePool = Math.max(10, limit * 4);
        List<ElasticsearchService.HitResult> hits = candidateGenerator.generate(primaryQuery, candidatePool);
        System.out.println("DEBUG: recommendWithMain primaryQuery='" + primaryQuery + "' hits=" + (hits == null ? 0 : hits.size()));

        List<ScoredMovie> recommendations = new ArrayList<>();
        double mainScore = 0.0;

        if (hits != null) {
            for (ElasticsearchService.HitResult hit : hits) {
                System.out.println("DEBUG: candidate id=" + hit.id + " score=" + hit.score);
                if (hit.id == initialMovieId) {
                    mainScore = hit.score;
                }
            }
        }


        if (mainScore == 0.0 && mainMovie.title != null && !mainMovie.title.isBlank()) {
            try {
                List<ElasticsearchService.HitResult> titleHits = candidateGenerator.generate(mainMovie.title, 5);
                System.out.println("DEBUG: title-search hits=" + (titleHits == null ? 0 : titleHits.size()) + " for title='" + mainMovie.title + "'");
                if (titleHits != null && !titleHits.isEmpty()) {
                    for (ElasticsearchService.HitResult th : titleHits) {
                        System.out.println("DEBUG: title candidate id=" + th.id + " score=" + th.score);
                        if (th.id == initialMovieId) {
                            mainScore = th.score;
                            break;
                        }
                    }
                    if (mainScore == 0.0) {
                        mainScore = titleHits.get(0).score;
                    }
                }
            } catch (Exception ex) {
                System.err.println("DEBUG: title-search fallback failed: " + ex.getMessage());
            }
        }

        if (hits != null) {
            for (ElasticsearchService.HitResult hit : hits) {
                if (hit.id == initialMovieId) continue;
                Movie m = movieDao.findById(hit.id);
                if (m != null) recommendations.add(new ScoredMovie(m, hit.score));
                if (recommendations.size() >= limit) break;
            }
        }

        if (recommendations.size() < limit) {
            String fallbackQuery = (mainMovie.title == null ? "" : mainMovie.title + " ") +
                    (mainMovie.genres == null ? "" : String.join(" ", mainMovie.genres));
            try {
                List<ElasticsearchService.HitResult> more = candidateGenerator.generate(fallbackQuery, candidatePool);
                System.out.println("DEBUG: fallback hits=" + (more == null ? 0 : more.size()) + " for query='" + fallbackQuery + "'");
                if (more != null) {
                    for (ElasticsearchService.HitResult hit : more) {
                        if (hit.id == initialMovieId) continue;
                        boolean already = false;
                        for (ScoredMovie sm : recommendations) {
                            if (sm.movie.id == hit.id) { already = true; break; }
                        }
                        if (already) continue;
                        Movie m = movieDao.findById(hit.id);
                        if (m != null) recommendations.add(new ScoredMovie(m, hit.score));
                        if (recommendations.size() >= limit) break;
                    }
                }
            } catch (Exception ex) {
                System.err.println("DEBUG: fallback generate failed: " + ex.getMessage());
            }
        }

        System.out.println("DEBUG: recommendWithMain -> mainScore=" + mainScore + ", recCount=" + recommendations.size());


        return SearchResponse.ok(mainMovie, mainScore, recommendations);
    }

    public MovieDao getMovieDao() {
        return movieDao;
    }

    public EsCandidateGenerator getCandidateGenerator() {
        return candidateGenerator;
    }
}
