package com.github.sszuev.ontconverter

import com.github.owlcs.ontapi.OntFormat
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig
import java.nio.file.Path

data class Args(
    val inputFile: Path,
    val inputFormat: OntFormat?,
    val outputFile: Path,
    val outputFormat: OntFormat,
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
            |    inputFile=$inputFile
            |    inputFormat=$inputFormat 
            |    outputFile=$outputFile 
            |    outputFormat=$outputFormat
            |    punnings=$punnings
            |    spin=$spin
            |    refine=$refine
            |    web=$web
            |    force=$force
            |    verbose=$verbose
            """.trimMargin()
    }
}
