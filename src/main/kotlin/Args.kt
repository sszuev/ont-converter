package com.github.sszuev.ontconverter

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig
import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.isDirectory
import kotlin.streams.toList

data class Args(
    val sourceFile: Path,
    val sourceFormat: OntFormat? = null,
    val sourceIsDirectory: Boolean,
    val targetFile: Path,
    val targetFormat: OntFormat,
    val targetIsDirectory: Boolean = sourceIsDirectory,
    val punnings: OntModelConfig.StdMode = OntModelConfig.StdMode.LAX,
    val refine: Boolean = false,
    val web: Boolean = false,
    val force: Boolean = false,
    val verbose: Boolean = false
) {

    val personality: OntPersonality
        get() = when (punnings) {
            OntModelConfig.StdMode.LAX -> OntModelConfig.ONT_PERSONALITY_LAX
            OntModelConfig.StdMode.MEDIUM -> OntModelConfig.ONT_PERSONALITY_MEDIUM
            OntModelConfig.StdMode.STRICT -> OntModelConfig.ONT_PERSONALITY_STRICT
        }

    fun printString(): String {
        return """
            |Arguments:
            |    input${(if (sourceIsDirectory) "Dir=" else "File=") + sourceFile}
            |    inputFormat=$sourceFormat
            |    output${(if (targetIsDirectory) "Dir=" else "File=") + targetFile}
            |    outputFormat=$targetFormat
            |    punnings=$punnings
            |    refine=$refine
            |    web=$web
            |    force=$force
            |    verbose=$verbose
            """.trimMargin()
    }
}

internal fun parseArgs(args: Array<String>): Args {
    val parser = ArgParser("java -jar ont-converter.jar")
    val inputStringPath by parser.option(
        ArgType.String, shortName = "i", fullName = "input",
        description = description(
            """
            The file path or not-empty directory to load ontology/ontologies.
            """
        )
    ).required()
    val sourceFormat by parser.option(
        ArgType.Choice(supportedReadFormats(), { formatByAlias(it) }, { formatInputString(it) }),
        shortName = "if", fullName = "input-format",
        description = description(
            """
            The input format. 
            If not specified the program will choose the most suitable one to load ontology from a file.
            """
        )
    )
    val outputStringPath by parser.option(
        ArgType.String, shortName = "o", fullName = "output",
        description = description(
            """
            The file or directory path to store result ontology/ontologies.
            If the --input is a file then this parameter must also be a file.
            """
        )
    ).required()
    val targetFormat by parser.option(
        ArgType.Choice(supportedWriteFormats(), { formatByAlias(it) }, { formatInputString(it) }),
        shortName = "of", fullName = "output-format",
        description = description(
            """
            The format of output ontology/ontologies.
            """
        )
    ).required()

    val punnings by parser.option(
        ArgType.Choice<OntModelConfig.StdMode>({ punningByAlias(it) }, { punningInputString(it) }),
        shortName = "p", fullName = "punnings",
        description = description(
            """
            The punning mode. 
            Could be used in conjunction with --refine option. Must be one of the following:
            Lax mode: allows any punnings, i.e. ontology is allowed to contain multiple entity declarations
            Middle mode: two forbidden intersections: Datatype <-> Class & NamedObjectProperty <-> DatatypeProperty
            Strict mode: all punnings are forbidden, i.e. Datatype <-> Class and rdf:Property intersections (any pairs of NamedObjectProperty, DatatypeProperty, AnnotationProperty).
            """
        )
    ).default(OntModelConfig.StdMode.LAX)

    val refine by parser.option(
        ArgType.Boolean, shortName = "r", fullName = "refine",
        description = description(
            """
            Refine output: 
            if specified the resulting ontologies will consist only of the OWL2-DL components (annotations and axioms), 
            otherwise there could be some rdf-stuff (in case the output format is provided by jena)
            """
        )
    ).default(false)

    val web by parser.option(
        ArgType.Boolean, shortName = "w", fullName = "web",
        description = description(
            """
            Allow web/ftp diving to retrieve dependent ontologies from imports (owl:imports),
            otherwise the specified directory (see --input) will be used as the only source.
            """
        )
    ).default(false)

    val force by parser.option(
        ArgType.Boolean, shortName = "f", fullName = "force",
        description = description("Ignore any exceptions while loading/saving and processing imports.")
    ).default(false)

    val verbose by parser.option(
        ArgType.Boolean, shortName = "v", fullName = "verbose",
        description = description("To print progress messages to console.")
    ).default(false)

    parser.parse(args)

    val source = parseInput(inputStringPath)
    val target = parseOutput(outputStringPath, source)

    return Args(
        source, sourceFormat, source.isDirectory(),
        target, targetFormat, target.isDirectory(),
        punnings, refine, web, force, verbose
    )
}

private fun parseInput(inputStringPath: String): Path {
    val source = Paths.get(inputStringPath).toRealPath()
    if (source.isDirectory() && Files.walk(source).filter { Files.isRegularFile(it) }.findFirst().isEmpty) {
        throw IllegalArgumentException("Directory $source contains no files.")
    }
    return source
}

private fun parseOutput(outputStringPath: String, source: Path): Path {
    var target: Path = Paths.get(outputStringPath)
    if (target.parent != null) {
        require(Files.exists(target.parent)) { "Directory ${target.parent} does not exist." }
        target = target.parent.toRealPath().resolve(target.fileName)
    }
    if (!source.isDirectory()) {
        return target
    }
    // if the input is a directory, then the output must also be a directory
    if (Files.exists(target)) {
        target = if (!Files.isDirectory(target)) {
            throw IllegalArgumentException("Output parameter is not a directory path: $target")
        } else {
            target.toRealPath()
        }
    } else {
        Files.createDirectory(target)
    }
    return target
}

private fun description(msg: String): String {
    return msg.trimIndent().replace("\n", "")
}

private fun supportedReadFormats(): List<OntFormat> {
    return OntFormat.formats().filter(OntFormat::isReadSupported).toList()
}

private fun supportedWriteFormats(): List<OntFormat> {
    return OntFormat.formats().filter(OntFormat::isWriteSupported).toList()
}

@Throws(NoSuchElementException::class)
private fun formatByAlias(key: String): OntFormat {
    return OntFormat.values().first { f -> formatAliases(f).any { key.equals(it, true) } }
}

private fun formatInputString(format: OntFormat): String {
    return formatAliases(format).joinToString(", ", "{", "}")
}

private fun formatAliases(f: OntFormat): Sequence<String> {
    return sequenceOf(f.ordinal.toString(), f.ext, f.name, f.id).distinctBy { it.lowercase(Locale.ENGLISH) }
}

@Throws(NoSuchElementException::class)
private fun punningByAlias(key: String): OntModelConfig.StdMode {
    return OntModelConfig.StdMode.values().first { m -> punningAliases(m).any { key.equals(it, true) } }
}

private fun punningInputString(mode: OntModelConfig.StdMode): String {
    return punningAliases(mode).joinToString(", ", "{", "}")
}

private fun punningAliases(m: OntModelConfig.StdMode): Sequence<String> {
    return sequenceOf(m.ordinal.toString(), m.name).distinctBy { it.lowercase(Locale.ENGLISH) }
}