package com.github.sszuev.utils;

import org.apache.jena.lang.csv.ReaderRIOTFactoryCSV;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParserRegistry;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import ru.avicomp.ontapi.OntFormat;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utils to work with {@link OntFormat format}s.
 * <p>
 * Created by @szuev on 11.01.2018.
 */
@SuppressWarnings("WeakerAccess")
public class Formats {

    /**
     * Finds {@link OntFormat} by string alias
     *
     * @param key String, key to search
     * @return {@link OntFormat}, not null
     * @throws NullPointerException     if no argument
     * @throws IllegalArgumentException if no result
     */
    public static OntFormat find(String key) {
        Objects.requireNonNull(key, "Null search key");
        return OntFormat.formats().filter(f -> aliases(f).contains(key.toLowerCase())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find format '" + key + "'"));
    }

    /**
     * Returns format aliases
     *
     * @param f {@link OntFormat}, not null
     * @return List of stings corresponding specified format.
     */
    public static List<String> aliases(OntFormat f) {
        return Stream.of(String.valueOf(f.ordinal()), f.name(), f.getID(), f.getExt())
                .map(String::toLowerCase)
                .distinct().collect(Collectors.toList());
    }

    /**
     * Registers {@link Lang#CSV} in jena system.
     * This operation enables {@link OntFormat#CSV} for reading operations.
     */
    public static void registerJenaCSV() {
        RDFParserRegistry.removeRegistration(Lang.CSV);
        RDFParserRegistry.registerLangTriples(Lang.CSV, new ReaderRIOTFactoryCSV());
    }

    /**
     * Retrieves format from ontology
     *
     * @param o {@link OWLOntology}, not null
     * @return Optional around {@link OntFormat}
     */
    public static Optional<OntFormat> format(OWLOntology o) {
        OWLDocumentFormat f = o.getFormat();
        if (f == null) return Optional.empty();
        return Optional.ofNullable(OntFormat.get(f));
    }
}
