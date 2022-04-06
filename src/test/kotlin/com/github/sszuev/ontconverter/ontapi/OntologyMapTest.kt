package com.github.sszuev.ontconverter.ontapi

import com.github.owlcs.ontapi.OntManagers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntologyID
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Paths

class OntologyMapTest {

    @Test
    fun `test put ok`() {
        val map = OntologyMap()
        Assertions.assertTrue(map.isEmpty)
        Assertions.assertEquals(0, map.toMap().size)

        val man = OntManagers.createManager()

        val ontA = man.createOntology(IRI.create("ns:ont-a"))
        map.put(IRI.create("h:A"), ontA)
        Assertions.assertFalse(map.isEmpty)
        Assertions.assertEquals(1, map.toMap().size)
        Assertions.assertEquals(ontA.ontologyID, map.toMap()[IRI.create("h:A")])

        val ontB = man.createOntology(IRI.create("ns:ont-b"))
        map.put(IRI.create("h:B"), ontB)
        Assertions.assertFalse(map.isEmpty)
        Assertions.assertEquals(2, map.toMap().size)
        Assertions.assertEquals(ontA.ontologyID, map.toMap()[IRI.create("h:A")])
        Assertions.assertEquals(ontB.ontologyID, map.toMap()[IRI.create("h:B")])

        val docs = map.documents().toList()
        Assertions.assertEquals(listOf(IRI.create("h:A"), IRI.create("h:B")), docs)

        Assertions.assertSame(man, map.getManager())

        Assertions.assertEquals(OWLOntologyID(IRI.create("ns:ont-a")), map.getOntologyID(IRI.create("h:A")))
        Assertions.assertEquals(OWLOntologyID(IRI.create("ns:ont-b")), map.getOntologyID(IRI.create("h:B")))

        Assertions.assertEquals(IRI.create("h:A"), map.getDocumentIRI(IRI.create("ns:ont-a")))
        Assertions.assertEquals(IRI.create("h:B"), map.getDocumentIRI(IRI.create("ns:ont-b")))

        Assertions.assertEquals("h:A => ns:ont-a [TURTLE]\nh:B => ns:ont-b [TURTLE]", map.toString())
    }

    @Test
    fun `test put fail`() {
        val map = OntologyMap()
        map.put(IRI.create("h:A"), OntManagers.createManager().createOntology())

        Assertions.assertThrows(IllegalArgumentException::class.java) {
            map.put(IRI.create("h:A"), OntManagers.createManager().createOntology())
        }

        Assertions.assertThrows(FileSystemNotFoundException::class.java) {
            map.sources().toList()
        }
    }

    @Test
    fun `test file sources`() {
        val ont = OntologyMapTest::class.java.getResourceAsStream("/pizza.ttl").use {
            OntManagers.createManager().loadOntologyFromOntologyDocument(it!!)
        }
        val doc = IRI.create(Paths.get("/x/x/x.x").toFile())
        val map = OntologyMap()
        map.put(doc, ont)
        val list = map.sources().map { it.documentIRI }.toList()
        Assertions.assertEquals(listOf(doc), list)
    }
}