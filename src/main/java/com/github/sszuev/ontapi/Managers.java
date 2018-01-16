package com.github.sszuev.ontapi;

import com.github.sszuev.spin.SpinTransform;
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
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.transforms.GraphTransformers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by @szuev on 15.01.2018.
 */
public class Managers {
    private static final Logger LOGGER = LoggerFactory.getLogger(Managers.class);

    static {
        // exclude csv by default:
        Formats.unregisterJenaCSV();
    }

    public static OntologyManager newManager() {
        return OntManagers.createONT();
    }

    public static OntologyManager createSoftManager() {
        OntologyManager manager = newManager();
        manager.getOntologyConfigurator()
                .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)
                .setPerformTransformation(false)
                .setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE))
                .setPersonality(OntModelConfig.ONT_PERSONALITY_LAX);
        return manager;
    }

    public static OntologyManager createManager(IRIMap map, boolean skipMissingImports, boolean transformSpin) { // no web access
        OntologyManager manager = createManager(skipMissingImports, transformSpin);
        OntConfig config = manager.getOntologyConfigurator();
        config.setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE));
        StreamManager.setGlobal(new NoWebStreamManager(map, config.getSupportedSchemes()));
        manager.getIRIMappers().add(map);
        return manager;
    }

    public static OntologyManager createManager(boolean allowFollowRedirects, boolean skipMissingImports, boolean transformSpin) {
        OntologyManager manager = createManager(skipMissingImports, transformSpin);
        if (!allowFollowRedirects) {
            OntConfig config = manager.getOntologyConfigurator();
            List<OntConfig.Scheme> schemes = Collections.singletonList(OntConfig.DefaultScheme.FILE);
            config.setSupportedSchemes(schemes);
            StreamManager.setGlobal(new NoWebStreamManager(schemes));
        }
        return manager;
    }

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

    public static OntologyManager copyManager(OntologyManager from) {
        return copyManager(from, false);
    }

    public static OntologyModel loadOntology(OntologyManager manager, OWLOntologyDocumentSource source) throws OWLOntologyCreationException {
        try {
            if (Formats.isCSV(source.getDocumentIRI())) {
                Formats.registerJenaCSV();
            }
            return manager.loadOntologyFromOntologyDocument(source);
        } finally {
            Formats.unregisterJenaCSV();
        }
    }

    public static List<IRIMap> createMappings(Path dir) throws IOException {
        return loadDirectory(dir, Managers::createSoftManager, true);
    }

    public static List<IRIMap> loadDirectory(Path dir, Supplier<OntologyManager> factory, boolean continueIfError) throws IOException, OntApiException {
        List<IRI> files = IRIs.getFiles(dir);
        List<IRIMap> res = new ArrayList<>();
        while (true) {
            List<IRI> next = new ArrayList<>();
            IRIMap map = new IRIMap();
            OntologyManager manager = factory.get();
            files.forEach(doc -> {
                OntologyModel o;
                try {
                    o = loadOntology(manager, IRIs.toSource(doc, null));
                } catch (OWLOntologyAlreadyExistsException e) {
                    next.add(doc);
                    return;
                } catch (UnparsableOntologyException e) {
                    if (!continueIfError)
                        throw new OntApiException("Can't parse document " + doc, e);
                    LOGGER.error("Can't parse document {}", doc); // todo: reason?
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
