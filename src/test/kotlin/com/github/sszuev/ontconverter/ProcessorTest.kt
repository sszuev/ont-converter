package com.github.sszuev.ontconverter

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.OntManagers
import com.github.sszuev.ontconverter.ontapi.OntologyMap
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
        p.save(manager, OntologyMap.of(IRI.create(src.toUri()) to ont))
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

