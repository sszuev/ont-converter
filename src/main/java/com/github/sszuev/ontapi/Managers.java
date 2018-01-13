package com.github.sszuev.ontapi;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.transforms.GraphTransformers;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A helper to work with {@link OntologyManager manager}s.
 *
 * Created by @szuev on 11.01.2018.
 */
public class Managers {

    public static OntologyManager createManager(GraphTransformers.Store transformers, List<OntConfig.Scheme> schemes) {
        OntologyManager m = OntManagers.createONT();
        if (transformers != null) {
            m.getOntologyConfigurator().setGraphTransformers(transformers);
        }
        if (schemes != null) {
            m.getOntologyConfigurator().setSupportedSchemes(schemes);
        }
        return m;
    }

    public static OntologyManager copyManager(OntologyManager from, boolean ignoreExceptions) {
        OntologyManager res = OntManagers.createONT();
        OntApiException ex = new OntApiException("Can't copy manager");
        from.ontologies()
                .sorted(Comparator.comparingInt(o -> (int) o.imports().count()))
                .forEach(o -> {
                    try {
                        res.copyOntology(o, OntologyCopy.DEEP);
                    } catch (OWLOntologyCreationException e) {
                        ex.addSuppressed(e);
                    }
                });
        if (ignoreExceptions) return res;
        if (ex.getSuppressed().length != 0) {
            throw ex;
        }
        return res;
    }

    public static OntologyManager copyManager(OntologyManager from) {
        return copyManager(from, false);
    }

    public static IRIMap createMappers(Path dir) throws IOException {
        OntologyManager manager = OntManagers.createONT();
        manager.getOntologyConfigurator()
                .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)
                .setPerformTransformation(false)
                .setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE));
        return loadDirectory(manager, dir, true, true, false, null);
    }

    public static IRIMap loadDirectory(OntologyManager manager, Path dir, PrintStream logs) throws IOException {
        return loadDirectory(manager, dir, true, true, false, logs);
    }

    public static IRIMap loadDirectory(OntologyManager manager,
                                       Path dir,
                                       boolean throwIOError,
                                       boolean throwLoadError,
                                       boolean throwDuplicateError,
                                       PrintStream logs) throws IOException, OntApiException {
        IRIMap map = new IRIMap();
        OntApiException duplicate = new OntApiException("Duplicate ontologies inside dir " + dir + ":");
        OntApiException load = new OntApiException("Errors while creating mapping for dir " + dir + ":");
        IOException io = new IOException("Unexpected i/o errors while processing dir " + dir + ":");
        Files.walk(dir)
                .sorted(Comparator.comparingInt(Path::getNameCount).reversed())
                .map(f -> {
                    try {
                        return f.toRealPath();
                    } catch (IOException e) {
                        io.addSuppressed(e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(f -> Files.isRegularFile(f))
                .map(Path::toUri)
                .map(IRI::create).forEach(docIRI -> {
            OWLOntologyID id;
            try {
                id = manager.loadOntologyFromOntologyDocument(docIRI).getOntologyID();
            } catch (OWLOntologyAlreadyExistsException e) {
                duplicate.addSuppressed(e);
                return;
            } catch (OWLOntologyCreationException | OntApiException e) {
                load.addSuppressed(wrap("Can't load " + docIRI, e));
                return;
            }
            if (logs != null) {
                logs.println("The ontology " + docIRI + " is loaded.");
            }
            map.add(id, docIRI);
        });
        if (throwIOError && io.getSuppressed().length != 0) {
            throw io;
        }
        if (throwLoadError && load.getSuppressed().length != 0)
            throw load;
        if (throwDuplicateError && duplicate.getSuppressed().length != 0) {
            throw duplicate;
        }
        return map;
    }

    private static Exception wrap(String message, Exception e) {
        return new Exception(message, e);
    }
}
