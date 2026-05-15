package com.itlab.domain.usecase.noteusecase

import com.itlab.domain.model.Note
import com.itlab.domain.repository.NotesRepository
import kotlin.time.Clock

class UpdateNoteUseCase(
    private val repo: NotesRepository,
    private val validateDuplicateNoteTitle: ValidateDuplicateNoteTitleUseCase,
) {
    suspend operator fun invoke(note: Note): Result<Unit> =
        runCatching {
            val normalizedTitle = note.title.trim()
            val hasDuplicateTitle =
                validateDuplicateNoteTitle(
                    title = normalizedTitle,
                    folderId = note.folderId,
                    excludeNoteId = note.id,
                )
            require(!hasDuplicateTitle) { "Note with title '$normalizedTitle' already exists in this folder" }
            val note = note.copy(updatedAt = Clock.System.now())
            repo.updateNote(note)
        }
}
