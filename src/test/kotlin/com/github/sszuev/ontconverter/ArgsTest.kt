package com.github.sszuev.ontconverter

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ArgsTest {

    @Test
    fun `test print`() {
        val args1 = Args(
            sourceFile = Path.of("A"), sourceFormat = OntFormat.CSV, sourceIsDirectory = true,
            targetFile = Path.of("B"), targetFormat = OntFormat.DL, targetIsDirectory = false,
            punnings = OntModelConfig.StdMode.STRICT,
            spin = false, refine = true, web = false, force = true, verbose = false
        )
        Assertions.assertEquals(
            "Arguments:\n" +
                    "    inputDir=A\n" +
                    "    inputFormat=CSV\n" +
                    "    outputFile=B\n" +
                    "    outputFormat=DL\n" +
                    "    punnings=STRICT\n" +
                    "    spin=false\n" +
                    "    refine=true\n" +
                    "    web=false\n" +
                    "    force=true\n" +
                    "    verbose=false", args1.printString()
        )
        Assertions.assertEquals(OntModelConfig.ONT_PERSONALITY_STRICT, args1.personality)

        val args2 = Args(
            sourceFile = Path.of("C"), sourceFormat = OntFormat.KRSS, sourceIsDirectory = false,
            targetFile = Path.of("D"), targetFormat = OntFormat.RDF_XML, targetIsDirectory = true,
            punnings = OntModelConfig.StdMode.MEDIUM,
            spin = true, refine = false, web = true, force = false, verbose = true
        )
        Assertions.assertEquals(
            "Arguments:\n" +
                    "    inputFile=C\n" +
                    "    inputFormat=KRSS\n" +
                    "    outputDir=D\n" +
                    "    outputFormat=RDF_XML\n" +
                    "    punnings=MEDIUM\n" +
                    "    spin=true\n" +
                    "    refine=false\n" +
                    "    web=true\n" +
                    "    force=false\n" +
                    "    verbose=true", args2.printString()
        )
        Assertions.assertEquals(OntModelConfig.ONT_PERSONALITY_MEDIUM, args2.personality)
    }
}