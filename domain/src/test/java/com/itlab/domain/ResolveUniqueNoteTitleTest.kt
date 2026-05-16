package com.itlab.domain

import com.itlab.domain.usecase.noteusecase.resolveUniqueNoteTitle
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveUniqueNoteTitleTest {
    @Test
    fun returnsDesiredTitleWhenNoConflict() {
        assertEquals(
            "Meeting",
            resolveUniqueNoteTitle("Meeting", listOf("Other")),
        )
    }

    @Test
    fun appendsIncrementingSuffixWhenBaseTaken() {
        assertEquals(
            "Meeting (1)",
            resolveUniqueNoteTitle("Meeting", listOf("Meeting")),
        )
        assertEquals(
            "Meeting (2)",
            resolveUniqueNoteTitle("Meeting", listOf("Meeting", "Meeting (1)")),
        )
    }

    @Test
    fun isCaseInsensitive() {
        assertEquals(
            "Meeting (1)",
            resolveUniqueNoteTitle("Meeting", listOf("meeting")),
        )
    }

    @Test
    fun blankTitleUsesUntitled() {
        assertEquals(
            "Untitled",
            resolveUniqueNoteTitle("   ", emptyList()),
        )
        assertEquals(
            "Untitled (1)",
            resolveUniqueNoteTitle("", listOf("Untitled")),
        )
    }
}
