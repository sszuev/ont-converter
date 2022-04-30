package com.github.sszuev.ontconverter.api.utils

/**
 * Finds independent components using depth-first-search.
 *
 * @param X - anything
 * @param [graph] directed graph in form of [Map] of [Collection] of [X]
 * @return [List] of [Set] of [X], where item-set is a component
 */
fun <X> findIndependentComponents(graph: Map<X, Collection<X>>): List<Set<X>> {
    val res: MutableList<Set<X>> = ArrayList()
    var root: X? = firstRootOrNull(graph)
    if (root == null) {
        res.add(toFlatSet(graph))
        return res
    }
    val map: MutableMap<X, Collection<X>> = graph.toMutableMap()
    while (map.isNotEmpty()) {
        if (root == null) {
            res.add(toFlatSet(map))
            break
        }
        val vertexes: MutableSet<X> = LinkedHashSet()
        dfs(root, graph, HashSet()) { x: X ->
            vertexes.add(x)
            map.remove(x)
        }
        res.add(vertexes)
        root = firstRootOrNull(map)
    }
    return res
}

/**
 * Performs the Depth-First Search on the given graph (recursive implementation).
 *
 * @param X - anything
 * @param [vertex]
 * @param [graph]
 * @see [wiki: DFS](https://en.wikipedia.org/wiki/Depth-first_search)
 */
private fun <X> dfs(
    vertex: X,
    graph: Map<X, Collection<X>>,
    seen: MutableSet<X>,
    block: (x: X) -> Unit
) {
    if (!seen.add(vertex)) {
        return
    }
    val adjacent = graph[vertex]
    adjacent?.forEach { u: X -> dfs(u, graph, seen, block) }
    block.invoke(vertex)
}

/**
 * @param [graph] directed graph in form of [Map] of [Collection] of [X]
 * @return [X] or `null` - first root
 */
private fun <X> firstRootOrNull(graph: Map<X, Collection<X>>): X? {
    for (key in graph.keys) {
        if (graph.values.none { v: Collection<X> -> v.contains(key) }) {
            return key
        }
    }
    return null
}

/**
 * [Map] of [Collection] -> [Set]
 */
private fun <X> toFlatSet(graph: Map<X, Collection<X>>): Set<X> {
    val res: MutableSet<X> = LinkedHashSet()
    graph.forEach { (k: X, values: Collection<X>) ->
        res.add(k)
        res.addAll(values)
    }
    return res
}