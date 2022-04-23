package com.github.sszuev.ontconverter.api.utils

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.OntGraphDocumentSource
import org.apache.jena.graph.Graph
import org.semanticweb.owlapi.io.FileDocumentSource
import org.semanticweb.owlapi.io.IRIDocumentSource
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource
import org.semanticweb.owlapi.model.IRI
import java.nio.file.Paths

/**
 * Creates a document-source from iri and format.
 * @param [document][IRI]
 * @param [format][OntFormat] or `null`
 * @return [OWLOntologyDocumentSource]
 */
@Throws(java.lang.RuntimeException::class)
fun createSource(document: IRI, format: OntFormat?): OWLOntologyDocumentSource {
    return if (format == null) {
        IRIDocumentSource(document)
    } else FileDocumentSource(Paths.get(document.toURI()).toFile(), format.createOwlFormat())
}

/**
 * Creates a document-source from iri and graph.
 * @param [document][IRI]
 * @param [graph][Graph]
 * @return [OWLOntologyDocumentSource]
 */
fun createSource(document: IRI, graph: Graph): OWLOntologyDocumentSource {
    return object : OntGraphDocumentSource() {
        override fun getDocumentIRI(): IRI {
            return document
        }

        override fun getGraph(): Graph {
            return graph
        }
    }
}

/**
 * Retrieves an iri from document-source
 * @param [source][OWLOntologyDocumentSource]
 * @return [IRI]
 */
@Throws(java.lang.RuntimeException::class)
fun getSourceIRI(source: OWLOntologyDocumentSource): IRI {
    return if (source is FileDocumentSource) {
        IRI.create(Paths.get(source.getDocumentIRI().toURI()).toUri())
    } else source.documentIRI
}