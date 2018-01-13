package com.github.sszuev.ontapi;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A simple iri mapper.
 *
 * Created by @szuev on 10.01.2018.
 */
public class IRIMap implements OWLOntologyIRIMapper {
    private final Map<OWLOntologyID, IRI> map = new LinkedHashMap<>();

    @Override
    public IRI getDocumentIRI(@Nonnull IRI ontologyIRI) {
        return documentIRI(ontologyIRI).orElse(null);
    }

    public Optional<IRI> documentIRI(IRI ontologyIRI) {
        return map.entrySet().stream()
                .filter(e -> e.getKey().getOntologyIRI().filter(ontologyIRI::equals).isPresent())
                .map(Map.Entry::getValue).findFirst();
    }

    /**
     * @param id          {@link OWLOntologyID}, the ontology id
     * @param documentIRI document source iri
     */
    public void add(OWLOntologyID id, IRI documentIRI) {
        map.put(Objects.requireNonNull(id, "id cannot be null"),
                Objects.requireNonNull(documentIRI, "documentIRI cannot be null"));
    }

    public Map<OWLOntologyID, IRI> toMap() {
        return Collections.unmodifiableMap(map);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public String toString() {
        return map.entrySet().stream().map(e ->
                e.getKey().getOntologyIRI().orElse(IRI.create("anonymous"))
                        + " => " +
                        e.getValue()).collect(Collectors.joining(", ", "[", "]"));
    }
}
