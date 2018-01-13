package com.github.sszuev;

import com.github.sszuev.ontapi.IRIMap;
import com.github.sszuev.ontapi.Managers;
import com.github.sszuev.ontapi.NoWebStreamManager;
import com.github.sszuev.spin.SpinTransform;
import org.apache.jena.riot.system.stream.StreamManager;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.transforms.GraphTransformers;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by @szuev on 09.01.2018.
 */
public class Main {

    public static void main(String... input) throws Exception {
        Args args = null;
        try {
            args = Args.parse(input);
        } catch (Args.UsageException u) {
            System.out.println(u.getMessage());
            System.exit(u.code());
        }
        if (args.verbose()) {
            System.out.println(args);
        }
        process(args, System.out);
    }

    public static void process(Args args, PrintStream logs) throws IOException, OntApiException {
        if (args.verbose()) {
            logs.println("Go");
        }
        // prepare and create manager:
        OntologyManager manager = createManager(args);
        IRIMap map = null;
        if (manager.getIRIMappers().size() == 1) {
            map = (IRIMap) manager.getIRIMappers().iterator().next();
        }
        // load:
        if (args.verbose()) {
            logs.println((args.isInputDirectory() ? "Load ontologies from directory <" : "Load ontology from file <") + args.getInput() + ">.");
        }
        IRIMap _map = Managers.loadDirectory(manager, args.getInput(), args.verbose() ? logs : null);
        if (map == null) map = _map;
        if (args.verbose()) {
            logs.printf("Mapping: %s%n", map);
        }
        // refine:
        if (args.refine()) {
            if (args.verbose()) {
                logs.print("Refine ... ");
            }
            manager = Managers.copyManager(manager);
            if (args.verbose()) {
                logs.println("done.");
            }
        }

        // save:
        Map<OWLOntology, IRI> res = getOntologyMapToSave(manager, map.toMap(), args);
        if (res.isEmpty()) {
            throw new OntApiException("Nothing to save");
        }
        for (OWLOntology o : res.keySet()) {
            IRI doc = res.get(o);
            if (args.verbose()) {
                logs.println("Save ontology <" + o.getOntologyID().getOntologyIRI().map(IRI::toString).orElse("anonymous") + "> to " + doc);
            }
            try {
                manager.saveOntology(o, args.getOntFormat().createOwlFormat(), doc);
            } catch (OWLOntologyStorageException e) {
                throw new OntApiException("Can't save " + o + " to " + doc, e);
            }
        }
        if (args.verbose()) {
            logs.println("Done.");
        }
    }

    private static Map<OWLOntology, IRI> getOntologyMapToSave(OntologyManager manager, Map<OWLOntologyID, IRI> map, Args args) {
        Map<OWLOntology, IRI> res = new HashMap<>();
        map.entrySet().stream().filter(e -> !e.getKey().isAnonymous()).forEach(e -> {
            OWLOntology o = Objects.requireNonNull(manager.getOntology(e.getKey()), "Can't find ontology " + e.getKey());
            IRI dst = args.toResultFile(e.getValue());
            res.put(o, dst);
        });
        manager.ontologies().filter(IsAnonymous::isAnonymous).forEach(o -> {
            res.put(o, args.toResultFile(manager.getOntologyDocumentIRI(o)));
        });
        return res;
    }

    public static OntologyManager createManager(Args args) throws IOException {
        OntologyManager manager = OntManagers.createONT();
        if (args.spin()) {
            GraphTransformers.Store transformers = manager.getOntologyConfigurator().getGraphTransformers();
            manager.getOntologyConfigurator().setGraphTransformers(transformers.addFirst(SpinTransform::new));
        }
        if (!args.webAccess()) {
            manager.getOntologyConfigurator().setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE));
            IRIMap map = args.isInputDirectory() ? Managers.createMappers(args.getInput()) : new IRIMap();
            StreamManager.setGlobal(new NoWebStreamManager(map));
            if (!map.isEmpty())
                manager.getIRIMappers().add(map);
        }
        return manager;
    }

    public static class SpinTest {
        public static void main(String... a) throws Exception {
            String cmd = "-input ..\\..\\ont-api\\src\\test\\resources\\etc -output ..\\..\\ont-api\\out\\etc -s -v -f 12";
            Args args = Args.parse(cmd.split("\\s+"));
            Main.process(args, System.out);
        }
    }


    public static class DirTest {
        public static void main(String... a) throws IOException {
            Path dir = Paths.get("..\\..\\ont-api\\src\\test\\resources\\");
            IRIMap res = Managers.createMappers(dir);
            System.out.println(res);
            System.out.println(res.toMap().size());
        }
    }

    public static class FileTest {
        public static void main(String... a) throws Exception {
            String cmd = "-input ..\\..\\ont-api\\src\\test\\resources\\pizza.ttl -output ..\\..\\ont-api\\out\\etc.obo -v -f 16";
            Main.main(cmd.split("\\s+"));
        }
    }

    public static class WalkTest {
        public static void main(String... a) throws IOException {
            String path = "..\\..\\ont-api\\src\\test\\resources\\pizza.ttl";
            Path file = Paths.get(path).toRealPath();
            Files.walk(file).forEach(System.out::println);
        }
    }

    public static class OBOTest {
        public static void main(String... a) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
            IRI iri = IRI.create(Paths.get("..\\..\\ont-api\\src\\test\\resources\\pizza.ttl").toRealPath().toUri());
            IRI out = IRI.create(Paths.get("..\\..\\ont-api\\out\\").toRealPath().resolve("etc.xxx").toUri());
            System.out.println(out);
            OWLOntologyManager m =

                    OntManagers.createONT();
            OWLOntology o = m.loadOntology(iri);
            m.saveOntology(o, OntFormat.OBO.createOwlFormat(), out);
        }
    }
}
