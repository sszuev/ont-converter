package com.github.sszuev.ontconverter

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntologyManager
import com.github.sszuev.ontconverter.utils.*
import org.semanticweb.owlapi.formats.PrefixDocumentFormat
import org.semanticweb.owlapi.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val logger: Logger = LoggerFactory.getLogger(Processor::class.java)

/**
 * A core of the tool: a facility to control transformation.
 */
class Processor(private val args: Args) {

    fun run() {
        logger.info("Start...")
        if (args.sourceIsDirectory) {
            TODO("Directory ")
        } else {
            handleSingleFile()
        }
        logger.info("Done.")
    }

    private fun handleSingleFile() {
        val m = createManager(args.personality, force = args.force, spin = args.spin)
        val s = createSource(IRI.create(args.sourceFile.toUri()), args.sourceFormat)
        val id = loadSource(m, s, args.force) ?: return
        save(m, mapOf(s.documentIRI to id))
    }

    /**
     * Saves ontologies to the target.
     */
    @Throws(OntApiException::class)
    internal fun save(manager: OntologyManager, map: Map<IRI, OWLOntologyID>) {
        if (map.isEmpty()) {
            if (args.force) {
                logger.error("Nothing to save")
                return
            }
            throw OntApiException("Noting to save")
        }

        logger.debug(
            "Number of ontologies in the manager: ${
                manager.ontologies().count()
            }, number of ontologies to save: ${map.size}"
        )
        val man = if (args.refine) {
            logger.info("Refine...")
            createCopyManager(source = manager, ignoreExceptions = args.force)
        } else {
            manager
        }
        for (src in map.keys) {
            val id = map[src]!!
            val file: IRI = toResultFile(src)
            val name: IRI = getNameIRI(id) { src }
            val ont: OWLOntology =
                requireNotNull(man.getOntology(id)) { "The ontology not found. id=$name, file=$src." }
            logger.info("Save ontology $name as ${args.targetFormat} to ${file}.")
            try {
                val internalFormat = ont.format ?: throw IllegalStateException("No format for ont $name")
                val targetFormat: OWLDocumentFormat = args.targetFormat.createOwlFormat()
                if (internalFormat is PrefixDocumentFormat && targetFormat is PrefixDocumentFormat) {
                    targetFormat.asPrefixOWLDocumentFormat()
                        .setPrefixManager(internalFormat.asPrefixOWLDocumentFormat())
                }
                man.saveOntology(ont, targetFormat, file)
            } catch (e: OWLOntologyStorageException) {
                if (args.force) {
                    logger.error("Can't save $name to $file")
                } else {
                    throw OntApiException("Can't save $name to $file", e)
                }
            }
        }
    }

    private fun toResultFile(iri: IRI): IRI {
        return if (!args.targetIsDirectory) {
            IRI.create(args.targetFile.toUri())
        } else composeResultFilePath(
            if (args.sourceIsDirectory) args.sourceFile else args.sourceFile.parent,
            args.targetFile,
            iri,
            args.targetFormat.ext
        )
    }
}

private fun composeResultFilePath(
    inputDirectory: Path,
    outputDirectory: Path,
    inputFile: IRI,
    extension: String
): IRI {
    val src = Paths.get(inputFile.toURI())
    val fileName = src.fileName.toString() + "." + extension
    val relativeRes =
        inputDirectory.relativize(Paths.get(src.parent.toString() + outputDirectory.fileSystem.separator + fileName))
    var res = outputDirectory.resolve(relativeRes).normalize()
    Files.createDirectories(res.parent)
    res = res.parent.toRealPath().resolve(res.fileName)
    return IRI.create(res.toUri())
}