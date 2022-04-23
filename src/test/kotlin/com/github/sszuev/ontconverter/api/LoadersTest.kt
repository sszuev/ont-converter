package com.github.sszuev.ontconverter.api

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.OntManagers
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Triple
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

private val logger: Logger = LoggerFactory.getLogger(LoaderTest::class.java)

class LoaderTest {

    @Test
    fun `test load single ontology fail`() {
        val file = Path.of("XX")
        val map = loadFile(file, null, ignoreExceptions = true)
        Assertions.assertTrue(map.ids.isEmpty())
        Assertions.assertThrows(OntApiException::class.java) {
            loadFile(
                file,
                OntFormat.DL,
                ignoreExceptions = false,
                OntManagers.createManager()
            )
        }
    }

    @Test
    fun `test load single ontology success`() {
        val sourceFile =
            Paths.get(LoaderTest::class.java.getResource("/ontologies/pizza.ttl")?.toURI() ?: Assertions.fail())
        val map = loadFile(sourceFile, OntFormat.TURTLE, ignoreExceptions = false)
        Assertions.assertEquals(1, map.ids.size)
    }

    @Test
    fun `test load directory - check components are valid`() {
        val dir = Path.of(LoaderTest::class.java.getResource("/simple-tree")!!.toURI())
        val parent = dir.fileName.toString()
        val mappings = loadDirectory(dir, OntFormat.TURTLE, false, ::createSoftManager)
        mappings.forEach {
            logger.debug("$it")
        }
        Assertions.assertEquals(7, mappings.size)
        // F
        Assertions.assertEquals(1, mappings[0].ids.size)
        Assertions.assertTrue(mappings[0].ids.keys.first().iriString.endsWith("$parent/BCDFJE/BDEF/BF/F.ttl"))
        // A + D
        Assertions.assertEquals(2, mappings[1].ids.size)
        Assertions.assertTrue(mappings[1].ids.keys.first().iriString.endsWith("$parent/AGHL/A.ttl"))
        Assertions.assertTrue(mappings[1].ids.keys.last().iriString.endsWith("$parent/BCDFJE/BDEF/D.ttl"))
        // A + E
        Assertions.assertEquals(2, mappings[2].ids.size)
        Assertions.assertTrue(mappings[2].ids.keys.first().iriString.endsWith("$parent/AGHL/A.ttl"))
        Assertions.assertTrue(mappings[2].ids.keys.last().iriString.endsWith("$parent/BCDFJE/BDEF/E.ttl"))
        // A + B + C
        Assertions.assertEquals(3, mappings[3].ids.size)
        Assertions.assertTrue(mappings[3].ids.keys.toList()[0].iriString.endsWith("$parent/AGHL/A.ttl"))
        Assertions.assertTrue(mappings[3].ids.keys.toList()[1].iriString.endsWith("$parent/BCDFJE/BDEF/BF/B.ttl"))
        Assertions.assertTrue(mappings[3].ids.keys.toList()[2].iriString.endsWith("$parent/BCDFJE/CJ/C.ttl"))
        // G + J + H
        Assertions.assertEquals(3, mappings[4].ids.size)
        Assertions.assertTrue(mappings[4].ids.keys.toList()[0].iriString.endsWith("$parent/AGHL/G.ttl"))
        Assertions.assertTrue(mappings[4].ids.keys.toList()[1].iriString.endsWith("$parent/BCDFJE/CJ/J.ttl"))
        Assertions.assertTrue(mappings[4].ids.keys.toList()[2].iriString.endsWith("$parent/AGHL/H.ttl"))
        // L + I
        Assertions.assertEquals(2, mappings[5].ids.size)
        Assertions.assertTrue(mappings[5].ids.keys.first().iriString.endsWith("$parent/AGHL/L.ttl"))
        Assertions.assertTrue(mappings[5].ids.keys.last().iriString.endsWith("$parent/I.ttl"))
        // K
        Assertions.assertEquals(1, mappings[6].ids.size)
        Assertions.assertTrue(mappings[6].ids.keys.first().iriString.endsWith("$parent/K.ttl"))
    }

    @Test
    fun `test load directory - check graphs are independent`() {
        val dir = Path.of(LoaderTest::class.java.getResource("/simple-tree")!!.toURI())
        val mappings = loadDirectory(dir, OntFormat.TURTLE, false, ::createSoftManager)
        mappings.forEach {
            logger.debug("$it")
        }
        Assertions.assertEquals(7, mappings.size)

        // A
        val a1 = mappings[1].graphs[mappings[1].ids.keys.first()]!!
        val a2 = mappings[2].graphs[mappings[2].ids.keys.first()]!!
        val a3 = mappings[3].graphs[mappings[3].ids.keys.first()]!!
        assertNotSame(a1, a2)
        assertNotSame(a1, a3)
        assertNotSame(a3, a2)
        val t = Triple(NodeFactory.createURI("A"), NodeFactory.createURI("B"), NodeFactory.createURI("C"))
        a1.add(t)
        assertTrue(a1.contains(t))
        assertFalse(a2.contains(t))
        assertFalse(a3.contains(t))
    }
}