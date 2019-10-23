package dev.anli.ftskit

import kotlin.math.roundToInt

internal fun parseRow(row: String): List<String> {
    return row
            .replace(Regex("<tr[^>]*>"), "")
            .replace(Regex("<td[^>]*>"), "")
            .replace("</tr>", "")
            .split("</td>")
            .dropLast(1)
}

internal fun parseTable(table: String): List<List<String>> {
    return table.split("</tr>").map { parseRow(it) }.dropLast(1)
}

fun <E>Collection<E>.indexOfOrNull(element: E): Int? {
    val index = indexOf(element)
    return if (index < 0) null else index
}

fun String.toIntBy100() = (toDouble() * 100).roundToInt()
fun String.removing(str: String) = replace(str, "")