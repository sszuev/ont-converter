package com.github.sszuev;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.sszuev.utils.Formats;
import ru.avicomp.ontapi.OntFormat;

/**
 * Container for programs input.
 * <p>
 * Created by @szuev on 12.01.2018.
 */
public class Args {
    private static final String JAR_NAME = "ont-converter.jar";
    private final Path input, output;
    private final OntFormat outFormat, inFormat;
    private final boolean spin, force, refine, verbose, webAccess;

    private boolean outDir, inDir;

    private Args(Path input, Path output, OntFormat outFormat, OntFormat inFormat, boolean spin, boolean force, boolean clear, boolean verbose, boolean webAccess) {
        this.input = input;
        this.output = output;
        this.outFormat = outFormat;
        this.inFormat = inFormat;
        this.spin = spin;
        this.force = force;
        this.refine = clear;
        this.verbose = verbose;
        this.webAccess = webAccess;
        this.outDir = Files.isDirectory(output);
        this.inDir = Files.isDirectory(input);
    }

    public static Args parse(String... args) throws IOException, IllegalArgumentException {
        Options opts = new Options();
        Arrays.stream(Opts.values()).forEach(o -> opts.addOption(o.build()));
        if (Stream.of("-h", "--h", "-help", "--help", "/?").anyMatch(h -> ArrayUtils.contains(args, h))) {
            throw new UsageException(help(opts, true), 0);
        }
        CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(opts, args);
        } catch (ParseException e) {
            throw new UsageException(e.getLocalizedMessage() + "\n" + help(opts, false), -1);
        }
        // parse
        OntFormat outputFormat = Formats.find(cmd.getOptionValue("output-format"));
        if (!outputFormat.isWriteSupported()) {
            throw new IllegalArgumentException(outputFormat + " is not suitable for writing.");
        }
        OntFormat inputFormat = null;
        if (cmd.hasOption("input-format")) {
            inputFormat = Formats.find(cmd.getOptionValue("input-format"));
            if (!inputFormat.isReadSupported()) {
                throw new IllegalArgumentException(inputFormat + " is not suitable for reading.");
            }
        }
        Path in = Paths.get(cmd.getOptionValue("input")).toRealPath();
        Path out = Paths.get(cmd.getOptionValue("output"));

        if (out.getParent() != null) {
            if (!Files.exists(out.getParent())) {
                throw new IllegalArgumentException("Directory " + out.getParent() + " does not exist.");
            }
            out = out.getParent().toRealPath().resolve(out.getFileName());
        }

        if (Files.isDirectory(in) && Files.walk(in).filter(f -> Files.isRegularFile(f)).count() > 1) {
            // out should be directory
            if (Files.exists(out)) {
                if (!Files.isDirectory(out)) {
                    throw new IllegalArgumentException("Output parameter is not a directory path: " + out);
                } else {
                    out = out.toRealPath();
                }
            } else {
                Files.createDirectory(out);
            }
        }
        return new Args(in, out, outputFormat, inputFormat, cmd.hasOption("s"), cmd.hasOption("force"), cmd.hasOption("r"), cmd.hasOption("v"), cmd.hasOption("w"));
    }

    private static String help(Options opts, boolean whole) {
        StringBuilder sb = new StringBuilder();
        if (whole) {
            sb.append("A simple command-line utility to convert any rdf graph to OWL2-DL ontology.").append("\n");
        }
        StringWriter sw = new StringWriter();
        new HelpFormatter().printHelp(new PrintWriter(sw), 74, "java -jar " + JAR_NAME, "options:", opts, 1, 3, null, true);
        sb.append(sw);
        if (whole) {
            sb.append("formats aliases (case insensitive):").append("\n");
            OntFormat.formats()
                    .filter(f -> f.isReadSupported() || f.isWriteSupported())
                    .map(f -> Formats.aliases(f).stream().collect(Collectors.joining("|", " " + StringUtils.rightPad(f.name(), 20) + "\t", "")))
                    .forEach(x -> sb.append(x).append("\n"));
        }
        return sb.toString();
    }

    public boolean verbose() {
        return verbose;
    }

    public boolean refine() {
        return refine;
    }

    public boolean spin() {
        return spin;
    }

    public boolean force() {
        return force;
    }

    public boolean web() {
        return webAccess;
    }

    public boolean isInputDirectory() {
        return inDir;
    }

    public Path getInput() {
        return input;
    }

    public boolean isOutputDirectory() {
        return outDir;
    }

    public Path getOutput() {
        return output;
    }

    public OntFormat getOutFormat() {
        return outFormat;
    }

    public OntFormat getInFormat() {
        return inFormat;
    }

    public String asString() {
        return String.format("Arguments:%n" +
                        "\tinput-%s=%s%n" +
                        "\toutput-%s=%s%n" +
                        "\toutput-format=%s%n" +
                        "\tinput-format=%s%n" +
                        "\tverbose=%s%n" +
                        "\tforce=%s%n" +
                        "\tweb-access=%s%n" +
                        "\trefine=%s%n" +
                        "\tspin=%s%n",
                inDir ? "dir" : "file", input,
                outDir ? "dir" : "file", output,
                outFormat,
                Optional.ofNullable(inFormat).map(Enum::toString).orElse("ANY"),
                verbose, force, webAccess, refine, spin);
    }

    public static class UsageException extends IllegalArgumentException {
        private final int code;

        UsageException(String s, int code) {
            super(s);
            this.code = code;
        }

        public int code() {
            return code;
        }
    }


    public enum Opts { // todo: add punnings parameter
        HELP("h", "help", "Print usage."),
        VERBOSE("v", "verbose", "To print progress info to console."),
        WEB("w", "web", "Allow web/ftp diving to retrieve dependent ontologies from owl:imports, " +
                "otherwise the only specified files will be used as the source."),
        FORCE("f", "force", "Ignore exceptions while loading/saving and keep processing ontologies with missed imports"),
        SPIN("s", "spin", "Use spin transformation to replace rdf:List based spin-constructs (e.g sp:Select) with their " +
                "text-literal representation to produce compact axioms list"),
        REFINE("r", "refine", "Refine output: the resulting ontologies will consist only of the OWL2-DL components."),

        INPUT_FORMAT("if", "input-format", "The input format. If not specified the program will choose the most suitable " +
                "one to load ontology from file.\nMust be one of the following:\n" +
                OntFormat.formats().filter(OntFormat::isReadSupported).map(Enum::name).collect(Collectors.joining(", ")) + "\n" +
                "- Optional.", "format"),
        OUTPUT_FORMAT("of", "output-format", "The format of output ontology/ontologies.\n" +
                "Must be one of the following:\n" +
                OntFormat.formats().filter(OntFormat::isWriteSupported).map(Enum::name).collect(Collectors.joining(", ")) + "\n" +
                "- Required.", "format", true),

        INPUT("i", "input", "Ontology file or directory with files to read.\nSee --input-format for list of supported output syntaxes.\n" +
                "- Required.", "path", true),
        OUTPUT("o", "output", "Ontology file or directory containing ontologies to write.\n" +
                "If the --input is a file then this option parameter must also be a file.\n" +
                "- Required.", "path", true),;

        private final String name;
        private final String longName;
        private final String description;
        private final String argType;
        private final boolean required;

        Opts(String name, String longName, String description) {
            this(name, longName, description, null);
        }

        Opts(String name, String longName, String description, String argType) {
            this(name, longName, description, argType, false);
        }

        Opts(String name, String longName, String description, String argType, boolean required) {
            this.name = name;
            this.longName = longName;
            this.description = description;
            this.argType = argType;
            this.required = required;
        }

        public Option build() {
            Option.Builder res = Option.builder(name).longOpt(longName).desc(description);
            if (required) {
                res.required();
            }
            if (argType != null) {
                res.hasArgs().argName(argType);
            }
            return res.build();
        }
    }
}