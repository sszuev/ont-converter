package com.github.sszuev.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

import org.apache.jena.riot.system.stream.StreamManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sszuev.ontapi.IRIMap;
import com.github.sszuev.ontapi.OWLStreamManager;
import com.github.sszuev.spin.SpinTransform;
import ru.avicomp.ontapi.*;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.transforms.GraphTransformers;
import ru.avicomp.ontapi.transforms.OWLTransform;

/**
 * Created by @szuev on 15.01.2018.
 */
public class Managers {
    private static final Logger LOGGER = LoggerFactory.getLogger(Managers.class);

    static {
        // exclude csv by default:
        Formats.unregisterJenaCSV();
    }

    /**
     * Creates a new manager with default settings.
     *
     * @return {@link OntologyManager}
     */
    public static OntologyManager newManager() {
        return OntManagers.createONT();
    }

    /**
     * Creates a manager to traverse through directory.
     * This manager is soft: missing imports are ignored, all punnings are allowed,
     * no transformations with except of {@link OWLTransform} (to fix ontology IRI), no web access,
     *
     * @return {@link OntologyManager}
     */
    public static OntologyManager createSoftManager() {
        OntologyManager manager = newManager();
        manager.getOntologyConfigurator()
                .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)
                .setGraphTransformers(new GraphTransformers.Store().addFirst(OWLTransform::new))
                .setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE))
                .setPersonality(OntModelConfig.ONT_PERSONALITY_LAX);
        return manager;
    }

    /**
     * Creates a manager without web-access
     *
     * @param map                {@link IRIMap} the mapper
     * @param skipMissingImports if true wrong imports will be ignored
     * @param transformSpin      if true run {@link SpinTransform} to fix bulk []-List based SPARQL-Queries, which can be present in spin-library rdf-ontologies.
     * @return {@link OntologyManager}
     */
    public static OntologyManager createManager(IRIMap map, boolean skipMissingImports, boolean transformSpin) {
        OntologyManager manager = createManager(skipMissingImports, transformSpin);
        OntConfig config = manager.getOntologyConfigurator();
        config.setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE));
        StreamManager.setGlobal(new OWLStreamManager(map, config.getSupportedSchemes()));
        manager.getIRIMappers().add(map);
        return manager;
    }

    /**
     * @param allowFollowRedirects true to prohibit web-traversing
     * @param skipMissingImports   true to ignore missing imports
     * @param transformSpin        true to enable spin transformation
     * @return {@link OntologyManager}
     */
    public static OntologyManager createManager(boolean allowFollowRedirects, boolean skipMissingImports, boolean transformSpin) {
        OntologyManager manager = createManager(skipMissingImports, transformSpin);
        if (!allowFollowRedirects) {
            OntConfig config = manager.getOntologyConfigurator();
            List<OntConfig.Scheme> schemes = Collections.singletonList(OntConfig.DefaultScheme.FILE);
            config.setSupportedSchemes(schemes);
            StreamManager.setGlobal(new OWLStreamManager(schemes));
        }
        return manager;
    }

    /**
     * @param skipMissingImports true to ignore missing imports
     * @param transformSpin      true to enable spin transformation
     * @return {@link OntologyManager}
     */
    public static OntologyManager createManager(boolean skipMissingImports, boolean transformSpin) {
        OntologyManager manager = newManager();
        OntConfig config = manager.getOntologyConfigurator();
        if (transformSpin) {
            GraphTransformers.Store transformers = config.getGraphTransformers();
            config.setGraphTransformers(transformers.addFirst(SpinTransform::new));
        }
        if (skipMissingImports) {
            config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        }
        return manager;
    }

    /**
     * Copies managers content
     *
     * @param from             {@link OntologyManager} the source manager
     * @param ignoreExceptions true to ignore any errors
     * @return {@link OntologyManager} the target manager
     */
    public static OntologyManager copyManager(OntologyManager from, boolean ignoreExceptions) {
        OntologyManager res = newManager();
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

    /**
     * Copies managers content
     *
     * @param from {@link OntologyManager} the source manager
     * @return {@link OntologyManager} the target manager
     */
    public static OntologyManager copyManager(OntologyManager from) {
        return copyManager(from, false);
    }

    /**
     * Loads an ontology to manager.
     * Since {@link org.apache.jena.riot.Lang#CSV} is very tolerant (almost all textual files could be treated as CSV table)
     * it is excluded from consideration. There is only exception - if the file has extension '.csv'
     *
     * @param manager {@link OntologyManager} the manager to put ontology
     * @param source  {@link OWLOntologyDocumentSource} the source with information to load
     * @return {@link OntologyModel} the ontology
     * @throws OWLOntologyCreationException in case of any error
     */
    public static OntologyModel loadOntology(OntologyManager manager, OWLOntologyDocumentSource source) throws OWLOntologyCreationException {
        Optional<OntFormat> format = Formats.format(source);
        boolean isCvs = (!format.isPresent() && Formats.isCSV(source.getDocumentIRI())) || format.filter(s -> Objects.equals(s, OntFormat.CSV)).isPresent();
        try {
            if (isCvs) {
                Formats.registerJenaCSV();
            }
            return manager.loadOntologyFromOntologyDocument(source);
        } finally {
            Formats.unregisterJenaCSV();
        }
    }

    /**
     * Creates a collection of {@link org.semanticweb.owlapi.model.OWLOntologyIRIMapper} by traversing the specified directory.
     * Each map will content unique ontology-id+file-iri pairs.
     *
     * @param dir {@link Path} the file (usually directory)
     * @param format {@link OntFormat}, can be null
     * @return a List of independent {@link IRIMap}
     * @throws IOException if something is wrong
     */
    public static List<IRIMap> createMappings(Path dir, OntFormat format) throws IOException {
        return loadDirectory(dir, format, Managers::createSoftManager, true);
    }


    /**
     * Loads a directory to {@link IRIMap} object.
     * Directory could content duplicated ontologies, in that case several mappings would be created
     *
     * @param dir             {@link Path}
     * @param format {@link OntFormat}, null to choose the most suitable.
     * @param factory         factory to create {@link OntologyManager} for each mapping
     * @param continueIfError if true just prints errors and go ahead, otherwise throws {@link OntApiException}
     * @return List of {@link IRIMap}s without duplicated ontologies inside
     * @throws IOException     if any i/o problem occurs.
     * @throws OntApiException if file is unparsable or unloadable
     */
    public static List<IRIMap> loadDirectory(Path dir, OntFormat format, Supplier<OntologyManager> factory, boolean continueIfError) throws IOException, OntApiException {
        List<IRI> files = IRIs.getFiles(dir);
        List<IRIMap> res = new ArrayList<>();
        while (true) {
            List<IRI> next = new ArrayList<>();
            IRIMap map = new IRIMap();
            OntologyManager manager = factory.get();
            files.forEach(doc -> {
                OntologyModel o;
                try {
                    o = loadOntology(manager, IRIs.toSource(doc, format));
                } catch (OWLOntologyAlreadyExistsException e) {
                    next.add(doc);
                    return;
                } catch (UnparsableOntologyException e) {
                    if (!continueIfError)
                        throw new OntApiException("Can't parse document " + doc, e);
                    LOGGER.error("Can't parse document {}", doc);
                    return;
                } catch (OWLOntologyCreationException e) {
                    throw new OntApiException("Error", e);
                }
                map.put(doc, o);
            });
            if (!map.isEmpty()) {
                res.add(map);
            }
            if (next.isEmpty()) break;
            files = next;
        }
        return res;
    }

}
