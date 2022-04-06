package com.github.sszuev.ontconverter

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.semanticweb.owlapi.model.IRI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
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
            punnings = OntModelConfig.StdMode.STRICT,
            spin = false, refine = true, web = false, force = true, verbose = false
        )
        logger.debug(args.toString())
        val manager = OntManagers.createManager()
        val ont = manager.createOntology(IRI.create("http://ont"))

        val p = Processor(args)
        p.save(manager, mapOf(IRI.create(src.toUri()) to ont.ontologyID))
        Assertions.assertTrue(dst.exists())
    }
}

