package com.github.sszuev.ontconverter

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.jena.vocabulary.RDF
import com.github.sszuev.ontconverter.ontapi.OntologyMap
import org.apache.jena.rdf.model.ResourceFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.semanticweb.owlapi.model.IRI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

private val logger: Logger = LoggerFactory.getLogger(ProcessorTest::class.java)

class ProcessorTest {

    @Test
    fun `test setup empty manager`() {
        val source = Path.of("A")
        val target = Path.of("A")
        val p1 = Processor(
            Args(
                force = false,
                sourceFile = source, sourceFormat = null, sourceIsDirectory = false,
                targetFile = target, targetFormat = OntFormat.RDF_XML, targetIsDirectory = true,
            )
        )
        Assertions.assertThrows(OntApiException::class.java) { p1.setup(OntManagers.createManager(), OntologyMap.of()) }
        val p2 = Processor(
            Args(
                force = true,
                sourceFile = source, sourceFormat = null, sourceIsDirectory = false,
                targetFile = target, targetFormat = OntFormat.RDF_XML, targetIsDirectory = true,
            )
        )
        Assertions.assertNull(p2.setup(OntManagers.createManager(), OntologyMap.of()))
    }

    @Test
    fun `test setup refine`() {
        val source = Path.of("A")
        val target = Path.of("A")
        val iri = IRI.create("ont1")
        val ontComment = "test-ontology"
        val ont = OntManagers.createManager().createOntology(iri)
        val gm = ont.asGraphModel()
        gm.id.addComment(ontComment)
        val clazz = gm.createOntClass("Clazz")
        val stm = gm.createStatement(
            ResourceFactory.createResource(), RDF.langRange, ResourceFactory.createResource()
        )
        gm.add(stm)
        val src = IRI.create("source")
        val map = OntologyMap.of(src to ont)

        val p1 = Processor(
            Args(
                refine = false,
                sourceFile = source, sourceFormat = null, sourceIsDirectory = false,
                targetFile = target, targetFormat = OntFormat.RDF_XML, targetIsDirectory = true,
            )
        )
        val origMan1 = OntManagers.createManager()
        val resMan1 = p1.setup(origMan1, map)!!
        Assertions.assertSame(origMan1, resMan1)
        Assertions.assertEquals(1, resMan1.ontologies.size)
        Assertions.assertSame(ont.asGraphModel().baseGraph, resMan1.getOntology(iri)?.asGraphModel()?.baseGraph)
        Assertions.assertTrue(gm.classes().anyMatch { clazz.equals(it) })
        Assertions.assertTrue(gm.contains(stm))

        val p2 = Processor(
            Args(
                refine = true,
                sourceFile = source, sourceFormat = null, sourceIsDirectory = false,
                targetFile = target, targetFormat = OntFormat.RDF_XML, targetIsDirectory = true,
            )
        )
        val origMan2 = OntManagers.createManager()
        val resMan2 = p2.setup(origMan2, map)!!
        Assertions.assertNotSame(origMan2, resMan2)
        Assertions.assertEquals(1, resMan2.ontologies.size)
        val ont2 = resMan2.getOntology(iri)!!
        val gm2 = ont2.asGraphModel()
        Assertions.assertNotSame(ont.asGraphModel().baseGraph, gm2.baseGraph)
        Assertions.assertTrue(gm2.classes().anyMatch { clazz.equals(it) })
        Assertions.assertFalse(gm2.contains(stm))
        Assertions.assertEquals(ontComment, gm2.id.comment)
    }

    @Test
    fun `test save single ontology to directory`(@TempDir dir: Path) {
        val src = Path.of("/tmp/XXX/ont.ttl").toAbsolutePath()
        val dst = dir.resolve(src.fileName.toString() + ".rdf")
        Assumptions.assumeFalse(dst.exists())
        val args = Args(
            sourceFile = src, sourceFormat = null, sourceIsDirectory = false,
            targetFile = dir, targetFormat = OntFormat.RDF_XML, targetIsDirectory = true,
        )
        logger.debug(args.toString())
        val manager = OntManagers.createManager()
        val ont = manager.createOntology(IRI.create("http://ont"))

        val p = Processor(args)
        p.save(manager, mapOf(IRI.create(src.toUri()) to ont.ontologyID))
        Assertions.assertTrue(dst.exists())
    }

    @Test
    fun `test process single file`(@TempDir dir: Path) {
        val targetFile = dir.resolve("dst.owl")
        Assumptions.assumeFalse(targetFile.exists())
        val sourceFile = Paths.get(ProcessorTest::class.java.getResource("/pizza.ttl")?.toURI() ?: Assertions.fail())
        val args = Args(
            sourceFile = sourceFile, sourceFormat = OntFormat.TURTLE, sourceIsDirectory = false,
            targetFile = targetFile, targetFormat = OntFormat.OWL_XML, targetIsDirectory = false,
        )
        logger.debug(args.toString())

        Processor(args).run()
        Assertions.assertTrue(targetFile.exists())

        var line: String
        Files.newBufferedReader(targetFile).lines().use { s ->
            line = s.findFirst().orElseThrow { AssertionError() }
        }
        Assertions.assertEquals("<?xml version=\"1.0\"?>", line)
    }
}

