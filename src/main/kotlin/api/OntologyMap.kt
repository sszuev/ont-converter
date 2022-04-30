package com.github.sszuev.ontconverter.api

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.OntGraphDocumentSource
import com.github.owlcs.ontapi.Ontology
import com.github.sszuev.ontconverter.api.utils.createSource
import com.github.sszuev.ontconverter.api.utils.ontologyName
import org.apache.jena.graph.Graph
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource
import org.semanticweb.owlapi.model.*

/**
 * An iri mapper to work with local File System.
 * Contains a collection of [ontologies][OWLOntology] in disassembled form:
 * [id][OWLOntologyID] + [graph][Graph] + [format][OntFormat].
 */
data class OntologyMap(
    val ids: Map<IRI, OWLOntologyID>,
    val graphs: Map<IRI, Graph>,
    val formats: Map<IRI, OntFormat>
) : OWLOntologyIRIMapper {

    /**
     * To use inside [OWLOntologyManager]
     */
    override fun getDocumentIRI(ontologyIRI: IRI): IRI? {
        return ids.entries.asSequence()
            .filter { it.value.matchOntology(ontologyIRI) }
            .map { it.key }
            .firstOrNull()
    }

    /**
     * Returns a sequence of document sources in ascending order of imports count.
     * @return a [Sequence] of [OntGraphDocumentSource]s
     */
    fun sources(): Sequence<OWLOntologyDocumentSource> {
        return graphs.entries.asSequence().map { createSource(it.key, it.value) }
    }

    override fun toString(): String {
        return ids.entries.asSequence()
            .map { "<${it.key}> => ${ontologyName(it.value)} [${formats[it.key]}]" }
            .joinToString(separator = ", ", prefix = "{", postfix = "}")
    }

    companion object {
        /**
         * Creates ontology mapping object.
         *
         * @param [ontologies] ([document-source][IRI] - [Ontology]) pairs
         * @return [OntologyMap]
         */
        fun of(vararg ontologies: Pair<IRI, Ontology>): OntologyMap {
            return of(ontologies.toMap())
        }

        /**
         * Creates an ontology mapping object, preserving an order defined by the input map.
         *
         * @param [ontologies] a [Map] with ([document-source][IRI] - [Ontology]) pairs
         * @return [OntologyMap]
         */
        fun of(ontologies: Map<IRI, Ontology>): OntologyMap {
            val ids: MutableMap<IRI, OWLOntologyID> = LinkedHashMap()
            val graphs: MutableMap<IRI, Graph> = LinkedHashMap()
            val formats: MutableMap<IRI, OntFormat> = LinkedHashMap()
            ontologies.forEach {
                ids[it.key] = it.value.ontologyID
                graphs[it.key] = it.value.asGraphModel().baseGraph
                val format = it.value.format
                if (format != null) formats[it.key] = OntFormat.get(format)
            }
            return OntologyMap(ids, graphs, formats)
        }
    }
}
