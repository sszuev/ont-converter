package com.github.sszuev.ontconverter

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntologyManager
import com.github.sszuev.ontconverter.api.*
import com.github.sszuev.ontconverter.api.utils.loadOntology
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.semanticweb.owlapi.formats.PrefixDocumentFormat
import org.semanticweb.owlapi.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.nameWithoutExtension

private val logger: Logger = LoggerFactory.getLogger(Processor::class.java)

/**
 * A core of the tool: a facility to control transformation.
 */
class Processor(private val args: Args) {

    fun run() {
        logger.info("Start...")
        if (args.sourceIsDirectory) {
            handleDirectory()
        } else {
            handleSingleFile()
        }
        logger.info("Done.")
    }

    private fun handleDirectory() {
        logger.info("Read directory ${args.sourceFile}")
        val mappings = loadDirectory(args.sourceFile, args.sourceFormat, args.force)
        runBlocking(Dispatchers.IO) {
            mappings.map {
                launch {
                    processOntologyMap(it)
                }
            }.forEach { it.join() }
        }
    }

    private fun handleSingleFile() {
        logger.info("Read file ${args.sourceFile}")
        val mapping = loadFile(args.sourceFile, args.sourceFormat, args.force)
        processOntologyMap(mapping)
    }

    private fun processOntologyMap(mapping: OntologyMap) {
        val manager = setup(
            createManager(
                personality = args.personality,
                softLoading = args.force,
                onlyFileSystem = !args.web,
                withLoadTransformation = true,
                spin = args.spin
            ), mapping
        )
        if (manager != null) {
            save(manager, mapping.ids)
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
    internal fun setup(manager: OntologyManager, map: OntologyMap): OntologyManager? {
        if (map.ids.isEmpty()) {
            if (args.force) {
                logger.error("Nothing to save")
                return null
            }
            throw OntApiException("Noting to save")
        }
        map.sources().forEach { loadOntology(it, manager, args.force) }
        val res = if (args.refine) {
            logger.info("Refine...")
            createOWLCopyManager(source = manager, ignoreExceptions = args.force)
        } else {
            manager
        }
        logger.debug(
            "Number of ontologies in the manager: ${res.ontologies().count()}, " +
                    "number of ontologies to save: ${map.ids.size}"
        )
        return res
    }

    /**
     * Saves ontologies to the target.
     * @param[manager][OntologyManager]
     * @param[ontologies] a [Map] with ([IRI] - [OWLOntologyID]) pairs
     */
    @Throws(OntApiException::class)
    internal fun save(manager: OntologyManager, ontologies: Map<IRI, OWLOntologyID>) {
        for (src in ontologies.keys) {
            val id = ontologies[src]!!
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
    val fileName = src.fileName.nameWithoutExtension + "." + extension
    val relativeRes =
        inputDirectory.relativize(Paths.get(src.parent.toString() + outputDirectory.fileSystem.separator + fileName))
    var res = outputDirectory.resolve(relativeRes).normalize()
    Files.createDirectories(res.parent)
    res = res.parent.toRealPath().resolve(res.fileName)
    return IRI.create(res.toUri())
}