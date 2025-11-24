package model;

public class ScoredMovie {
    public final Movie movie;
    public final double score;

    public ScoredMovie(Movie movie, double score) {
        this.movie = movie;
        this.score = score;
    }

    @Override
    public String toString() {
        return movie.title + " score:" + score;
    }
}
