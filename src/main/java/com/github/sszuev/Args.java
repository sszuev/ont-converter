package com.github.sszuev;

import com.github.sszuev.utils.Formats;
import org.apache.commons.cli.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;

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

/**
 * Parsing programs input.
 * <p>
 * Created by @szuev on 12.01.2018.
 */
public class Args {
    private static final String JAR_NAME = "ont-converter.jar";
    private static final int NAME_COL_LENGTH = 19;
    private static final int PROVIDER_COL_LENGTH = 13;
    private static final int READ_WRITE_COL_LENGTH = 13;
    private final Path input, output;
    private final OntFormat outFormat, inFormat;
    private final OntModelConfig.StdMode personality;
    private final boolean spin, force, refine, verbose, webAccess;
    private boolean outDir, inDir;

    private Args(Path input,
                 Path output,
                 OntFormat outFormat,
                 OntFormat inFormat,
                 OntModelConfig.StdMode personality,
                 boolean spin,
                 boolean force,
                 boolean clear,
                 boolean verbose,
                 boolean webAccess) {
        this.input = input;
        this.output = output;
        this.outFormat = outFormat;
        this.inFormat = inFormat;
        this.personality = personality;
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
        OntFormat outputFormat = parseOutputFormat(cmd);
        OntFormat inputFormat = parseInputFormat(cmd);
        OntModelConfig.StdMode mode = parsePersonalities(cmd);

        Path in = Paths.get(cmd.getOptionValue(Opts.INPUT.longName)).toRealPath();
        Path out = Paths.get(cmd.getOptionValue(Opts.OUTPUT.longName));
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
        return new Args(in, out, outputFormat, inputFormat, mode,
                cmd.hasOption("s"), cmd.hasOption("force"), cmd.hasOption("r"), cmd.hasOption("v"), cmd.hasOption("w"));
    }

    private static OntFormat parseInputFormat(CommandLine cmd) {
        if (!cmd.hasOption(Opts.INPUT_FORMAT.longName)) return null;
        OntFormat res = Formats.find(cmd.getOptionValue(Opts.INPUT_FORMAT.longName));
        if (!res.isReadSupported()) {
            throw new IllegalArgumentException(res + " is not suitable for reading.");
        }
        return res;
    }

    private static OntFormat parseOutputFormat(CommandLine cmd) {
        OntFormat res = Formats.find(cmd.getOptionValue(Opts.OUTPUT_FORMAT.longName));
        if (!res.isWriteSupported()) {
            throw new IllegalArgumentException(res + " is not suitable for writing.");
        }
        return res;
    }

    private static OntModelConfig.StdMode parsePersonalities(CommandLine cmd) {
        if (!cmd.hasOption(Opts.PUNNING.longName)) {
            return OntModelConfig.StdMode.LAX;
        }
        String val = cmd.getOptionValue(Opts.PUNNING.longName);
        OntModelConfig.StdMode[] values = OntModelConfig.StdMode.values();
        try {
            return values[values.length - Integer.parseInt(val)];
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Wrong --" + Opts.PUNNING.longName + ":" + val, e);
        }
    }

    private static String help(Options opts, boolean usage) {
        StringBuilder sb = new StringBuilder();
        if (usage) {
            sb.append("A simple command-line utility to convert any rdf graph to OWL2-DL ontology.").append("\n");
        }
        StringWriter sw = new StringWriter();
        new HelpFormatter().printHelp(new PrintWriter(sw), 74, "java -jar " + JAR_NAME,
                "options:", opts, 1, 3, null, true);
        sb.append(sw);
        if (usage) {
            sb.append("Full list of supported formats:").append("\n");
            sb.append(" ").append(formatHeader()).append("\n");
            try {
                Formats.registerJenaCSV();
                OntFormat.formats()
                        .filter(f -> f.isReadSupported() || f.isWriteSupported())
                        .map(Args::formatLine)
                        .forEach(x -> sb.append(" ").append(x).append("\n"));
            } finally {
                Formats.unregisterJenaCSV();
            }
        }
        return sb.toString();
    }

    private static String formatLine(OntFormat f) {
        return StringUtils.rightPad(f.name(), NAME_COL_LENGTH) +
                StringUtils.rightPad(f.isJena() ? "Apache Jena" : "OWL-API", PROVIDER_COL_LENGTH) +
                StringUtils.rightPad(f.isReadSupported() + "/" + f.isWriteSupported(), READ_WRITE_COL_LENGTH) +
                Formats.aliases(f).stream().collect(Collectors.joining(", "));

    }

    private static String formatHeader() {
        return StringUtils.rightPad("Name:", NAME_COL_LENGTH) +
                StringUtils.rightPad("Provider:", PROVIDER_COL_LENGTH) +
                StringUtils.rightPad("Read/Write:", READ_WRITE_COL_LENGTH) +
                "Aliases (case insensitive):";
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

    public OntFormat getOutputFormat() {
        return outFormat;
    }

    public OntFormat getInputFormat() {
        return inFormat;
    }

    public OntPersonality getPersonality() {
        switch (personality) {
            case LAX:
                return OntModelConfig.ONT_PERSONALITY_LAX;
            case MEDIUM:
                return OntModelConfig.ONT_PERSONALITY_MEDIUM;
            case STRICT:
                return OntModelConfig.ONT_PERSONALITY_STRICT;
            default:
                throw new IllegalStateException();
        }
    }

    public String asString() {
        return String.format("Arguments:%n" +
                        "\tinput-%s=%s%n" +
                        "\toutput-%s=%s%n" +
                        "\toutput-format=%s%n" +
                        "\tinput-format=%s%n" +
                        "\tpunnings-mode=%s%n" +
                        "\tverbose=%s%n" +
                        "\tforce=%s%n" +
                        "\tweb-access=%s%n" +
                        "\trefine=%s%n" +
                        "\tspin=%s%n",
                inDir ? "dir" : "file", input,
                outDir ? "dir" : "file", output,
                outFormat,
                Optional.ofNullable(inFormat).map(Enum::toString).orElse("ANY"),
                personality,
                verbose, force, webAccess, refine, spin);
    }

    public enum Opts {
        HELP("h", "help", "Print usage."),
        VERBOSE("v", "verbose", "To print progress messages to console."),
        WEB("w", "web", "Allow web/ftp diving to retrieve dependent ontologies from imports (owl:imports), " +
                "otherwise the specified directory (see --input) will be used as the only source."),
        FORCE("f", "force", "Ignore any exceptions while loading/saving and processing imports"),

        SPIN("s", "spin", "Use spin transformation to replace rdf:List based spin-constructs (e.g sp:Select) with their " +
                "text-literal representation to produce compact axioms list.\n" +
                "- Optional."),

        REFINE("r", "refine", "Refine output: if specified the resulting ontologies will consist only of the OWL2-DL components (annotations and axioms), " +
                "otherwise there could be some rdf-stuff (in case the output format is provided by jena)\n" +
                "- Optional. Experimental"),
        PUNNING("p", "punnings", "The punning mode. Could be used in conjunction with --refine option. Must be one of the following:\n" +
                "0 - Lax mode. Default. Allow any punnings, i.e. ontology is allowed to contain multiple entity declarations\n" +
                "1 - Middle mode. Two forbidden intersections: Datatype <-> Class & NamedObjectProperty <-> DatatypeProperty\n" +
                "2 - Strict mode: All punnings are forbidden, i.e. Datatype <-> Class and rdf:Property intersections " +
                "(any pairs of NamedObjectProperty, DatatypeProperty, AnnotationProperty).\n" +
                "- Optional. Experimental",
                "0|1|2"),

        INPUT_FORMAT("if", "input-format", "The input format. If not specified the program will choose the most suitable " +
                "one to load ontology from a file.\nMust be one of the following:\n" +
                OntFormat.formats().filter(OntFormat::isReadSupported).map(Enum::name).collect(Collectors.joining(", ")) + "\n" +
                "- Optional.", "format"),
        OUTPUT_FORMAT("of", "output-format", "The format of output ontology/ontologies.\n" +
                "Must be one of the following:\n" +
                OntFormat.formats().filter(OntFormat::isWriteSupported).map(Enum::name).collect(Collectors.joining(", ")) + "\n" +
                "- Required.", "format", true),

        INPUT("i", "input", "The file path or not-empty directory to load ontology/ontologies.\n" +
                "See --input-format for list of supported syntaxes.\n" +
                "- Required.", "path", true),
        OUTPUT("o", "output", "The file or directory path to store result ontology/ontologies.\n" +
                "If the --input is a file then this parameter must also be a file.\n" +
                "See --output-format for list of supported syntaxes.\n" +
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
}