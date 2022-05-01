package br.com.recommendation.ufba;

import java.io.IOException;

import br.com.recommendation.ufba.services.OntologyService;

/**
 * Paulo Roberto de Souza
 *
 */
public class App {
    public static void main(String[] args) {
        try {
            new OntologyService().createOntology().exportOntology();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
