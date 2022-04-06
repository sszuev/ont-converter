package com.github.sszuev.ontconverter.utils

import com.github.owlcs.ontapi.OntFormat
import org.semanticweb.owlapi.io.FileDocumentSource
import org.semanticweb.owlapi.io.IRIDocumentSource
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource
import org.semanticweb.owlapi.model.IRI
import java.nio.file.Paths

/**
 * Creates a document-source from iri and format
 */
fun createSource(document: IRI, format: OntFormat?): OWLOntologyDocumentSource {
    return if (format == null) {
        IRIDocumentSource(document)
    } else FileDocumentSource(Paths.get(document.toURI()).toFile(), format.createOwlFormat())
}