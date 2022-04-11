package com.github.sszuev.ontconverter.utils

import com.github.owlcs.ontapi.jena.utils.Graphs
import org.apache.jena.graph.Graph
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyID

val byImportsDeclarationCount: Comparator<Graph> = Comparator.comparingInt { Graphs.getImports(it).size }
val byImportsCount: Comparator<OWLOntology> = Comparator.comparingLong { it.imports().count() }

fun ontologyName(id: OWLOntologyID): String {
    return id.ontologyIRI.map { "<${it.iriString}>" }.orElse("<anonymous>")
}