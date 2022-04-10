package com.github.sszuev.ontconverter.utils

import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyID

val byImportsDeclarationCount: Comparator<OWLOntology> = Comparator.comparingLong { it.importsDeclarations().count() }
val byImportsCount: Comparator<OWLOntology> = Comparator.comparingLong { it.imports().count() }

fun getNameIRI(ont: OWLOntology): IRI {
    return getNameIRI(ont.ontologyID) { ont.owlOntologyManager.getOntologyDocumentIRI(ont) }
}

fun getNameIRI(id: OWLOntologyID, orElse: () -> IRI): IRI {
    return id.ontologyIRI.orElseGet(orElse)
}
