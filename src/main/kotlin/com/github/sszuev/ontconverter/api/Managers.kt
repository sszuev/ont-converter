package com.github.sszuev.ontconverter.api

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.Ontology
import com.github.owlcs.ontapi.OntologyManager
import com.github.owlcs.ontapi.config.OntConfig
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig
import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality
import com.github.sszuev.ontconverter.api.utils.byImportsCount
import com.github.sszuev.ontconverter.api.utils.copyOWLContent
import com.github.sszuev.ontconverter.api.utils.insertOntology
import com.github.sszuev.ontconverter.api.utils.ontologyName
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
 * @param [softLoading] if `true`, then missed imports and wrong axioms are ignored
 * @param [onlyFileSystem] if `true`, then loads only from file system, web-diving is disabled
 * @param [withLoadTransformation] if `true`, then load graph transformation is performed
 * @param [spin] if `true`, then transformation `spin-queries -> axioms` is enabled into configuration
 * @return [OntologyManager]
 */
fun createManager(
    personality: OntPersonality?,
    softLoading: Boolean,
    onlyFileSystem: Boolean,
    withLoadTransformation: Boolean,
    spin: Boolean
): OntologyManager {
    val manager: OntologyManager = createDefaultManager()
    val config: OntConfig = manager.ontologyConfigurator
    if (personality != null) {
        config.personality = personality
    }
    if (spin) {
        TODO("spin transformation is not supported right now")
    }
    if (onlyFileSystem) {
        config.supportedSchemes = listOf(OntConfig.DefaultScheme.FILE)
    }
    config.isPerformTransformation = !withLoadTransformation
    if (softLoading) {
        config.missingImportHandlingStrategy = MissingImportHandlingStrategy.SILENT
        config.isIgnoreAxiomsReadErrors = true
    }
    return manager
}

/**
 * Creates a soft manager suitable for traversing through directory.
 * It has the following settings: missing imports are ignored, punnings are allowed,
 * any errors while collecting axioms are ignored,
 * No transformations are performed.
 * If the document being processing is a valid RDF and the serialization format is supported by jena
 * then the graph is loaded as is and may contain any non-OWL constructions.
 * Otherwise, OWLAPI mechanisms are used and the graph will only contain a valid OWL axioms (unless the load fails).
 *
 * @return [OntologyManager]
 */
fun createSoftManager(): OntologyManager {
    val manager: OntologyManager = createDefaultManager()
    manager.ontologyConfigurator
        .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)
        .setPerformTransformation(false)
        .setSupportedSchemes(listOf(OntConfig.DefaultScheme.FILE))
        .setPersonality(OntModelConfig.ONT_PERSONALITY_LAX).isIgnoreAxiomsReadErrors = true
    return manager
}

/**
 * Creates (if not specified) a manager and copies all the OWL contents of [source] manager,
 * leaving non-OWL RDF garbage.
 * @param [source][OntologyManager]
 * @param [target][OntologyManager] (by default [createDefaultManager])
 * @param [ignoreExceptions][Boolean]
 * @return [OntologyManager] a new manager or [target] if specified
 */
@Throws(OntApiException::class)
fun createOWLCopyManager(
    source: OntologyManager,
    target: OntologyManager = createDefaultManager(),
    ignoreExceptions: Boolean = true
): OntologyManager {
    target.ontologyConfigurator.personality = source.ontologyConfigurator.personality
    copyOntologies(source, target, ignoreExceptions) { targetManager, srcOntology ->
        copyOWLContent(targetManager, srcOntology)
    }
    return target
}

/**
 * Copies ontologies from one manager to another.
 * @param [source][OntologyManager]
 * @param [target][OntologyManager]
 * @param [ignoreExceptions][Boolean]
 * @param [copyMethod] - by default coping with [OntologyCopy.DEEP]
 */
@Throws(OntApiException::class)
fun copyOntologies(
    source: OntologyManager,
    target: OntologyManager,
    ignoreExceptions: Boolean,
    copyMethod: (OntologyManager, Ontology) -> Unit = { targetManager, sourceOntology ->
        insertOntology(targetManager, sourceOntology)
    }
) {
    source.ontologies()
        .sorted(byImportsCount)
        .forEach {
            val name: String = ontologyName(it.ontologyID)
            logger.trace("Copy ontology $name")
            try {
                copyMethod.invoke(target, it as Ontology)
            } catch (ex: Exception) {
                if (ignoreExceptions) {
                    logger.error("Can't copy ontology document $name")
                } else {
                    throw OntApiException("Can't copy manager, problem with ontology $name", ex)
                }
            }
        }
}
