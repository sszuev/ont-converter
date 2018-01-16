package com.github.sszuev.ontapi;

import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntFormat;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A simple iri mapper for local File System.
 * todo: description
 *
 * Created by @szuev on 10.01.2018.
 */
public class IRIMap implements OWLOntologyIRIMapper {
    private final Map<IRI, OWLOntology> map = new LinkedHashMap<>();

    @Override
    public IRI getDocumentIRI(@Nonnull IRI ontologyIRI) {
        return documentIRI(ontologyIRI).orElse(null);
    }

    public Optional<IRI> documentIRI(IRI ontologyIRI) {
        return map.entrySet().stream()
                .filter(e -> e.getValue().getOntologyID().matchOntology(ontologyIRI))
                .map(Map.Entry::getKey).findFirst();
    }

    public Optional<OWLOntologyID> ontologyID(IRI documentIRI) {
        return Optional.ofNullable(map.get(documentIRI)).map(HasOntologyID::getOntologyID);
    }

    public IRIMap put(IRI document, OWLOntology ontology) throws NullPointerException, IllegalArgumentException {
        if (map.containsKey(Objects.requireNonNull(document, "Document iri cannot be null"))) {
            throw new IllegalArgumentException("The map already contains document " + document);
        }
        Objects.requireNonNull(ontology, "Ontology cannot be null");
        Optional<OWLOntologyManager> manager = manager();
        if (!manager.isPresent() || manager.get().equals(ontology.getOWLOntologyManager())) {
            map.put(document, ontology);
            return this;
        }
        throw new IllegalArgumentException("Wrong manager inside " + ontology.getOntologyID() + ". File " + document);
    }

    public Optional<OWLOntologyManager> manager() {
        return map.values().stream().map(OWLOntology::getOWLOntologyManager).findFirst();
    }

    public Stream<IRI> documents() {
        return map.keySet().stream();
    }

    public Stream<OWLOntologyDocumentSource> sources() throws IllegalStateException {
        return map.entrySet().stream().map(e -> IRIs.toSource(e.getKey(), format(e.getValue())
                .orElseThrow(() -> new IllegalStateException("Null format for " + e.getValue().getOntologyID()))));
    }

    public static Optional<OntFormat> format(OWLOntology o) {
        OWLDocumentFormat f = o.getFormat();
        if (f == null) return Optional.empty();
        return Optional.ofNullable(OntFormat.get(o.getFormat()));
    }

    public Map<IRI, OWLOntologyID> toMap() {
        Map<IRI, OWLOntologyID> map = new LinkedHashMap<>();
        this.map.forEach((iri, o) -> map.put(iri, o.getOntologyID()));
        return Collections.unmodifiableMap(map);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public String toString() {
        return map.entrySet().stream().map(e -> e.getKey() + " => " +
                e.getValue().getOntologyID().getOntologyIRI().map(IRI::getIRIString).orElse("anonymous") +
                "[" + format(e.getValue()).map(Enum::name).orElse("null") + "]").collect(Collectors.joining("\n"));
    }
}
