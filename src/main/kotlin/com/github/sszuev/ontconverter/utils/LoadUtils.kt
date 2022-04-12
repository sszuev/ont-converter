package com.github.sszuev.ontconverter.utils

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.Ontology
import com.github.owlcs.ontapi.OntologyManager
import com.github.sszuev.ontconverter.ontapi.OntologyMap
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntologyCreationException
import org.semanticweb.owlapi.model.OWLOntologyID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import kotlin.streams.asSequence

private val logger: Logger = LoggerFactory.getLogger("LoadUtils.kt")

/**
 * Loads single ontology from the [source] into the specified [manager].
 *
 * @param [manager][OntologyManager]
 * @param [source][OWLOntologyDocumentSource]
 * @param [ignoreExceptions][Boolean]
 * @return [OWLOntologyID?][OWLOntologyID]
 */
@Throws(OntApiException::class)
internal fun loadSource(
    manager: OntologyManager,
    source: OWLOntologyDocumentSource,
    ignoreExceptions: Boolean
): OWLOntologyID? {
    return loadSourceOntology(manager, source, ignoreExceptions)?.ontologyID
}

/**
 * Loads the specified directory into an [OntologyMap] or several [OntologyMap]s.
 * The catalog may contain duplicate ontologies (ontologies with the same id),
 * in which case multiple mappings will be created.
 * @param [dir]
 * @param [format] or `null`
 * @param [factory] to produce safe manager, which ignores missing imports
 * @param [continueIfError]
 * @return [List] of [OntologyMap]s
 */
@Throws(IOException::class, OntApiException::class)
internal fun loadDirectory(
    dir: Path,
    format: OntFormat?,
    factory: () -> OntologyManager,
    continueIfError: Boolean
): List<OntologyMap> {
    val ontologies: MutableMap<IRI, Ontology> = LinkedHashMap() // LinkedHashMap to preserve order
    listFiles(dir, continueIfError).forEach {
        val manager = factory.invoke() // each ontology has its own manager to avoid possible clashes
        val ont = loadSourceOntology(manager, createSource(it, format), continueIfError)
        if (ont != null) {
            ontologies[it] = ont
        }
    }
    val dependencies = toDependencyMap(ontologies)
    val components = findIndependentComponents(dependencies)
    return components
        .map {
            it.asSequence().map { iri ->
                iri to ontologies[iri]!!
            }.toMap()
        }
        .map { OntologyMap.of(it) }
        .toList()
}

/**
 * Transforms a collection of ([document][IRI] - [Ontology]) pairs to
 * a collection of ([document][IRI] - [importDocument][IRI][Set]) pairs.
 * @param [ontologies]
 * @return [Map]
 */
private fun toDependencyMap(ontologies: Map<IRI, Ontology>): Map<IRI, Set<IRI>> {
    val res: MutableMap<IRI, Set<IRI>> = HashMap()
    ontologies.forEach {
        val iri = it.key
        val ont: Ontology = it.value
        val other = ontologies.filterKeys { k -> iri != k }
        val dependencies = findImportDocumentSources(ont, other)
        res[iri] = dependencies
    }
    return res
}

/**
 * Fins all imports for [thisOntology] from [allOntologies] collection.
 * @param [thisOntology][Ontology]
 * @param [allOntologies] a [Map] with ([document][IRI] - [Ontology]) pairs
 * @return a [Set] of [Ontology]
 */
private fun findImportDocumentSources(thisOntology: Ontology, allOntologies: Map<IRI, Ontology>): Set<IRI> {
    val imports = thisOntology.importsDeclarations().asSequence().map { x -> x.iri }.toSet()
    return imports.mapNotNull { import ->
        allOntologies.filterValues {
            connectedByDirectImports(import, it.ontologyID, allOntologies = { allOntologies.values })
        }.map { it.key }.firstOrNull()
    }.toSet()
}

/**
 * Answers `true` if two ontologies (represented by [thisImportDeclaration] and [otherOntologyID])
 * can be considered as connected by imports.
 * @param [thisImportDeclaration][IRI]
 * @param [otherOntologyID][allOntologies]
 * @param [allOntologies] a [Collection] of [Ontology]s to perform final check
 * @return [Boolean]
 */
private fun connectedByDirectImports(
    thisImportDeclaration: IRI,
    otherOntologyID: OWLOntologyID,
    allOntologies: () -> Collection<Ontology>
): Boolean {
    if (otherOntologyID.isAnonymous) {
        return false
    }
    if (otherOntologyID.matchDocument(thisImportDeclaration)) {
        return true
    }
    if (!otherOntologyID.matchOntology(thisImportDeclaration)) {
        return false
    }
    // the primary ontologyIRI corresponds to the import,
    // if such an ontology is the only one in the collection, then it can be considered imported
    return allOntologies.invoke()
        //.asSequence()
        .map {
            it.ontologyID
        }
        .filter {
            it.matchOntology(thisImportDeclaration)
        }
        .drop(1)
        .isEmpty()
}

private fun loadSourceOntology(
    manager: OntologyManager,
    source: OWLOntologyDocumentSource,
    ignoreExceptions: Boolean
): Ontology? {
    return try {
        manager.loadOntologyFromOntologyDocument(source)
    } catch (ex: OWLOntologyCreationException) {
        val iri: IRI = getSourceIRI(source)
        if (ignoreExceptions) {
            logger.error("Unable to load ontology document $iri")
            return null
        }
        throw OntApiException("Can't proceed the ontology document $iri", ex)
    }
}