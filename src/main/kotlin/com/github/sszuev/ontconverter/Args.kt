package com.github.sszuev.ontconverter

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig
import java.nio.file.Path

data class Args(
    val sourceFile: Path,
    val sourceFormat: OntFormat?,
    val sourceIsDirectory: Boolean,
    val targetFile: Path,
    val targetFormat: OntFormat,
    val targetIsDirectory: Boolean,
    val punnings: OntModelConfig.StdMode,
    val spin: Boolean,
    val refine: Boolean,
    val web: Boolean,
    val force: Boolean,
    val verbose: Boolean
) {

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
