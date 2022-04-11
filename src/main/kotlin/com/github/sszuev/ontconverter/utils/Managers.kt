package com.github.sszuev.ontconverter.utils

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.OntologyManager
import com.github.owlcs.ontapi.config.OntConfig
import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy
import org.semanticweb.owlapi.model.parameters.OntologyCopy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("Managers.kt")

/**
 * Just an alias for [OntManagers.createManager]
 */
fun createDefaultManager(): OntologyManager = OntManagers.createManager()

/**
 * Creates a [manager][OntologyManager] according to specified settings.
 *
 * @param [personality][OntPersonality]
 * @param [force] if `true`, then missed imports and wrong axioms are ignored
 * @param [spin] if `true`, then transformation `spin-queries -> axioms` is enabled into configuration
 * @return [OntologyManager]
 */
fun createManager(personality: OntPersonality?, force: Boolean, spin: Boolean): OntologyManager {
    val manager: OntologyManager = createDefaultManager()
    val config: OntConfig = manager.ontologyConfigurator
    if (personality != null) {
        config.personality = personality
    }
    if (spin) {
        TODO("spin transformation is not supported right now")
    }
    if (force) {
        config.missingImportHandlingStrategy = MissingImportHandlingStrategy.SILENT
        config.isIgnoreAxiomsReadErrors = true
    }
    return manager
}

/**
 * Creates (if not specified) a manager and copies managers content form the [source].
 *
 * @param [source][OntologyManager]
 * @param [target][OntologyManager]
 * @param [ignoreExceptions][Boolean]
 * @return [OntologyManager] a new manager or [target] if specified
 */
@Throws(OntApiException::class)
fun createCopyManager(
    source: OntologyManager,
    target: OntologyManager = createDefaultManager(),
    ignoreExceptions: Boolean = true
): OntologyManager {
    target.ontologyConfigurator.personality = source.ontologyConfigurator.personality
    copyOntologies(source, target, ignoreExceptions)
    return target
}

/**
 * Copies ontologies from one manager to another.
 * @param [source][OntologyManager]
 * @param [target][OntologyManager]
 * @param [ignoreExceptions][Boolean]
 */
@Throws(OntApiException::class)
fun copyOntologies(source: OntologyManager, target: OntologyManager, ignoreExceptions: Boolean) {
    source.ontologies()
        .sorted(byImportsCount)
        .forEach {
            val name: String = ontologyName(it.ontologyID)
            logger.trace("Copy ontology $name")
            try {
                target.copyOntology(it, OntologyCopy.DEEP)
            } catch (ex: Exception) {
                if (ignoreExceptions) {
                    logger.error("Can't copy ontology document $name")
                } else {
                    throw OntApiException("Can't copy manager, problem with ontology $name", ex)
                }
            }
        }
}
