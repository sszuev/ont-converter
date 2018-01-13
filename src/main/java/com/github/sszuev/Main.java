package com.github.sszuev;

import com.github.sszuev.ontapi.IRIMap;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntologyManager;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
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
        Logs logs = args.createLogger(System.out);
        logs.debug(args.asString());
        try {
            process(args, logs);
        } catch (OntApiException e) {
            if (e.getSuppressed().length != 0) {
                logs.error(Exceptions.flatSuppressedMessage(e));
                System.exit(-3);
            }
            throw e;
        }
    }

    public static void process(Args args, Logs logs) throws IOException, OntApiException {
        logs.info("Start ...");
        // prepare and create manager:
        OntologyManager manager = Managers.createManager(args, logs);
        IRIMap map = null;
        if (manager.getIRIMappers().size() == 1) {
            map = (IRIMap) manager.getIRIMappers().iterator().next();
        }
        logs.info((args.isInputDirectory() ? "Load ontologies from directory <" : "Load ontology from file <") + args.getInput() + ">.");
        IRIMap _map = Managers.loadDirectory(manager, args, logs);
        if (map == null) map = _map;
        logs.trace("Mappings: " + map);
        // refine:
        if (args.refine()) {
            logs.info("Refine...");
            manager = Managers.copyManager(manager);
            logs.debug("done.");
        }

        // save:
        Map<OWLOntology, IRI> res = Managers.getOntologies(manager, map.toMap(), args);
        if (res.isEmpty()) {
            throw new OntApiException("Nothing to save");
        }
        for (OWLOntology o : res.keySet()) {
            IRI doc = res.get(o);
            logs.info(String.format("Save ontology <%s> as %s to %s.",
                    o.getOntologyID().getOntologyIRI().map(IRI::toString).orElse("anonymous"),
                    args.getOntFormat(),
                    doc));
            try {
                manager.saveOntology(o, args.getOntFormat().createOwlFormat(), doc);
            } catch (OWLOntologyStorageException e) {
                if (args.force()) {
                    logs.info("\tCan't save " + o + " to " + doc);
                } else {
                    throw new OntApiException("Can't save " + o + " to " + doc, e);
                }
            }
        }
        logs.info("Done.");
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

    public static class Test1 {
        public static void main(String... args) throws Exception {
            String cmd = "-i ..\\..\\ont-api\\out -o logs -f 0 -v -e";
            Main.main(cmd.split("\\s+"));
        }
    }

}
