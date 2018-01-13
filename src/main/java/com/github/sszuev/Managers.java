package com.github.sszuev;

import com.github.sszuev.ontapi.IRIMap;
import com.github.sszuev.ontapi.NoWebStreamManager;
import com.github.sszuev.spin.SpinTransform;
import org.apache.jena.riot.system.stream.StreamManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.transforms.GraphTransformers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * A helper to work with {@link OntologyManager manager}s.
 *
 * Created by @szuev on 11.01.2018.
 */
public class Managers {

    public static OntologyManager createManager(Args args, Logs logs) throws IOException {
        OntologyManager manager = OntManagers.createONT();
        if (args.spin()) {
            GraphTransformers.Store transformers = manager.getOntologyConfigurator().getGraphTransformers();
            manager.getOntologyConfigurator().setGraphTransformers(transformers.addFirst(SpinTransform::new));
        }
        if (!args.web()) {
            manager.getOntologyConfigurator().setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE));
            IRIMap map = args.isInputDirectory() ? createMappers(args.getInput(), logs) : new IRIMap();
            StreamManager.setGlobal(new NoWebStreamManager(map));
            if (!map.isEmpty())
                manager.getIRIMappers().add(map);
        }
        return manager;
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

    public static IRIMap createMappers(Path dir, Logs logs) throws IOException {
        OntologyManager manager = OntManagers.createONT();
        manager.getOntologyConfigurator()
                .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)
                .setPerformTransformation(false)
                .setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE));
        return loadDirectory(manager, dir, logs.toLevel(Logs.Level.INFO), true, false, false);
    }

    public static IRIMap loadDirectory(OntologyManager manager, Args args, Logs logs) throws IOException {
        return loadDirectory(manager, args.getInput(), logs, true, !args.force(), false);
    }

    public static IRIMap loadDirectory(OntologyManager manager,
                                       Path dir,
                                       Logs logs,
                                       boolean throwIOError,
                                       boolean throwLoadError,
                                       boolean throwDuplicateError
    ) throws IOException, OntApiException {
        IRIMap map = new IRIMap();
        OntApiException duplicate = new OntApiException("Duplicate ontologies inside dir " + dir + ":");
        OntApiException load = new OntApiException("Errors while creating mapping for dir " + dir + ":");
        IOException io = new IOException("Unexpected i/o errors while processing dir " + dir + ":");
        walk(dir, io).forEach(doc -> {
            OWLOntologyID id;
            try {
                id = manager.loadOntologyFromOntologyDocument(doc).getOntologyID();
            } catch (OWLOntologyAlreadyExistsException e) {
                duplicate.addSuppressed(e);
                return;
            } catch (OWLOntologyCreationException | OntApiException | UnloadableImportException e) {
                // TODO: handle UnparsableOntologyEx
                if ("tt.txt".equals(Paths.get(doc.toURI()).getFileName().toString())) {
                    System.err.println("ER" + doc);
                }
                String msg = "Can't load " + doc + ": " + Exceptions.shortMessage(e, 20);
                logs.error(msg);
                load.addSuppressed(Exceptions.wrap(msg, e));
                return;
            }
            logs.debug("The ontology " + doc + " is loaded.");
            map.add(id, doc);
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

    private static Stream<IRI> walk(Path dir, IOException holder) throws IOException {
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

    public static Map<OWLOntology, IRI> getOntologies(OntologyManager manager, Map<OWLOntologyID, IRI> map, Args args) {
        Map<OWLOntology, IRI> res = new HashMap<>();
        map.entrySet().stream().filter(e -> !e.getKey().isAnonymous()).forEach(e -> {
            OWLOntology o = manager.getOntology(e.getKey());
            if (o == null) return;
            IRI dst = args.toResultFile(e.getValue());
            res.put(o, dst);
        });
        manager.ontologies().filter(IsAnonymous::isAnonymous).forEach(o -> {
            res.put(o, args.toResultFile(manager.getOntologyDocumentIRI(o)));
        });
        return res;
    }

}
