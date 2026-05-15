package com.itlab.domain.usecase.noteusecase

import com.itlab.domain.repository.NotesRepository
import kotlinx.coroutines.flow.first

class ValidateDuplicateNoteTitleUseCase(
    private val repo: NotesRepository,
) {
    /**
     * @return `true` if another note in the same folder already has this title.
     */
    suspend operator fun invoke(
        title: String,
        folderId: String?,
        excludeNoteId: String? = null,
    ): Boolean {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) return false
        return repo.observeNotes().first().any { existing ->
            (excludeNoteId == null || existing.id != excludeNoteId) &&
                existing.folderId == folderId &&
                existing.title.trim().equals(normalizedTitle, ignoreCase = true)
        }
    }
}
