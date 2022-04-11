package com.github.sszuev.ontconverter.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.writeText

private val logger: Logger = LoggerFactory.getLogger(FilesUtilsTest::class.java)

class FilesUtilsTest {

    @Test
    fun `test list files`(@TempDir dir: Path) {
        val parent = dir.resolve("for-test").createDirectory()
        val dir1 = parent.resolve("dir1").createDirectory()
        val dir2 = parent.resolve("dir2").createDirectory()
        val dir3 = dir1.resolve("dir3").createDirectory()

        parent.resolve("file1").createFile()
        val file2 = parent.resolve("file2").createFile()
        dir3.resolve("file3").createFile()
        val file4 = dir2.resolve("file3").createFile()

        file4.writeText("test4")
        file2.writeText("test2")

        val res = listFiles(parent, true).onEach { logger.debug("file=$it") }.toList()
        Assertions.assertEquals(2, res.size)
        Assertions.assertEquals("file", res[0].scheme)
        Assertions.assertEquals("file", res[1].scheme)
        Assertions.assertTrue(res[0].iriString.endsWith("/for-test/dir2/file3"))
        Assertions.assertTrue(res[1].iriString.endsWith("/for-test/file2"))
    }
}