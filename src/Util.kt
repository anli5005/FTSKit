package dev.anli.ftskit

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