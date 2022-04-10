package com.github.sszuev.ontconverter.utils

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntologyManager
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntologyCreationException
import org.semanticweb.owlapi.model.OWLOntologyID
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
    return try {
        manager.loadOntologyFromOntologyDocument(source).ontologyID
    } catch (ex: OWLOntologyCreationException) {
        val iri: IRI = getSourceIRI(source)
        if (ignoreExceptions) {
            logger.error("Unable to load ontology document $iri")
            return null
        }
        throw OntApiException("Can't proceed the ontology document $iri", ex)
    }
}