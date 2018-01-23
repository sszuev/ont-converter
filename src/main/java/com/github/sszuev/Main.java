package com.github.sszuev;

import com.github.sszuev.ontapi.IRIMap;
import com.github.sszuev.utils.IRIs;
import com.github.sszuev.utils.Managers;
import org.apache.log4j.Level;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntologyManager;
import sun.misc.Unsafe;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by @szuev on 09.01.2018.
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String... inputs) throws Exception {
        forceDisableExternalLogging();
        Args args = null;
        try {
            args = Args.parse(inputs);
        } catch (Args.UsageException u) {
            System.out.println(u.getMessage());
            System.exit(u.code());
        }
        // configure logger:
        Level level = args.verbose() ? Level.DEBUG : Level.FATAL;
        org.apache.log4j.Logger.getRootLogger().setLevel(level);
        LOGGER.debug(args.asString());
        process(args);
    }

    public static void process(Args args) throws IOException, OntApiException {
        LOGGER.info("Start...");
        if (args.isInputDirectory()) {
            processDir(args);
        } else {
            processFile(args);
        }
        LOGGER.info("Done.");
    }

    private static void processFile(Args args) throws OntApiException {
        Path file = args.getInput();
        OntologyManager manager = Managers.createManager(args.web(), args.spin(), args.force());
        OWLOntologyDocumentSource source = IRIs.toSource(IRI.create(file.toUri()), args.getInputFormat());
        OWLOntologyID id = load(manager, source, args.force());
        if (id == null) return;
        Map<IRI, OWLOntologyID> map = new HashMap<>();
        map.put(source.getDocumentIRI(), id);
        save(manager, map, args);
    }

    private static void processDir(Args args) throws IOException, OntApiException {
        Path dir = args.getInput();
        List<IRIMap> maps;
        if (args.web()) {
            maps = Managers.loadDirectory(dir, args.getInputFormat(), () -> Managers.createManager(args.force(), args.spin()), args.force());
        } else {
            maps = Managers.createMappings(dir, args.getInputFormat());
        }
        for (IRIMap map : maps) {
            LOGGER.trace("Mapping: {}", map);
            OntologyManager manager;
            Map<IRI, OWLOntologyID> saveMap;
            if (args.web()) {
                saveMap = map.toMap();
                manager = map.manager().map(OntologyManager.class::cast).orElseThrow(() -> new OntApiException("No manager found"));
            } else {
                manager = Managers.createManager(map, args.force(), args.spin());
                saveMap = new HashMap<>();
                map.sources()
                        .forEach(source -> {
                            IRI src = IRIs.getDocumentIRI(source);
                            OWLOntologyID id = map.ontologyID(src).orElseThrow(IllegalStateException::new);
                            if (!manager.contains(id)) {
                                id = load(manager, source, args.force());
                            }
                            if (id != null) {
                                LOGGER.info("Ontology <{}> has been loaded.", toName(id, src));
                                saveMap.put(src, id);
                            }
                        });
            }
            save(manager, saveMap, args);
        }
    }

    private static OWLOntologyID load(OntologyManager manager, OWLOntologyDocumentSource source, boolean ignoreErrors) throws OntApiException {
        try {
            return Managers.loadOntology(manager, source).getOntologyID();
        } catch (OWLOntologyCreationException | OntApiException | UnloadableImportException e) {
            IRI iri = IRIs.getDocumentIRI(source);
            if (ignoreErrors) {
                LOGGER.error("Unable to load ontology from {}", iri);
                return null;
            }
            throw new OntApiException("Can't proceed ontology " + iri, e);
        }
    }

    private static void save(OntologyManager manager, Map<IRI, OWLOntologyID> map, Args args) {
        if (args.refine()) {
            LOGGER.info("Refine...");
            manager = Managers.copyManager(manager);
        }
        if (map.isEmpty()) { // wrong situation:
            if (args.force()) {
                LOGGER.error("Nothing to save");
                return;
            }
            throw new OntApiException("Nothing to save");
        }
        for (IRI src : map.keySet()) {
            OWLOntologyID id = map.get(src);
            IRI res = toResultFile(args, src);
            String name = toName(id, src);
            LOGGER.info(String.format("Save ontology <%s> as %s to <%s>", name, args.getOutputFormat(), res));
            OWLOntology o = Objects.requireNonNull(manager.getOntology(id), "Null ontology. id=" + name + ", file=" + src);
            try {
                OWLDocumentFormat in = o.getFormat();
                if (in == null) throw new IllegalStateException("No format for ont " + o);
                OWLDocumentFormat out = args.getOutputFormat().createOwlFormat();
                if (in instanceof PrefixDocumentFormat && out instanceof PrefixDocumentFormat) {
                    out.asPrefixOWLDocumentFormat().setPrefixManager(in.asPrefixOWLDocumentFormat());
                }
                manager.saveOntology(o, out, res);
            } catch (OWLOntologyStorageException e) {
                if (args.force()) {
                    LOGGER.error("Can't save " + name + " to " + res);
                } else {
                    throw new OntApiException("Can't save " + name + " to " + res, e);
                }
            }
        }
    }

    private static String toName(OWLOntologyID id, IRI doc) {
        return id.getOntologyIRI().orElse(doc).toString();
    }

    private static IRI toResultFile(Args args, IRI iri) {
        if (!args.isOutputDirectory()) {
            return IRI.create(args.getOutput().toUri());
        }
        return composeResultFilePath(args.getInput(), args.getOutput(), iri, args.getOutputFormat().getExt());
    }

    private static IRI composeResultFilePath(Path inputDirectory, Path outputDirectory, IRI inputFile, String extension) {
        Path src = Paths.get(inputFile.toURI());
        String fileName = src.getFileName() + "." + extension;
        Path res = outputDirectory
                .resolve(inputDirectory.relativize(Paths.get(src.getParent().toString() + outputDirectory.getFileSystem().getSeparator() + fileName)))
                .normalize();
        try {
            Files.createDirectories(res.getParent());
            res = res.getParent().toRealPath().resolve(res.getFileName());
        } catch (IOException e) {
            throw new UncheckedIOException("Can't create dir " + res.getParent(), e);
        }
        return IRI.create(res.toUri());
    }

    private static void forceDisableExternalLogging() {
        try {
            // java9:
            Class clazz = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = clazz.getDeclaredField("logger");
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);
            unsafe.putObjectVolatile(clazz, unsafe.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }
    }

    public static class SimpleTest { // todo: remove
        public static void main(String... args) throws Exception {
            String cmd = "-i ..\\..\\ont-api\\out -o out-0 -of 0 -v -f";
            Main.main(cmd.split("\\s+"));
        }
    }

    public static class SimpleTest2 { // todo: remove
        public static void main(String... args) throws Exception {
            String cmd = "-i ..\\..\\ont-api\\src\\test\\resources -o out-2 -of 1 -v -f";
            Main.main(cmd.split("\\s+"));
        }
    }

    public static class HelpPrint { // todo: remove
        public static void main(String... args) throws Exception {
            Main.main("-h");
        }

    }

}
