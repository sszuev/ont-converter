package com.github.sszuev.ontconverter.api.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DependenciesTest {

    @Test
    fun `test find independent components`() {
        val graph1 = mapOf(
            "a" to listOf(),
            "b" to listOf("a", "c"),
            "c" to listOf("d", "e", "f"),
            "g" to listOf(),
            "x" to listOf(),
            "h" to listOf("y")
        )

        val actual1 = findIndependentComponents(graph1)
        val expected1 = listOf(setOf("a", "d", "e", "f", "c", "b"), setOf("g"), setOf("x"), setOf("y", "h"))
        Assertions.assertEquals(expected1, actual1)

        val graph2 = mapOf(
            "G" to listOf("F", "C", "H"),
            "F" to listOf("G"),
            "C" to listOf("D"),
            "H" to listOf("D"),
            "D" to listOf("C", "H"),
            "A" to listOf("B"),
            "B" to listOf("C", "F", "E"),
            "E" to listOf("A", "F"),
        )
        val actual2 = findIndependentComponents(graph2)
        val expected2 = listOf(setOf("A", "B", "C", "D", "E", "F", "G", "H"))
        Assertions.assertEquals(expected2, actual2)

        val graph3 = mapOf(
            "f" to listOf<String>(),
            "c" to listOf(),
            "j" to listOf(),
            "q" to listOf(),
            "x" to listOf(),
            "v" to listOf(),
            "w" to listOf(),
        )
        val actual3 = findIndependentComponents(graph3)
        val expected3 = listOf(setOf("f"), setOf("c"), setOf("j"), setOf("q"), setOf("x"), setOf("v"), setOf("w"))
        Assertions.assertEquals(expected3, actual3)

        val graph4 = mapOf(
            "x" to listOf("y"),
            "a" to listOf("b"),
            "b" to listOf("c"),
            "c" to listOf("a")
        )
        val actual4 = findIndependentComponents(graph4)
        val expected4 = listOf(setOf("x", "y"), setOf("a", "b", "c"))
        Assertions.assertEquals(expected4, actual4)
    }
}