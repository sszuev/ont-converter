package com.github.sszuev.ontconverter

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig
import com.github.sszuev.ontconverter.utils.supportedReadFormats
import com.github.sszuev.ontconverter.utils.supportedWriteFormats
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

fun main(argsArray: Array<String>) {
    val args = parse(argsArray)
    println(args.printString())
}

private fun parse(args: Array<String>): Args {
    val locale = Locale.ENGLISH
    val parser = ArgParser("java -jar ont-converter.jar")
    val inputStringPath by parser.option(
        ArgType.String, shortName = "i", fullName = "input",
        description = """
            The file path or not-empty directory to load ontology/ontologies.
            """.trimIndent()
    ).required()
    val inputFormat by parser.option(
        ArgType.Choice(supportedReadFormats(), { e -> OntFormat.valueOf(e) }, { it.name }),
        shortName = "if", fullName = "input-format",
        description = """
            The input format. If not specified the program will choose 
            the most suitable one to load ontology from a file.
            """.trimIndent()
    )
    val outputStringPath by parser.option(
        ArgType.String, shortName = "o", fullName = "output",
        description = """
            The file or directory path to store result ontology/ontologies.
            If the --input is a file then this parameter must also be a file.
            """.trimIndent()
    ).required()
    val outputFormat by parser.option(
        ArgType.Choice(supportedWriteFormats(), { OntFormat.valueOf(it.uppercase(locale)) }, { it.name }),
        shortName = "of", fullName = "output-format",
        description = """
            The format of output ontology/ontologies.
            """.trimIndent()
    ).required()

    val punnings by parser.option(
        ArgType.Choice<OntModelConfig.StdMode>({ OntModelConfig.StdMode.valueOf(it.uppercase(locale)) }, { it.name }),
        shortName = "p", fullName = "punnings",
        description = """
            The punning mode. Could be used in conjunction with --refine option. Must be one of the following:
            Lax mode: allows any punnings, i.e. ontology is allowed to contain multiple entity declarations
            Middle mode: two forbidden intersections: Datatype <-> Class & NamedObjectProperty <-> DatatypeProperty
            Strict mode: all punnings are forbidden, i.e. Datatype <-> Class and rdf:Property intersections
            (any pairs of NamedObjectProperty, DatatypeProperty, AnnotationProperty).
            """.trimIndent()
    ).default(OntModelConfig.StdMode.LAX)

    val spin by parser.option(
        ArgType.Boolean, shortName = "s", fullName = "spin",
        description = """
            Use spin transformation to replace rdf:List based spin-constructs (e.g sp:Select)
            with their text-literal representation to produce compact axioms list.
            """.trimIndent()
    ).default(false)

    val refine by parser.option(
        ArgType.Boolean, shortName = "r", fullName = "refine",
        description = """
            Refine output: 
            if specified the resulting ontologies will consist only of the OWL2-DL components (annotations and axioms), 
            otherwise there could be some rdf-stuff (in case the output format is provided by jena)
            """.trimIndent()
    ).default(false)

    val web by parser.option(
        ArgType.Boolean, shortName = "w", fullName = "web",
        description = """
            Allow web/ftp diving to retrieve dependent ontologies from imports (owl:imports),
            otherwise the specified directory (see --input) will be used as the only source.
            """.trimIndent()
    ).default(false)

    val force by parser.option(
        ArgType.Boolean, shortName = "f", fullName = "force",
        description = "Ignore any exceptions while loading/saving and processing imports."
    ).default(false)

    val verbose by parser.option(
        ArgType.Boolean, shortName = "v", fullName = "verbose",
        description = "To print progress messages to console."
    ).default(false)

    parser.parse(args)

    val input = Paths.get(inputStringPath).toRealPath()
    val output = parseOutput(outputStringPath, input)

    return Args(input, inputFormat, output, outputFormat, punnings, spin, refine, web, force, verbose)
}

private fun parseOutput(outputStringPath: String, input: Path): Path {
    var out: Path = Paths.get(outputStringPath)
    if (out.parent != null) {
        require(Files.exists(out.parent)) { "Directory ${out.parent} does not exist." }
        out = out.parent.toRealPath().resolve(out.fileName)
    }
    if (Files.isDirectory(input) &&
        Files.walk(input).filter { Files.isRegularFile(it) }.skip(1).findFirst().isPresent
    ) {
        // if input is a directory with more than one file, then out should be a directory
        if (Files.exists(out)) {
            out = if (!Files.isDirectory(out)) {
                throw IllegalArgumentException("Output parameter is not a directory path: $out")
            } else {
                out.toRealPath()
            }
        } else {
            Files.createDirectory(out)
        }
    }
    return out
}

