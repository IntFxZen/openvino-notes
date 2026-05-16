package com.itlab.domain.usecase.noteusecase

import com.itlab.domain.model.Note
import com.itlab.domain.repository.NotesRepository
import kotlin.time.Clock

class CreateNoteUseCase(
    private val repo: NotesRepository,
    private val validateDuplicateNoteTitle: ValidateDuplicateNoteTitleUseCase,
) {
    suspend operator fun invoke(note: Note): Result<String> =
        runCatching {
            val normalizedTitle = note.title.trim()
            val hasDuplicateTitle =
                validateDuplicateNoteTitle(
                    title = normalizedTitle,
                    folderId = note.folderId,
                )
            require(!hasDuplicateTitle) { "Note with title '$normalizedTitle' already exists in this folder" }
            val now = Clock.System.now()
            val noteToPersist =
                note.copy(
                    title = normalizedTitle,
                    updatedAt = now,
                )
            repo.createNote(noteToPersist)
            noteToPersist.id
        }
}
