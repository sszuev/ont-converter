package com.github.sszuev.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;

import ru.avicomp.ontapi.OntFormat;

/**
 * The helper to work with {@link IRI}s, usually related to local files.
 *
 * Created by @szuev on 15.01.2018.
 */
public class IRIs {

    /**
     * Determines if the specified iri ends with extension
     *
     * @param extension String
     * @param iri       {@link IRI}
     * @return true if iri has an extension on its end.
     */
    public static boolean hasExtension(String extension, IRI iri) {
        return extension.equalsIgnoreCase(FilenameUtils.getExtension(iri.toString()));
    }

    /**
     * Returns all files from a directory, most deep go first, empty files are excluded
     * @param dir {@link Path}, the directory (but can be file also)
     * @return List of {@link IRI}s
     * @throws IOException if any i/o error occurs.
     */
    public static List<IRI> getFiles(Path dir) throws IOException {
        IOException io = new IOException("Unexpected i/o errors while processing " + dir);
        return walk(dir, io).collect(Collectors.toList());
    }

    /**
     * Walks through directory, the output stream are sorted in descending order of file depth
     * @param dir {@link Path}
     * @param holder {@link IOException} to collect any internal (file-specified) exceptions
     * @return Stream of {@link IRI}s
     * @throws IOException if i/o error occurs when read directory
     */
    public static Stream<IRI> walk(Path dir, IOException holder) throws IOException {
        return Files.walk(dir)
                .map(f -> {
                    try {
                        return f.toRealPath();
                    } catch (IOException e) {
                        holder.addSuppressed(e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(f -> Files.isRegularFile(f))
                .filter(f -> {
                    try {
                        return Files.size(f) != 0;
                    } catch (IOException e) {
                        holder.addSuppressed(e);
                        return false;
                    }
                })
                .sorted(Comparator.comparingInt(Path::getNameCount).reversed())
                .map(Path::toUri)
                .map(IRI::create);
    }


    /**
     * Creates a document-source from iri and format
     * @param document {@link IRI}, not null
     * @param format {@link OntFormat}, nullable
     * @return {@link OWLOntologyDocumentSource}
     */
    public static OWLOntologyDocumentSource toSource(IRI document, OntFormat format) {
        if (format == null) {
            return new IRIDocumentSource(document);
        }
        return new FileDocumentSource(Paths.get(document.toURI()).toFile(), format.createOwlFormat());
    }

    /**
     * Retrieves an iri from document-source
     *
     * @param source {@link OWLOntologyDocumentSource}
     * @return {@link IRI}
     */
    public static IRI getDocumentIRI(OWLOntologyDocumentSource source) {
        if (source instanceof FileDocumentSource) {
            return IRI.create(Paths.get(source.getDocumentIRI().toURI()).toUri());
        }
        return source.getDocumentIRI();
    }

    public static IRI toName(OWLOntologyID id, IRI orElse) {
        return id.getOntologyIRI().orElse(orElse);
    }

    public static IRI toName(OWLOntology o) {
        return toName(o.getOntologyID(), o.getOWLOntologyManager().getOntologyDocumentIRI(o));
    }
}
