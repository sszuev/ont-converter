package com.github.sszuev.ontconverter.api.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DependenciesTest {

    @Test
    fun `test find independent components - simple graph 1`() {
        val graph = mapOf(
            "a" to listOf(),
            "b" to listOf("a", "c"),
            "c" to listOf("d", "e", "f"),
            "g" to listOf(),
            "x" to listOf(),
            "h" to listOf("y")
        )

        val actual = findIndependentComponents(graph)
        val expected = listOf(setOf("a", "d", "e", "f", "c", "b"), setOf("g"), setOf("x"), setOf("y", "h"))
        assertEquals(expected, actual)
    }

    @Test
    fun `test find independent components - simple graph 2`() {
        val graph = mapOf(
            "d" to listOf("c"),
            "a" to listOf("b", "d", "e"),
            "e" to listOf("c", "f"),
            "g" to listOf("k", "h"),
            "h" to listOf("f")
        )

        val actual = findIndependentComponents(graph)
        val expected = listOf(setOf("b", "c", "d", "f", "e", "a"), setOf("k", "f", "h", "g"))
        assertEquals(expected, actual)
    }

    @Test
    fun `test find independent components - complex looped graph`() {
        val graph = mapOf(
            "G" to listOf("F", "C", "H"),
            "F" to listOf("G"),
            "C" to listOf("D"),
            "H" to listOf("D"),
            "D" to listOf("C", "H"),
            "A" to listOf("B"),
            "B" to listOf("C", "F", "E"),
            "E" to listOf("A", "F"),
        )
        val actual = findIndependentComponents(graph)
        val expected = listOf(setOf("G", "F", "C", "H", "D", "A", "B", "E"))
        assertEquals(expected, actual)
    }

    @Test
    fun `test find independent components - independent graphs`() {
        val graph = mapOf(
            "f" to listOf<String>(),
            "c" to listOf(),
            "j" to listOf(),
            "q" to listOf(),
            "x" to listOf(),
            "v" to listOf(),
            "w" to listOf(),
        )
        val actual = findIndependentComponents(graph)
        val expected = listOf(setOf("f"), setOf("c"), setOf("j"), setOf("q"), setOf("x"), setOf("v"), setOf("w"))
        assertEquals(expected, actual)

    }

    @Test
    fun `test find independent components - simple looped graph`() {
        val graph = mapOf(
            "x" to listOf("y"),
            "a" to listOf("b"),
            "b" to listOf("c"),
            "c" to listOf("a")
        )
        val actual = findIndependentComponents(graph)
        val expected = listOf(setOf("y", "x"), setOf("a", "b", "c"))
        assertEquals(expected, actual)
    }
}

private fun <X> assertEquals(expected: List<Set<X>>, actual: List<Set<X>>) {
    Assertions.assertEquals(toListList(expected), toListList(actual))
}

private fun <X> toListList(sets: List<Set<X>>): List<List<X>> {
    return sets.map { it.toList() }.toList()
}

