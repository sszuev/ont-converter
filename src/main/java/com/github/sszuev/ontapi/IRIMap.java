package com.github.sszuev.ontapi;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;

import com.github.sszuev.utils.Formats;
import com.github.sszuev.utils.IRIs;
import ru.avicomp.ontapi.OntFormat;

/**
 * An iri mapper to work with local File System.
 * Contains a collection of {@link OWLOntology ontologies} and a reference to the {@link OWLOntologyManager}
 * <p>
 * Created by @szuev on 10.01.2018.
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class IRIMap implements OWLOntologyIRIMapper {
    private final Map<IRI, OWLOntology> map = new LinkedHashMap<>();

    /**
     * To use inside {@link OWLOntologyManager}
     *
     * @param ontologyIRI the iri of ontology
     * @return file iri, nullable.
     */
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

    /**
     *
     * @param document {@link IRI} file iri
     * @param ontology {@link OWLOntology} the ontology to map
     * @return this mapper
     * @throws NullPointerException if null arguments
     * @throws IllegalArgumentException if the specified ontology is wrong: has no manager or attached to another manager
     */
    public IRIMap put(IRI document, OWLOntology ontology) throws NullPointerException, IllegalArgumentException {
        if (map.containsKey(Objects.requireNonNull(document, "Document iri cannot be null"))) {
            throw new IllegalArgumentException("The map already contains document " + document);
        }
        Objects.requireNonNull(ontology, "Ontology cannot be null");
        Optional<OWLOntologyManager> manager = manager();
        OWLOntologyManager m = ontology.getOWLOntologyManager();
        if (m == null) {
            throw new IllegalArgumentException("The ontology has no manager");
        }
        if (!manager.isPresent() || manager.get().equals(m)) {
            map.put(document, ontology);
            return this;
        }
        throw new IllegalArgumentException("Wrong manager inside " + ontology.getOntologyID() + ". File " + document);
    }

    /**
     *
     * @return Optional around {@link OWLOntologyManager}, empty in case the mapper is empty
     */
    public Optional<OWLOntologyManager> manager() {
        return map.values().stream().map(OWLOntology::getOWLOntologyManager).findFirst();
    }

    public Stream<IRI> documents() {
        return map.keySet().stream();
    }

    public Stream<OWLOntologyID> ids() {
        return map.values().stream().map(HasOntologyID::getOntologyID);
    }

    /**
     * Return a stream of document sources in ascending order of imports count
     *
     * @return Stream of {@link OWLOntologyDocumentSource}s
     * @throws IllegalStateException in case of something wrong with state of this mapper.
     */
    public Stream<OWLOntologyDocumentSource> sources() throws IllegalStateException {
        return map.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getValue().importsDeclarations().count()))
                .map(e -> IRIs.toSource(e.getKey(), getFormat(e.getValue())));
    }

    private static OntFormat getFormat(OWLOntology o) {
        return Formats.format(o).orElseThrow(() -> new IllegalStateException("Null format for " + o.getOntologyID()));
    }

    /**
     * Represents the mapper as java map with file {@link IRI}s as keys, {@link OWLOntologyID}s as values
     * @return immutable {@link Map}
     */
    public Map<IRI, OWLOntologyID> toMap() {
        Map<IRI, OWLOntologyID> map = new LinkedHashMap<>();
        this.map.forEach((iri, o) -> map.put(iri, o.getOntologyID()));
        return Collections.unmodifiableMap(map);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    private static String toFormatString(OWLOntology o) {
        return Formats.format(o).map(Enum::name).orElse("null");
    }

    private static String toIRIString(OWLOntology o) {
        return IRIs.toName(o.getOntologyID(), IRI.create("anonymous")).getIRIString();
    }

    @Override
    public String toString() {
        return map.entrySet().stream().map(e -> String.format("%s => %s [%s]",
                e.getKey(),
                toIRIString(e.getValue()),
                toFormatString(e.getValue()))).collect(Collectors.joining("\n"));
    }

}
