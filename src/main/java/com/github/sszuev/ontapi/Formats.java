package com.github.sszuev.ontapi;

import org.apache.jena.lang.csv.ReaderRIOTFactoryCSV;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParserRegistry;
import org.semanticweb.owlapi.model.IRI;
import ru.avicomp.ontapi.OntFormat;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utils to work with {@link OntFormat format}s.
 *
 * Created by @szuev on 11.01.2018.
 */
public class Formats {

    public static OntFormat find(String key) {
        Objects.requireNonNull(key, "Null search key");
        return OntFormat.formats().filter(f -> aliases(f).contains(key.toLowerCase())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find format '" + key + "'"));
    }

    public static List<String> aliases(OntFormat f) {
        return Stream.of(String.valueOf(f.ordinal()), f.name(), f.getID(), f.getExt())
                .map(String::toLowerCase)
                .distinct().collect(Collectors.toList());
    }

    public static void registerJenaCSV() {
        RDFParserRegistry.removeRegistration(Lang.CSV);
        RDFParserRegistry.registerLangTriples(Lang.CSV, new ReaderRIOTFactoryCSV());
    }

    public static void unregisterJenaCSV() {
        RDFParserRegistry.removeRegistration(Lang.CSV);
    }

    public static boolean isCSV(IRI iri) {
        return IRIs.hasExtension(OntFormat.CSV.getExt(), iri);
    }
}
