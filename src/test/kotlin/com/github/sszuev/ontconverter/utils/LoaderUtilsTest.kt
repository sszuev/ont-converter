package com.github.sszuev.ontconverter.utils

import com.github.owlcs.ontapi.OntApiException
import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.OntManagers
import com.github.sszuev.ontconverter.Args
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
        val args = Args(
            sourceFile = Path.of("A"), sourceFormat = null, sourceIsDirectory = false,
            targetFile = Path.of("B"), targetFormat = OntFormat.RDF_XML, targetIsDirectory = true,
            force = true
        )
        logger.debug(args.toString())
        val m = OntManagers.createManager()
        val s = IRIDocumentSource(IRI.create("iri"))
        Assertions.assertNull(loadSource(m, s, true))
        Assertions.assertThrows(OntApiException::class.java) { loadSource(m, s, false) }
    }

}