package com.github.sszuev.ontconverter.api.utils

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.OntGraphDocumentSource
import org.apache.jena.graph.Graph
import org.semanticweb.owlapi.io.FileDocumentSource
import org.semanticweb.owlapi.io.IRIDocumentSource
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource
import org.semanticweb.owlapi.model.IRI
import java.nio.file.Path

/**
 * Creates a document-source from [file] and [format].
 *
 * @param [file][Path]
 * @param [format][OntFormat] or `null`
 * @return [OWLOntologyDocumentSource]
 */
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // in owl-api:5.1.20, OWLDocumentFormat can be null
@Throws(java.lang.RuntimeException::class)
fun createSource(file: Path, format: OntFormat?): OWLOntologyDocumentSource {
    return FileDocumentSource(file.toFile(), format?.createOwlFormat())
}

/**
 * Creates a document-source from [iri] and [format].
 *
 * @param [iri][IRI]
 * @param [format][OntFormat] or `null`
 * @return [OWLOntologyDocumentSource]
 */
fun createSource(iri: IRI, format: OntFormat?): OWLOntologyDocumentSource {
    if (iri.scheme == "file") {
        return createSource(Path.of(iri.toURI()), format)
    }
    return IRIDocumentSource(iri, format?.createOwlFormat(), null)
}

/**
 * Creates a document-source from [iri] and [graph].
 *
 * @param [iri][IRI]
 * @param [graph][Graph]
 * @return [OWLOntologyDocumentSource]
 */
fun createSource(iri: IRI, graph: Graph): OWLOntologyDocumentSource {
    return object : OntGraphDocumentSource() {
        override fun getDocumentIRI(): IRI {
            return iri
        }

        override fun getGraph(): Graph {
            return graph
        }
    }
}