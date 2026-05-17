package com.itlab.domain.usecase.noteusecase

import com.itlab.domain.repository.NoteFolderRepository
import com.itlab.domain.repository.NotesRepository
import com.itlab.domain.usecase.requireNotBlank
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

class MoveNoteToFolderUseCase(
    private val notesRepo: NotesRepository,
    private val folderRepo: NoteFolderRepository,
) {
    suspend operator fun invoke(
        folderId: String,
        noteId: String,
    ): Result<Unit> =
        runCatching {
            requireNotBlank(noteId, "Note id")
            requireNotBlank(folderId, "Folder id")
            requireNotNull(folderRepo.getFolderById(folderId)) { "Folder not found: $folderId" }
            val note = notesRepo.getNoteById(noteId) ?: throw IllegalArgumentException("Note not found: $noteId")
            val titlesInTargetFolder =
                notesRepo
                    .observeNotesByFolder(folderId)
                    .first()
                    .filter { it.id != noteId }
                    .map { it.title }
            val uniqueTitle = resolveUniqueNoteTitle(note.title, titlesInTargetFolder)
            val updated =
                note.copy(
                    folderId = folderId,
                    title = uniqueTitle,
                    updatedAt = Clock.System.now(),
                )
            notesRepo.updateNote(updated)
        }
}
