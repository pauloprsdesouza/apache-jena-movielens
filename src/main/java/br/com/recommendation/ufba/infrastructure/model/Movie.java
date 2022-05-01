package br.com.recommendation.ufba.infrastructure.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Movie implements Serializable {
    private Integer movieId;
    private String title;
    private String genres;

    public Integer getId() {
        return this.movieId;
    }

    public void setId(Integer movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getGenres() {
        return Arrays.asList(this.genres.split("|"));
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }
}
