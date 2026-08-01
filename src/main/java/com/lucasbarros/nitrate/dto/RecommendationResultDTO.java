package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.recommender.RecommendationResult;

import java.io.Serializable;
import java.util.List;

public class RecommendationResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long movieId;
    private String title;
    private Double score;
    private List<String> reasons;

    public RecommendationResultDTO() {
    }

    public RecommendationResultDTO(RecommendationResult result) {
        this.movieId = result.getMovie().getId();
        this.title = result.getMovie().getTitle();
        this.score = result.getScore();
        this.reasons = result.getReasons();
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}
