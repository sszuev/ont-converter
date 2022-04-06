package com.github.sszuev.ontconverter.utils

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.OntologyManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.parameters.OntologyCopy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("managers.kt")

/**
 * Copies managers content
 */
fun copyManager(from: OntologyManager, ignoreExceptions: Boolean = true): OntologyManager {
    val res: OntologyManager = OntManagers.createManager()
    res.ontologyConfigurator.personality = from.ontologyConfigurator.personality
    copyOntologies(from, res, ignoreExceptions)
    return res
}

/**
 * Copies ontologies from one manager to another.
 */
@Throws(OntApiException::class)
fun copyOntologies(from: OntologyManager, to: OntologyManager, ignoreExceptions: Boolean) {
    val ex = OntApiException("Can't copy manager")
    from.ontologies()
        .sorted(byImportsCount)
        .forEach {
            val name: IRI = getNameIRI(it)
            logger.trace("Copy ontology $name")
            try {
                to.copyOntology(it, OntologyCopy.DEEP)
            } catch (e: Exception) {
                ex.addSuppressed(e)
            }
        }
    if (!ignoreExceptions && ex.suppressed.isNotEmpty()) {
        throw ex
    }
}