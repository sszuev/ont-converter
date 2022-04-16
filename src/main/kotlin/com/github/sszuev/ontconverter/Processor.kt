package com.github.sszuev.ontconverter

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntologyManager
import com.github.sszuev.ontconverter.ontapi.OntologyMap
import com.github.sszuev.ontconverter.utils.createCopyManager
import com.github.sszuev.ontconverter.utils.createManager
import com.github.sszuev.ontconverter.utils.loadFile
import com.github.sszuev.ontconverter.utils.loadOntology
import org.semanticweb.owlapi.formats.PrefixDocumentFormat
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLDocumentFormat
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyStorageException
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
        val mapping = loadFile(args.sourceFile, args.sourceFormat, args.force)
        val manager = compile(createManager(args.personality, force = args.force, spin = args.spin), mapping)
        if (manager != null) {
            save(manager, mapping)
        }
    }

    /**
     * Prepares manager for saving by passing preloaded ontologies,
     * loading dependent ontologies, if required cleaning up final manager instance from possible RDF garbage.
     * @param[manager][OntologyManager]
     * @param[map][OntologyMap]
     * @return [OntologyManager] same instance or new one or `null`
     */
    @Throws(OntApiException::class)
    internal fun compile(manager: OntologyManager, map: OntologyMap): OntologyManager? {
        if (map.ids.isEmpty()) {
            if (args.force) {
                logger.error("Nothing to save")
                return null
            }
            throw OntApiException("Noting to save")
        }
        map.sources().forEach { loadOntology(it, manager, args.force) }
        logger.debug(
            "Number of ontologies in the manager: ${manager.ontologies().count()}, " +
                    "number of ontologies to save: ${map.ids.size}"
        )
        return if (args.refine) {
            logger.info("Refine...")
            createCopyManager(source = manager, ignoreExceptions = args.force)
        } else {
            manager
        }
    }

    /**
     * Saves ontologies to the target.
     * @param[manager][OntologyManager]
     * @param[map][OntologyMap]
     */
    @Throws(OntApiException::class)
    internal fun save(manager: OntologyManager, map: OntologyMap) {
        for (src in map.ids.keys) {
            val id = map.ids[src]!!
            val file: IRI = toResultFile(src)
            val name: String = id.ontologyIRI.map { "<${it.iriString}>" }.orElse("<anonymous>")
            val ont: OWLOntology =
                requireNotNull(manager.getOntology(id)) { "The ontology not found. id=$name, file=$src." }
            logger.info("Save ontology $name as ${args.targetFormat} to ${file}.")
            try {
                val internalFormat = ont.format ?: throw IllegalStateException("No format for ont $name")
                val targetFormat: OWLDocumentFormat = args.targetFormat.createOwlFormat()
                if (internalFormat is PrefixDocumentFormat && targetFormat is PrefixDocumentFormat) {
                    targetFormat.asPrefixOWLDocumentFormat()
                        .setPrefixManager(internalFormat.asPrefixOWLDocumentFormat())
                }
                manager.saveOntology(ont, targetFormat, file)
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