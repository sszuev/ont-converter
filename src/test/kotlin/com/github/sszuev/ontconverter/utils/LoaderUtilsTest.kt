package com.github.sszuev.ontconverter.utils

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.OntManagers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.semanticweb.owlapi.io.IRIDocumentSource
import org.semanticweb.owlapi.model.IRI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val logger: Logger = LoggerFactory.getLogger(LoaderUtilsTest::class.java)

class LoaderUtilsTest {

    @Test
    fun `test load single ontology fail`() {
        val m = OntManagers.createManager()
        val s = IRIDocumentSource(IRI.create("iri"))
        Assertions.assertNull(loadSource(m, s, true))
        Assertions.assertThrows(OntApiException::class.java) { loadSource(m, s, false) }
    }

    @Test
    fun `test load directory`() {
        val dir = Path.of(LoaderUtilsTest::class.java.getResource("/simple")!!.toURI())
        val parent = dir.fileName.toString()
        val mappings = loadDirectory(dir, OntFormat.TURTLE, ::createSoftManager, false)
        mappings.forEach {
            logger.debug("$it")
        }
        Assertions.assertEquals(7, mappings.size)
        // L + I
        Assertions.assertEquals(2, mappings[0].ids.size)
        Assertions.assertTrue(mappings[0].ids.keys.first().iriString.endsWith("$parent/AGHL/L.ttl"))
        Assertions.assertTrue(mappings[0].ids.keys.last().iriString.endsWith("$parent/I.ttl"))
        // A + B + C
        Assertions.assertEquals(3, mappings[1].ids.size)
        Assertions.assertTrue(mappings[1].ids.keys.toList()[0].iriString.endsWith("$parent/AGHL/A.ttl"))
        Assertions.assertTrue(mappings[1].ids.keys.toList()[1].iriString.endsWith("$parent/BCDFJE/BDEF/BF/B.ttl"))
        Assertions.assertTrue(mappings[1].ids.keys.toList()[2].iriString.endsWith("$parent/BCDFJE/CJ/C.ttl"))
        // G + J + H
        Assertions.assertEquals(3, mappings[2].ids.size)
        Assertions.assertTrue(mappings[2].ids.keys.toList()[0].iriString.endsWith("$parent/AGHL/G.ttl"))
        Assertions.assertTrue(mappings[2].ids.keys.toList()[1].iriString.endsWith("$parent/BCDFJE/CJ/J.ttl"))
        Assertions.assertTrue(mappings[2].ids.keys.toList()[2].iriString.endsWith("$parent/AGHL/H.ttl"))
        // A + D
        Assertions.assertEquals(2, mappings[3].ids.size)
        Assertions.assertTrue(mappings[3].ids.keys.first().iriString.endsWith("$parent/AGHL/A.ttl"))
        Assertions.assertTrue(mappings[3].ids.keys.last().iriString.endsWith("$parent/BCDFJE/BDEF/D.ttl"))
        // F
        Assertions.assertEquals(1, mappings[4].ids.size)
        Assertions.assertTrue(mappings[4].ids.keys.first().iriString.endsWith("$parent/BCDFJE/BDEF/BF/F.ttl"))
        // K
        Assertions.assertEquals(1, mappings[5].ids.size)
        Assertions.assertTrue(mappings[5].ids.keys.first().iriString.endsWith("$parent/K.ttl"))
        // A + E
        Assertions.assertEquals(2, mappings[6].ids.size)
        Assertions.assertTrue(mappings[6].ids.keys.first().iriString.endsWith("$parent/AGHL/A.ttl"))
        Assertions.assertTrue(mappings[6].ids.keys.last().iriString.endsWith("$parent/BCDFJE/BDEF/E.ttl"))
    }
}