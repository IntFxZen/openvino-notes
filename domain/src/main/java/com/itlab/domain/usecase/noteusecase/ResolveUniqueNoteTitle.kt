package com.itlab.domain.usecase.noteusecase

/**
 * Picks a title that does not collide with [existingTitles] in the same folder (case-insensitive).
 * If [desiredTitle] is taken, returns `"$base (1)"`, `"$base (2)"`, …
 */
@Suppress("ReturnCount")
fun resolveUniqueNoteTitle(
    desiredTitle: String,
    existingTitles: Iterable<String>,
): String {
    val base = desiredTitle.trim().ifBlank { return resolveUniqueNoteTitle("Untitled", existingTitles) }
    val taken =
        existingTitles
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    fun isTaken(title: String): Boolean = taken.any { it.equals(title, ignoreCase = true) }

    if (!isTaken(base)) return base

    var index = 1
    while (isTaken("$base ($index)")) {
        index++
    }
    return "$base ($index)"
}
