package com.itlab.notes.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itlab.domain.model.ContentItem
import com.itlab.domain.model.Note
import com.itlab.domain.model.NoteFolder
import com.itlab.notes.ui.notes.DirectoryItemUi
import com.itlab.notes.ui.notes.NoteItemUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class NotesViewModel(
    private val useCases: NotesUseCases,
) : ViewModel(),
    NotesViewModelContract {
    override var uiState: NotesUiState by mutableStateOf(
        NotesUiState(screen = NotesUiScreen.Directories),
    )
        private set
    private var notesJob: Job? = null

    init {
        viewModelScope.launch {
            useCases.observeFoldersUseCase().collect { folders ->
                val existingCounts = uiState.directories.associate { it.id to it.noteCount }
                uiState =
                    uiState.copy(
                        directories =
                            folders.map { folder ->
                                folder.toUi(noteCount = existingCounts[folder.id] ?: 0)
                            },
                    )
            }
        }
    }

    override fun onEvent(event: NotesUiEvent) {
        when (event) {
            is NotesUiEvent.OpenDirectory -> openDirectory(event.directory)
            NotesUiEvent.BackToDirectories -> backToDirectories()
            is NotesUiEvent.OpenNote -> openNote(event.note)
            NotesUiEvent.CreateNote -> createNote()
            is NotesUiEvent.CreateDirectory -> {
                val normalized = event.name.trim()
                if (normalized.isNotBlank()) {
                    viewModelScope.launch {
                        useCases.createFolderUseCase(NoteFolder(name = normalized))
                    }
                }
            }
            is NotesUiEvent.RenameDirectory -> renameDirectory(event)
            is NotesUiEvent.DeleteDirectory -> deleteDirectory(event.directoryId)
            is NotesUiEvent.MoveNoteToDirectory -> {
                if (event.targetDirectoryId == "all") return
                viewModelScope.launch {
                    useCases.moveNoteToFolderUseCase(
                        folderId = event.targetDirectoryId,
                        noteId = event.noteId,
                    )
                }
            }
            NotesUiEvent.BackToDirectoryNotes -> backToDirectoryNotes()
            is NotesUiEvent.SaveNote -> saveNote(event.note)
            is NotesUiEvent.DeleteNote -> {
                viewModelScope.launch {
                    useCases.deleteNoteUseCase(event.noteId)
                }
            }
        }
    }

    private fun renameDirectory(event: NotesUiEvent.RenameDirectory) {
        val normalized = event.newName.trim()
        if (normalized.isBlank() || event.directoryId == "all") return
        viewModelScope.launch {
            val existingFolder = useCases.getFolderUseCase(event.directoryId) ?: return@launch
            useCases.updateFolderUseCase(existingFolder.copy(name = normalized))
        }
    }

    private fun deleteDirectory(directoryId: String) {
        if (directoryId == "all") return
        viewModelScope.launch {
            val notesInDirectory =
                useCases
                    .observeNotesByFolderUseCase(directoryId)
                    .firstOrNull()
                    .orEmpty()
            notesInDirectory.forEach { note ->
                useCases.deleteNoteUseCase(note.id)
            }
            useCases.deleteFolderUseCase(directoryId)
            if ((uiState.screen as? NotesUiScreen.DirectoryNotes)?.directory?.id == directoryId) {
                backToDirectories()
            }
        }
    }

    private fun openDirectory(directory: DirectoryItemUi) {
        uiState =
            uiState.copy(
                screen = NotesUiScreen.DirectoryNotes(directory = directory),
                notes = emptyList(),
            )
        notesJob?.cancel()
        val isAll = directory.id == "all"
        notesJob =
            viewModelScope.launch {
                val flow =
                    if (isAll) {
                        useCases.observeNotesUseCase()
                    } else {
                        useCases.observeNotesByFolderUseCase(directory.id)
                    }

                flow.collect { notes ->
                    uiState =
                        uiState.copy(
                            notes = notes.map { it.toUi() },
                            screen =
                                NotesUiScreen.DirectoryNotes(
                                    directory = directory.copy(noteCount = notes.size),
                                ),
                        )
                }
            }
    }

    private fun backToDirectories() {
        notesJob?.cancel()
        uiState =
            uiState.copy(
                screen = NotesUiScreen.Directories,
                notes = emptyList(),
            )
    }

    private fun openNote(note: NoteItemUi) {
        val dir = (uiState.screen as? NotesUiScreen.DirectoryNotes)?.directory
        if (dir != null) {
            uiState =
                uiState.copy(
                    screen = NotesUiScreen.NoteEditor(directory = dir, note = note),
                )
        }
    }

    private fun createNote() {
        val dir = (uiState.screen as? NotesUiScreen.DirectoryNotes)?.directory
        if (dir != null) {
            val newNote =
                NoteItemUi(
                    id = UUID.randomUUID().toString(),
                    title = "",
                    content = "",
                    folderId = dir.id.asDomainFolderId(),
                )
            uiState =
                uiState.copy(
                    screen = NotesUiScreen.NoteEditor(directory = dir, note = newNote),
                )
        }
    }

    private fun backToDirectoryNotes() {
        val editor = uiState.screen as? NotesUiScreen.NoteEditor
        if (editor != null) {
            uiState = uiState.copy(screen = NotesUiScreen.DirectoryNotes(directory = editor.directory))
        }
    }

    private fun saveNote(note: NoteItemUi) {
        val editor = uiState.screen as? NotesUiScreen.NoteEditor ?: return
        viewModelScope.launch {
            val existing =
                useCases
                    .observeNotesUseCase()
                    .firstOrNull()
                    .orEmpty()
                    .any { it.id == note.id }
            val targetFolderId = note.folderId ?: editor.directory.id.asDomainFolderId()
            val domainNote = note.toDomain(folderId = targetFolderId)
            if (existing) {
                useCases.updateNoteUseCase(domainNote)
            } else {
                useCases.createNoteUseCase(domainNote)
            }
            uiState = uiState.copy(screen = NotesUiScreen.DirectoryNotes(directory = editor.directory))
        }
    }

    override fun onCleared() {
        notesJob?.cancel()
        super.onCleared()
    }
}

internal fun NoteFolder.toUi(noteCount: Int): DirectoryItemUi =
    DirectoryItemUi(id = id, name = name, noteCount = noteCount)

internal fun Note.toUi(): NoteItemUi =
    NoteItemUi(
        id = id,
        title = title,
        content =
            contentItems
                .filterIsInstance<ContentItem.Text>()
                .joinToString("\n") { it.text },
        folderId = folderId,
    )

internal fun NoteItemUi.toDomain(folderId: String?): Note =
    Note(
        id = id,
        title = title,
        folderId = folderId,
        contentItems = listOf(ContentItem.Text(content)),
    )

internal fun String.asDomainFolderId(): String? = if (this == "all") null else this
