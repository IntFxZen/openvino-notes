package com.itlab.domain.usecase.noteusecase

import com.itlab.domain.model.ContentItem
import com.itlab.domain.repository.NotesRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import kotlin.time.Clock

class DuplicateNoteUseCase(
    private val repo: NotesRepository,
) {
    suspend operator fun invoke(noteId: String): Result<String> =
        runCatching {
            val note =
                repo.getNoteById(noteId)
                    ?: throw IllegalArgumentException("Note not found: $noteId")

            val folderId = note.folderId
            val existingTitles =
                if (folderId != null) {
                    repo.observeNotesByFolder(folderId).first().map { it.title }
                } else {
                    repo.observeNotes().first().map { it.title }
                }
            val baseTitle = note.title.trim().ifBlank { "Copy" }
            val uniqueTitle = resolveUniqueNoteTitle(baseTitle, existingTitles)

            val now = Clock.System.now()
            val duplicated =
                note.copy(
                    id = UUID.randomUUID().toString(),
                    title = uniqueTitle,
                    createdAt = now,
                    updatedAt = now,
                    contentItems =
                        note.contentItems.map { item ->
                            when (item) {
                                is ContentItem.Text -> item.copy(id = UUID.randomUUID().toString())
                                is ContentItem.Image -> item.copy(id = UUID.randomUUID().toString())
                                is ContentItem.File -> item.copy(id = UUID.randomUUID().toString())
                                is ContentItem.Link -> item.copy(id = UUID.randomUUID().toString())
                            }
                        },
                )
            repo.createNote(duplicated)
        }
}
