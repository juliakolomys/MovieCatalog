package search;

public class Candidate {
    public final int id;
    public final double bm25Score;

    public Candidate(int id, double bm25Score) {
        this.id = id;
        this.bm25Score = bm25Score;
    }
}
