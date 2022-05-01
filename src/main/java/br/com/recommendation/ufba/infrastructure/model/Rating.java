package br.com.recommendation.ufba.infrastructure.model;

import java.io.Serializable;

public class Rating implements Serializable {
    private Integer userId;
    private Integer movieId;
    private float rating;

    public Integer getUserId() {
        return this.userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getMovieId() {
        return this.movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public float getRating() {
        return this.rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }
}
