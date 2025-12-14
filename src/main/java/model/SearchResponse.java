package model;

import java.util.List;

public class SearchResponse {
    public String status;
    public Movie movie;
    public Double score;
    public List<ScoredMovie> recommendations;
    public String message;

    public static SearchResponse ok(Movie movie, Double score, List<ScoredMovie> recs) {
        SearchResponse r = new SearchResponse();
        r.status = "ok";
        r.movie = movie;
        r.score = score;
        r.recommendations = recs;
        return r;
    }

    public static SearchResponse error(String msg) {
        SearchResponse r = new SearchResponse();
        r.status = "error";
        r.message = msg;
        return r;
    }
}
