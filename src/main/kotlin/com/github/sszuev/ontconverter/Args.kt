package com.github.sszuev.ontconverter

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig
import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality
import java.nio.file.Path

data class Args(
    val sourceFile: Path,
    val sourceFormat: OntFormat?,
    val sourceIsDirectory: Boolean,
    val targetFile: Path,
    val targetFormat: OntFormat,
    val targetIsDirectory: Boolean,
    val punnings: OntModelConfig.StdMode = OntModelConfig.StdMode.LAX,
    val spin: Boolean = false,
    val refine: Boolean = false,
    val web: Boolean = false,
    val force: Boolean = false,
    val verbose: Boolean = false
) {

    val personality: OntPersonality
        get() = when (punnings) {
            OntModelConfig.StdMode.LAX -> OntModelConfig.ONT_PERSONALITY_LAX
            OntModelConfig.StdMode.MEDIUM -> OntModelConfig.ONT_PERSONALITY_MEDIUM
            OntModelConfig.StdMode.STRICT -> OntModelConfig.ONT_PERSONALITY_STRICT
        }

    fun printString(): String {
        return """
            |Arguments:
            |    input${(if (sourceIsDirectory) "Dir=" else "File=") + sourceFile}
            |    inputFormat=$sourceFormat
            |    output${(if (targetIsDirectory) "Dir=" else "File=") + targetFile}
            |    outputFormat=$targetFormat
            |    punnings=$punnings
            |    spin=$spin
            |    refine=$refine
            |    web=$web
            |    force=$force
            |    verbose=$verbose
            """.trimMargin()
    }
}
