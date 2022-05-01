package br.com.recommendation.ufba.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;

import br.com.recommendation.ufba.infrastructure.model.Movie;
import br.com.recommendation.ufba.infrastructure.model.Rating;

public class OntologyService {
    private OntModel ontModel;
    private ObjectProperty performsEvaluation;
    private ObjectProperty hasEvaluated;
    private ObjectProperty hasNotWatched;
    private DatatypeProperty hasRating;
    private DatatypeProperty hasTitle;
    private DatatypeProperty hasYear;
    private OntClass userClass;
    private OntClass evaluationClass;
    private OntClass movieClass;
    private final String NS = "urn:x-hp:eg/";

    private Collection<Movie> movies;
    private Collection<Rating> ratings;
    private List<Integer> userIds;

    public OntologyService() {
        ontModel = ModelFactory.createOntologyModel();
        ontModel.createOntology("http://www.example.com.br/onto-jena/");

        userClass = ontModel.createClass(NS + "User");
        evaluationClass = ontModel.createClass(NS + "Evaluation");
        movieClass = ontModel.createClass(NS + "Movie");

        performsEvaluation = ontModel.createObjectProperty(NS + "performsEvaluation");
        hasEvaluated = ontModel.createObjectProperty(NS + "hasEvaluated");
        hasNotWatched = ontModel.createObjectProperty(NS + "hasNotWatched");

        hasRating = ontModel.createDatatypeProperty(NS + "hasRating");
        hasTitle = ontModel.createDatatypeProperty(NS + "hasTitle");
        hasYear = ontModel.createDatatypeProperty(NS + "hasYear");

        try {
            movies = readMoviesFromJson();
            ratings = readRatingsFromJson();
        } catch (FileNotFoundException e) {
            System.out.println("Unable to read json files.");
        }

        userIds = ratings.stream().map(p -> p.getUserId()).distinct().collect(Collectors.toList());
    }

    public Collection<Movie> readMoviesFromJson() throws FileNotFoundException {
        JsonReader jsonReader = new JsonReader(new FileReader(
                "src/main/java/br/com/recommendation/ufba/infrastructure/resources/movies.json"));
        Type listTyped = new TypeToken<Collection<Movie>>() {
        }.getType();

        return new Gson().fromJson(jsonReader, listTyped);
    }

    public Collection<Rating> readRatingsFromJson() throws FileNotFoundException {
        JsonReader jsonReader = new JsonReader(new FileReader(
                "src/main/java/br/com/recommendation/ufba/infrastructure/resources/ratings.json"));
        Type listTyped = new TypeToken<Collection<Rating>>() {
        }.getType();

        return new Gson().fromJson(jsonReader, listTyped);
    }

    public void createMovies() {
        for (Movie movie : movies) {
            Individual movieIndividual = movieClass.createIndividual(NS + movie.getTitle());

            ontModel.add(movieIndividual, hasTitle,
                    ResourceFactory.createTypedLiteral(String.valueOf(movie.getTitle())));
            ontModel.add(movieIndividual, hasYear,
                    ResourceFactory.createTypedLiteral(Integer.valueOf(getMovieYear(movie.getTitle()))));
        }
    }

    public void createUsers() {
        for (int userId : userIds) {
            userClass.createIndividual(NS + userId);
        }
    }

    public void createMoviesWatched() {
        List<Rating> ratingsByUser = null;
        int ratingsIndex = 0;
        int userIndex = 0;

        while (userIndex < 5) {
            final int userIndexConst = userIndex;

            ratingsByUser = ratings.stream().filter(p -> p.getUserId() == userIds.get(userIndexConst))
                    .collect(Collectors.toList());

            if (ratingsByUser != null) {
                Ulid ulid = UlidCreator.getUlid();

                Rating rating = ratingsByUser.get(ratingsIndex);

                if (rating != null) {
                    Movie movie = movies.stream().filter(p -> p.getId() == rating.getMovieId()).findFirst()
                            .orElse(null);

                    if (movie != null) {
                        Individual movieIndividual = ontModel.getIndividual(NS + movie.getTitle());
                        Individual evaluation = evaluationClass.createIndividual(NS +
                                ulid.toString());
                        Individual user = ontModel.getIndividual(NS + rating.getUserId());

                        ontModel.add(user, performsEvaluation, evaluation);
                        ontModel.add(evaluation, hasEvaluated, movieIndividual);
                        ontModel.add(evaluation, hasRating,
                                ResourceFactory.createTypedLiteral(Float.valueOf(rating.getRating())));
                    }
                }
            }

            ratingsIndex++;

            if (ratingsByUser.size() == ratingsIndex) {
                ratingsIndex = 0;
                userIndex++;
            }
        }
    }

    public void createMoviesNotWatched() {
        List<Rating> ratingsByUser = null;
        int ratingsIndex = 0;
        int userIndex = 0;

        while (userIndex < 5) {
            final int userIndexConst = userIndex;

            ratingsByUser = ratings.stream().filter(p -> !(p.getUserId() == userIds.get(userIndexConst)))
                    .collect(Collectors.toList());

            if (ratingsByUser != null) {
                Rating rating = ratingsByUser.get(ratingsIndex);

                if (rating != null) {
                    Movie movie = movies.stream().filter(p -> p.getId() == rating.getMovieId()).findFirst()
                            .orElse(null);

                    if (movie != null) {
                        Individual movieIndividual = ontModel.getIndividual(NS + movie.getTitle());
                        Individual user = ontModel.getIndividual(NS + rating.getUserId());
                        ontModel.add(user, hasNotWatched, movieIndividual);
                    }
                }
            }

            ratingsIndex++;

            if (ratingsByUser.size() == ratingsIndex) {
                ratingsIndex = 0;
                userIndex++;
            }
        }
    }

    public int getMovieYear(String movieTitle) {
        Pattern pattern = Pattern.compile("[0-9]{4}");
        Matcher matcher = pattern.matcher(movieTitle);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(0));
        }

        return 0;
    }

    public OntologyService createOntology() throws IOException {
        createMovies();
        createUsers();
        createMoviesWatched();
        createMoviesNotWatched();

        return this;
    }

    public void exportOntology() throws IOException {
        File file = new File("ontologyMovies.owl");

        OutputStream outputStream = new FileOutputStream(file);

        ontModel.setNsPrefix("rdfs",
                "http://www.w3.org/2000/01/rdf-schema#");

        ontModel.write(outputStream, "RDF/XML", NS);
        outputStream.close();
    }
}
