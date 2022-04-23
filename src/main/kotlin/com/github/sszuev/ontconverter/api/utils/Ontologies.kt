package com.github.sszuev.ontconverter.api.utils

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.Ontology
import com.github.owlcs.ontapi.OntologyManager
import com.github.owlcs.ontapi.jena.utils.Graphs
import org.apache.jena.graph.Factory
import org.apache.jena.graph.Graph
import org.apache.jena.graph.GraphUtil
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.model.parameters.OntologyCopy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("Ontologies.kt")

val byImportsDeclarationCount: Comparator<Graph> = Comparator.comparingInt { Graphs.getImports(it).size }
val byImportsCount: Comparator<OWLOntology> = Comparator.comparingLong { it.imports().count() }

/**
 * [OWLOntologyID] -> [String].
 * @param [id][OWLOntologyID]
 * @return [String]
 */
fun ontologyName(id: OWLOntologyID): String {
    return id.ontologyIRI.map { "<${it.iriString}>" }.orElse("<anonymous>")
}

/**
 * Copies all owl content (axioms and annotations) ignoring non-OWL RDF triples.
 * @param [targetManager][OntologyManager]
 * @param [sourceOntology][Ontology]
 * @return [Ontology] -a new instance attached to [targetManager]
 */
fun copyOWLContent(targetManager: OntologyManager, sourceOntology: Ontology): Ontology {
    val res = targetManager.createOntology(sourceOntology.ontologyID)
    sourceOntology.annotations().forEach {
        res.applyChange(AddOntologyAnnotation(res, it))
    }
    sourceOntology.axioms().forEach { res.add(it) }
    return res
}

/**
 * Copies the ontology RDF base graph and place it into the target manager including format.
 * @param [targetManager][OntologyManager]
 * @param [sourceOntology][Ontology]
 * @return [Ontology] - a new instance attached to [targetManager]
 */
fun copyBaseGraph(targetManager: OntologyManager, sourceOntology: Ontology): Ontology {
    val graph = Factory.createGraphMem()
    GraphUtil.add(graph, sourceOntology.asGraphModel().baseGraph.find())
    val res = targetManager.addOntology(graph)
    val format = sourceOntology.owlOntologyManager.getOntologyFormat(sourceOntology)
    if (format != null) {
        targetManager.setOntologyFormat(res, format)
    }
    return res
}

/**
 * Inserts the ontology graph into the [targetManager].
 * @param [targetManager][OntologyManager]
 * @param [sourceOntology][Ontology]
 * @return [Ontology] - a new instance attached to [targetManager]
 */
fun insertOntology(targetManager: OntologyManager, sourceOntology: Ontology): Ontology {
    return targetManager.copyOntology(sourceOntology, OntologyCopy.DEEP)
}

/**
 * Performs ontology loading.
 *
 * @param [source][OWLOntologyDocumentSource]
 * @param [manager][OntologyManager]
 * @param [ignoreExceptions][Boolean]
 * @return [Ontology] or `null`
 */
fun loadOntology(
    source: OWLOntologyDocumentSource,
    manager: OntologyManager,
    ignoreExceptions: Boolean
): Ontology? {
    return try {
        manager.loadOntologyFromOntologyDocument(source)
    } catch (ex: OWLOntologyCreationException) {
        val iri: IRI = getSourceIRI(source)
        if (ignoreExceptions) {
            logger.error("Failed to load ontology document $iri")
            return null
        }
        throw OntApiException("Failed to load ontology document $iri", ex)
    }
}