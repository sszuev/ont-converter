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
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

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

    public static OntologyManager createManager(Args args) throws IOException {
        OntologyManager manager = OntManagers.createONT();
        if (args.spin()) {
            GraphTransformers.Store transformers = manager.getOntologyConfigurator().getGraphTransformers();
            manager.getOntologyConfigurator().setGraphTransformers(transformers.addFirst(SpinTransform::new));
        }
        if (!args.web()) {
            manager.getOntologyConfigurator().setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE));
            IRIMap map = args.isInputDirectory() ? createMappers(args.getInput()) : new IRIMap();
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

    public static IRIMap createMappers(Path dir) throws IOException {
        OntologyManager manager = OntManagers.createONT();
        manager.getOntologyConfigurator()
                .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)
                .setPerformTransformation(false)
                .setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE));
        return loadDirectory(manager, dir, true, false, false, null);
    }

    public static IRIMap loadDirectory(OntologyManager manager, Args args, PrintStream logs) throws IOException {
        return loadDirectory(manager, args.getInput(), true, !args.force(), false, args.verbose() ? logs : null);
    }

    public static IRIMap loadDirectory(OntologyManager manager, Path dir, PrintStream logs) throws IOException {
        return loadDirectory(manager, dir, true, true, false, logs);
    }

    public static IRIMap loadDirectory(OntologyManager manager,
                                       Path dir,
                                       boolean throwIOError,
                                       boolean throwLoadError,
                                       boolean throwDuplicateError,
                                       PrintStream out) throws IOException, OntApiException {
        IRIMap map = new IRIMap();
        OntApiException duplicate = new OntApiException("Duplicate ontologies inside dir " + dir + ":");
        OntApiException load = new OntApiException("Errors while creating mapping for dir " + dir + ":");
        IOException io = new IOException("Unexpected i/o errors while processing dir " + dir + ":");
        walk(dir, io).forEach(docIRI -> {
            OWLOntologyID id;
            try {
                id = manager.loadOntologyFromOntologyDocument(docIRI).getOntologyID();
            } catch (OWLOntologyAlreadyExistsException e) {
                duplicate.addSuppressed(e);
                return;
            } catch (OWLOntologyCreationException | OntApiException | UnloadableImportException e) {
                if (!throwLoadError && out != null) {
                    out.println("\tCan't load " + docIRI + ": " + Exceptions.shortMessage(e, 100));
                }
                load.addSuppressed(Exceptions.wrap("Can't load " + docIRI, e));
                return;
            }
            if (out != null) {
                out.println("The ontology " + docIRI + " is loaded.");
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

    private static Stream<IRI> walk(Path dir, IOException holder) throws IOException {
        return Files.walk(dir)
                .sorted(Comparator.comparingInt(Path::getNameCount).reversed())
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
