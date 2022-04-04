package com.github.sszuev.ontconverter.utils

import com.github.owlcs.ontapi.OntFormat
import org.semanticweb.owlapi.io.FileDocumentSource
import org.semanticweb.owlapi.io.IRIDocumentSource
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyID
import java.nio.file.Paths

/**
 * Creates a document-source from iri and format
 */
fun createSource(document: IRI, format: OntFormat?): OWLOntologyDocumentSource {
    return if (format == null) {
        IRIDocumentSource(document)
    } else FileDocumentSource(Paths.get(document.toURI()).toFile(), format.createOwlFormat())
}

fun toName(o: OWLOntology): IRI? {
    return toName(o.ontologyID) { o.owlOntologyManager.getOntologyDocumentIRI(o) }
}

private fun toName(id: OWLOntologyID, orElse: () -> IRI): IRI? {
    return id.ontologyIRI.orElseGet(orElse)
}
