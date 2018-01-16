package com.github.sszuev.ontapi;

import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import ru.avicomp.ontapi.OntFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO: rename to Files , to IO, to something else ?
 * Created by @szuev on 15.01.2018.
 */
public class IRIs {

    public static boolean hasExtension(String extension, IRI iri) {
        return extension.equalsIgnoreCase(FilenameUtils.getExtension(iri.toString()));
    }

    /**
     * @param dir
     * @return
     * @throws IOException
     */
    public static List<IRI> getFiles(Path dir) throws IOException {
        IOException io = new IOException("Unexpected i/o errors while processing " + dir);
        return walk(dir, io).collect(Collectors.toList());
    }

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


    public static OWLOntologyDocumentSource toSource(IRI document, OntFormat format) {
        if (format == null) {
            return new IRIDocumentSource(document);
        }
        return new FileDocumentSource(Paths.get(document.toURI()).toFile(), format.createOwlFormat());
    }

}
