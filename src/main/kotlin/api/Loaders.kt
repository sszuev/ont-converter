package com.github.sszuev.ontconverter.api

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.Ontology
import com.github.owlcs.ontapi.OntologyManager
import com.github.sszuev.ontconverter.api.utils.*
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntologyID
import java.io.IOException
import java.nio.file.Path
import kotlin.streams.asSequence

/**
 * Loads the specified [file] containing ontology-document into the specified [manager].
 *
 * @param [file][Path]
 * @param [format][OntFormat] or `null`
 * @param [manager][OntologyManager] the manager
 * (by default it is soft manager, which ignores any missing imports and other errors)
 * @param [ignoreExceptions][Boolean]
 * @return [OntologyMap]
 */
@Throws(OntApiException::class)
fun loadFile(
    file: Path,
    format: OntFormat?,
    ignoreExceptions: Boolean,
    manager: OntologyManager = createSoftManager()
): OntologyMap {
    return loadOntologyMapping(createSource(file, format), manager, ignoreExceptions)
}

/**
 * Loads the specified directory into an [OntologyMap] or several [OntologyMap]s.
 * The catalog may contain duplicate ontologies (ontologies with the same id),
 * in which case multiple mappings will be created.
 *
 * Each [OntologyMap]-container contains a collection of graphs in order from independent to dependent.
 * For example, if an ontology `a` imports an ontology `b` (i.e. graph `a` has triple `<a> owl:import <b>`),
 * then sequence of ontologies is `b`, `a`.
 *
 * @param [dir][Path]
 * @param [format][OntFormat] or `null`
 * @param [factory] to produce manager instances
 * (by default it produces soft managers, which ignore any missing imports, have no transformations, etc)
 * @param [continueIfError]
 * @return [List] of [OntologyMap]s
 */
@Throws(IOException::class, OntApiException::class)
fun loadDirectory(
    dir: Path,
    format: OntFormat?,
    continueIfError: Boolean,
    factory: () -> OntologyManager = ::createSoftManager
): List<OntologyMap> {
    return loadOntologyMappings(
        { listFiles(dir, continueIfError) },
        factory,
        { i, m -> loadOntology(createSource(i, format), m, continueIfError) }
    )
}

/**
 * Loads a single ontology from the [source] using the specified [manager],
 * returns the result in the form of [OntologyMap].
 *
 * @param [source][OWLOntologyDocumentSource]
 * @param [manager][OntologyManager]
 * @param [ignoreExceptions][Boolean]
 * @return [OntologyMap] (possibly empty in case of error)
 */
@Throws(OntApiException::class)
fun loadOntologyMapping(
    source: OWLOntologyDocumentSource,
    manager: OntologyManager,
    ignoreExceptions: Boolean
): OntologyMap {
    val ont = loadOntology(source, manager, ignoreExceptions) ?: return OntologyMap.of()
    return OntologyMap.of(source.documentIRI to ont)
}

/**
 * Loads many ontologies from a sequence of sources (provided by [sourceProvider]) using the [ontologyLoader].
 *
 * @param [sourceProvider] provides sources as [Sequence] of [IRI]s
 * @param [managerFactory] manager factory
 * @param [ontologyLoader] method to load [Ontology] from [IRI] to [OntologyManager]
 * @return a [List] of [Ontology]s
 */
fun loadOntologyMappings(
    sourceProvider: () -> Sequence<IRI>,
    managerFactory: () -> OntologyManager,
    ontologyLoader: (IRI, OntologyManager) -> Ontology?,
): List<OntologyMap> {
    val ontologies: MutableMap<IRI, Ontology> = LinkedHashMap() // LinkedHashMap to preserve order
    sourceProvider.invoke().forEach {
        val manager = managerFactory.invoke() // each ontology has its own manager to avoid possible clashes
        val ont = ontologyLoader.invoke(it, manager)
        if (ont != null) {
            ontologies[it] = ont
        }
    }
    val dependencies = toDependencyMap(ontologies)
    val components = findIndependentComponents(dependencies)
    val ids = HashSet<OWLOntologyID>()
    return components
        .map {
            it.asSequence().map { iri ->
                var ont = ontologies[iri]!!
                if (!ids.add(ont.ontologyID)) {
                    // make a copy of a base graph
                    ont = copyBaseGraph(managerFactory.invoke(), ont)
                }
                iri to ont
            }.toMap()
        }
        .map { OntologyMap.of(it) }
        .toList()
}

/**
 * Transforms a collection of ([document][IRI] - [Ontology]) pairs to
 * a collection of ([document][IRI] - [importDocument][IRI][Set]) pairs.
 *
 * @param [ontologies]
 * @return [Map]
 */
private fun toDependencyMap(ontologies: Map<IRI, Ontology>): Map<IRI, Set<IRI>> {
    val res: MutableMap<IRI, Set<IRI>> = LinkedHashMap()
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
 *
 * @param [thisOntology][Ontology]
 * @param [allOntologies] a [Map] with ([document][IRI] - [Ontology]) pairs
 * @return a [Set] of [Ontology]
 */
private fun findImportDocumentSources(thisOntology: Ontology, allOntologies: Map<IRI, Ontology>): Set<IRI> {
    val imports = thisOntology.importsDeclarations().asSequence().map { it.iri }.toSet()
    return imports.mapNotNull { import ->
        allOntologies.filterValues {
            connectedByDirectImports(import, it.ontologyID, allOntologies = { allOntologies.values })
        }.map { it.key }.firstOrNull()
    }.toSet()
}

/**
 * Answers `true` if two ontologies (represented by [thisImportDeclaration] and [otherOntologyID])
 * can be considered as connected by imports.
 *
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
        .map { it.ontologyID }
        .filter {
            it.matchOntology(thisImportDeclaration)
        }
        .drop(1)
        .isEmpty()
}

