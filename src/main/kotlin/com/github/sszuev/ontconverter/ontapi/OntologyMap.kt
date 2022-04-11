package com.github.sszuev.ontconverter.ontapi

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.OntGraphDocumentSource
import com.github.owlcs.ontapi.Ontology
import com.github.sszuev.ontconverter.utils.byImportsDeclarationCount
import com.github.sszuev.ontconverter.utils.createSource
import com.github.sszuev.ontconverter.utils.ontologyName
import org.apache.jena.graph.Graph
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource
import org.semanticweb.owlapi.model.*

/**
 * An iri mapper to work with local File System.
 * Contains a collection of [ontologies][OWLOntology] in disassembled form:
 * [id][OWLOntologyID] + [graph][Graph] + [format][OntFormat].
 */
class OntologyMap : OWLOntologyIRIMapper {
    private val ids: MutableMap<IRI, OWLOntologyID> = LinkedHashMap()
    private val graphs: MutableMap<IRI, Graph> = HashMap()
    private val formats: MutableMap<IRI, OntFormat> = HashMap()

    /**
     * Puts document-ontology pair into this map.
     */
    @Throws(IllegalArgumentException::class)
    fun put(document: IRI, ontology: Ontology): OntologyMap {
        require(!ids.containsKey(document)) { "The map already contains document $document." }
        ids[document] = ontology.ontologyID
        graphs[document] = ontology.asGraphModel().baseGraph
        val format = ontology.format
        if (format != null) formats[document] = OntFormat.get(format)
        return this
    }

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
     * Gets [OWLOntologyID] by document [IRI].
     */
    fun geOntologyID(documentIRI: IRI): OWLOntologyID? {
        return ids[documentIRI]
    }

    /**
     * Returns all document [IRI]s as sequence.
     */
    fun documents(): Sequence<IRI> {
        return ids.keys.asSequence()
    }

    /**
     * Returns a sequence of document sources in ascending order of imports count.
     * @return a [Sequence] of [OntGraphDocumentSource]s
     */
    fun sources(): Sequence<OWLOntologyDocumentSource> {
        return graphs.entries.asSequence()
            .sortedWith(java.util.Map.Entry.comparingByValue(byImportsDeclarationCount))
            .map { createSource(it.key, it.value) }
    }

    /**
     * Represents the mapper as [Map].
     * @return [Map] with [IRI]-[OWLOntologyID] pairs.
     */
    fun toIdsMap(): Map<IRI, OWLOntologyID> {
        return ids
    }

    /**
     * Answers `true` if this map is empty.
     */
    val isEmpty: Boolean
        get() = ids.isEmpty()

    override fun toString(): String {
        return ids.entries.asSequence()
            .map { "<${it.key}> => ${ontologyName(it.value)} [${formats[it.key]}]" }
            .joinToString("\n")
    }
}
