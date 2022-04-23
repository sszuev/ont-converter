package com.github.sszuev.ontconverter.api.utils

import org.semanticweb.owlapi.io.FileDocumentSource
import org.semanticweb.owlapi.io.IRIDocumentSource
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource
import org.semanticweb.owlapi.model.OWLOntologyID
import java.nio.file.Paths

/**
 * [OWLOntologyID] -> [String].
 * @param [id][OWLOntologyID]
 * @return [String]
 */
fun ontologyName(id: OWLOntologyID): String {
    return id.ontologyIRI.map { "<${it.iriString}>" }.orElse("<anonymous>")
}

/**
 * [OWLOntologyDocumentSource] -> [String].
 * @param [source][OWLOntologyDocumentSource]
 * @return [String]
 */
fun sourceName(source: OWLOntologyDocumentSource): String {
    if (source is FileDocumentSource || (source is IRIDocumentSource && source.documentIRI.scheme == "file")) {
        return Paths.get(source.documentIRI.toURI()).toString()
    }
    return source.documentIRI.iriString
}

/**
 * [Throwable] -> [String].
 * @param [ex][Throwable]
 * @return [String]
 */
fun exceptionMessage(ex: Throwable): String {
    val msg = ex.message?.split("\n")?.get(0)?.take(100)
    val clazz = ex.javaClass.name.split(".").last()
    return "$clazz -- $msg"
}