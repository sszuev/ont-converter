package com.github.sszuev.ontconverter.ontapi

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.OntologyManager
import com.github.sszuev.ontconverter.utils.byImportsDeclarationCount
import com.github.sszuev.ontconverter.utils.createSource
import com.github.sszuev.ontconverter.utils.getNameIRI
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource
import org.semanticweb.owlapi.model.*

/**
 * An iri mapper to work with local File System.
 * Contains a collection of [ontologies][OWLOntology] and a reference to the [OWLOntologyManager]
 */
class OntologyMap : OWLOntologyIRIMapper {
    private val map: MutableMap<IRI, OWLOntology> = LinkedHashMap()
    private var manager: OntologyManager? = null

    /**
     * Puts document-ontology pair into this map.
     */
    @Throws(IllegalArgumentException::class)
    fun put(document: IRI, ontology: OWLOntology): OntologyMap {
        require(!map.containsKey(document)) { "The map already contains document $document." }
        val m = ontology.owlOntologyManager ?: throw IllegalArgumentException("The ontology has no manager.")
        if (this.manager == null) {
            this.manager = m as OntologyManager
        } else if (this.manager != m) {
            throw IllegalArgumentException("Wrong manager inside ${ontology.ontologyID}. Source=${document}")
        }
        map[document] = ontology
        return this
    }

    /**
     * To use inside [OWLOntologyManager]
     */
    override fun getDocumentIRI(ontologyIRI: IRI): IRI? {
        return map.entries.asSequence()
            .filter { it.value.ontologyID.matchOntology(ontologyIRI) }
            .map { it.key }
            .firstOrNull()
    }

    /**
     * Gets [OWLOntologyID] by document [IRI].
     */
    fun getOntologyID(documentIRI: IRI): OWLOntologyID? {
        return map[documentIRI]?.ontologyID
    }

    /**
     * Returns [OntologyManager] or `null` in case the mapper is empty
     */
    fun getManager(): OntologyManager? {
        return manager
    }

    /**
     * Returns all document [IRI]s as sequence.
     */
    fun documents(): Sequence<IRI> {
        return map.keys.asSequence()
    }

    /**
     * Returns a sequence of document sources in ascending order of imports count.
     */
    @Throws(NullPointerException::class)
    fun sources(): Sequence<OWLOntologyDocumentSource> {
        return map.entries.asSequence()
            .sortedWith(java.util.Map.Entry.comparingByValue(byImportsDeclarationCount))
            .map { e -> createSource(e.key, getFormatOrNull(e.value)!!) }
    }

    /**
     * Represents the mapper as [Map] with file [IRI]s as keys, [OWLOntologyID]s as values
     */
    fun toMap(): Map<IRI, OWLOntologyID> {
        return this.map.mapValues { it.value.ontologyID }
    }

    /**
     * Answers `true` if this map is empty.
     */
    val isEmpty: Boolean
        get() = map.isEmpty()

    override fun toString(): String {
        return map.entries.asSequence()
            .map { (key, value) -> "$key => ${getNameIRI(value)} [${getFormatOrNull(value)}]" }
            .joinToString("\n")
    }

    companion object {
        private fun getFormatOrNull(ont: OWLOntology): OntFormat? {
            val f = ont.format
            return if (f == null) null else OntFormat.get(f)
        }
    }
}
