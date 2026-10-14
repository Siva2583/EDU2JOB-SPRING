package com.edu2job.dto;

/**
 * One ranked job-role recommendation.
 *
 * <p>{@code score} is {@code null} unless the ML model genuinely produced a
 * meaningful numeric score for this role. Scores are never fabricated: when
 * the new model only returns ranked role names, the score stays null and the
 * UI hides it.</p>
 */
public class RankedRole {

    private int rank;
    private String role;
    private Double score;

    public RankedRole() {
    }

    public RankedRole(int rank, String role, Double score) {
        this.rank = rank;
        this.role = role;
        this.score = score;
    }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
