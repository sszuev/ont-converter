package com.github.sszuev.ontconverter.ontapi

import com.github.owlcs.ontapi.OntGraphDocumentSource
import com.github.sszuev.ontconverter.utils.createDefaultManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntologyID
import java.nio.file.Paths

class OntologyMapTest {

    @Test
    fun `test put ok`() {
        val man = createDefaultManager()
        val ontA = man.createOntology(IRI.create("ns:ont-a"))
        val ontB = man.createOntology(IRI.create("ns:ont-b"))

        val map = OntologyMap.of(IRI.create("h:A") to ontA, IRI.create("h:B") to ontB)
        Assertions.assertFalse(map.isEmpty)
        Assertions.assertEquals(2, map.toIdsMap().size)

        Assertions.assertEquals(ontA.ontologyID, map.toIdsMap()[IRI.create("h:A")])
        Assertions.assertEquals(ontB.ontologyID, map.toIdsMap()[IRI.create("h:B")])

        val docs = map.documents().toList()
        Assertions.assertEquals(listOf(IRI.create("h:A"), IRI.create("h:B")), docs)

        Assertions.assertEquals(OWLOntologyID(IRI.create("ns:ont-a")), map.geOntologyID(IRI.create("h:A")))
        Assertions.assertEquals(OWLOntologyID(IRI.create("ns:ont-b")), map.geOntologyID(IRI.create("h:B")))

        Assertions.assertEquals(IRI.create("h:A"), map.getDocumentIRI(IRI.create("ns:ont-a")))
        Assertions.assertEquals(IRI.create("h:B"), map.getDocumentIRI(IRI.create("ns:ont-b")))

        Assertions.assertEquals("<h:A> => <ns:ont-a> [TURTLE]\n<h:B> => <ns:ont-b> [TURTLE]", map.toString())
    }

    @Test
    fun `test load file`() {
        val ont = OntologyMapTest::class.java.getResourceAsStream("/pizza.ttl").use {
            createDefaultManager().loadOntologyFromOntologyDocument(it!!)
        }
        val doc = IRI.create(Paths.get("/x/x/x.x").toFile())
        val map = OntologyMap.of(doc to ont)
        val documents = map.sources().map { it.documentIRI }.toList()
        Assertions.assertEquals(listOf(doc), documents)
        val graphs = map.sources().map { it as OntGraphDocumentSource }.map { it.graph }.toList()
        Assertions.assertEquals(1, graphs.size)
        Assertions.assertSame(ont.asGraphModel().baseGraph, graphs[0])
    }
}