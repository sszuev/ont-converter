package com.github.sszuev.ontconverter.utils

import org.semanticweb.owlapi.model.IRI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

private val logger: Logger = LoggerFactory.getLogger("Files.kt")

private val byPathLengthReversed = Comparator.comparingInt { obj: Path -> obj.nameCount }.reversed()

/**
 * Walks through directory, the output sequence are sorted in descending order of path length.
 *
 * @param [dir][Path]
 * @param [ignoreExceptions][Boolean]
 * @return [Sequence] of [IRI]s
 */
fun listFiles(dir: Path, ignoreExceptions: Boolean): Sequence<IRI> {
    return Files.walk(dir).asSequence()
        .filter {
            isRegularNonEmptyFile(it, ignoreExceptions)
        }
        .sortedWith(byPathLengthReversed)
        .map { IRI.create(it.toUri()) }
}

private fun isRegularNonEmptyFile(file: Path, ignoreExceptions: Boolean): Boolean {
    return try {
        file.isRegularFile() && file.fileSize() > 0L
    } catch (ex: IOException) {
        if (!ignoreExceptions) {
            throw ex
        }
        logger.error("Can't access file $file", ex)
        false
    }
}
