package com.github.sszuev;

import com.github.sszuev.ontapi.IRIMap;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import sun.misc.Unsafe;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Created by @szuev on 09.01.2018.
 */
public class Main {

    public static void main(String... input) throws Exception {
        forceDisableAnyExternalLogging();
        Args args = null;
        try {
            args = Args.parse(input);
        } catch (Args.UsageException u) {
            System.out.println(u.getMessage());
            System.exit(u.code());
        }
        if (args.verbose()) {
            System.out.println(args.print());
        }
        try {
            process(args, System.out);
        } catch (Exception e) {
            if (e.getSuppressed().length != 0) {
                System.err.println(Exceptions.flatSuppressedMessage(e));
                System.exit(-3);
            } else {
                throw e;
            }
        }
    }

    public static void process(Args args, PrintStream logs) throws IOException, OntApiException {
        if (args.verbose()) {
            logs.println("Start ...");
        }
        // prepare and create manager:
        OntologyManager manager = Managers.createManager(args);
        IRIMap map = null;
        if (manager.getIRIMappers().size() == 1) {
            map = (IRIMap) manager.getIRIMappers().iterator().next();
        }
        // load:
        if (args.verbose()) {
            logs.println((args.isInputDirectory() ? "Load ontologies from directory <" : "Load ontology from file <") + args.getInput() + ">.");
        }
        IRIMap _map = Managers.loadDirectory(manager, args, logs);
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
        Map<OWLOntology, IRI> res = Managers.getOntologies(manager, map.toMap(), args);
        if (res.isEmpty()) {
            throw new OntApiException("Nothing to save");
        }
        for (OWLOntology o : res.keySet()) {
            IRI doc = res.get(o);
            if (args.verbose()) {
                logs.printf("Save ontology <%s> as %s to %s.%n", o.getOntologyID().getOntologyIRI().map(IRI::toString).orElse("anonymous"), args.getOntFormat(), doc);
            }
            try {
                manager.saveOntology(o, args.getOntFormat().createOwlFormat(), doc);
            } catch (OWLOntologyStorageException e) {
                if (args.force()) {
                    if (args.verbose()) {
                        logs.println("\tCan't save " + o + " to " + doc);
                    }
                } else {
                    throw new OntApiException("Can't save " + o + " to " + doc, e);
                }
            }
        }
        if (args.verbose()) {
            logs.println("Done.");
        }
    }

    private static void forceDisableAnyExternalLogging() {
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);

            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }
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
