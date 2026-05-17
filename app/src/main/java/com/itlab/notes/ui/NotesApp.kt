package com.itlab.notes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.itlab.notes.ui.auth.AuthViewModel
import com.itlab.notes.ui.auth.authScreen
import com.itlab.notes.ui.editor.editorScreen
import com.itlab.notes.ui.filterDirectoriesByName
import com.itlab.notes.ui.notes.NotesListActions
import com.itlab.notes.ui.notes.directoriesScreen
import com.itlab.notes.ui.notes.notesListScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun notesApp() {
    val authViewModel: AuthViewModel = koinViewModel()
    val authState by authViewModel.uiState.collectAsState()
    if (!authState.isSignedIn && !authState.continueOffline) {
        authScreen(authViewModel)
        return
    }
    notesMain()
}

@Composable
private fun notesMain() {
    val viewModel: NotesViewModel = koinViewModel()
    val state = viewModel.uiState

    when (val screen = state.screen) {
        NotesUiScreen.Directories -> {
            directoriesScreen(
                directories =
                    filterDirectoriesByName(
                        directories = state.directories,
                        query = state.directorySearchQuery,
                    ),
                searchQuery = state.directorySearchQuery,
                onSearchQueryChange = { query ->
                    viewModel.onEvent(NotesUiEvent.DirectorySearchQueryChanged(query))
                },
                onCreateDirectory = { name ->
                    viewModel.onEvent(NotesUiEvent.CreateDirectory(name))
                },
                onDeleteDirectory = { directory ->
                    viewModel.onEvent(NotesUiEvent.DeleteDirectory(directory.id))
                },
                onRenameDirectory = { directory, newName ->
                    viewModel.onEvent(NotesUiEvent.RenameDirectory(directory.id, newName))
                },
                onDirectoryClick = { directory ->
                    viewModel.onEvent(NotesUiEvent.OpenDirectory(directory))
                },
            )
        }

        is NotesUiScreen.DirectoryNotes -> {
            notesListScreen(
                directoryId = screen.directory.id,
                directoryName = screen.directory.name,
                notes = state.notes,
                searchQuery = state.notesSearchQuery,
                onSearchQueryChange = { query ->
                    viewModel.onEvent(NotesUiEvent.NotesSearchQueryChanged(query))
                },
                directories = state.directories,
                actions =
                    NotesListActions(
                        onBack = { viewModel.onEvent(NotesUiEvent.BackToDirectories) },
                        onAddNoteClick = { viewModel.onEvent(NotesUiEvent.CreateNote) },
                        onNoteDelete = { note -> viewModel.onEvent(NotesUiEvent.DeleteNote(note.id)) },
                        onNoteMove = { noteId, directoryId ->
                            viewModel.onEvent(
                                NotesUiEvent.MoveNoteToDirectory(
                                    noteId = noteId,
                                    targetDirectoryId = directoryId,
                                ),
                            )
                        },
                        onNoteClick = { note ->
                            viewModel.onEvent(NotesUiEvent.OpenNote(note))
                        },
                    ),
            )
        }

        is NotesUiScreen.NoteEditor -> {
            editorScreen(
                directoryName = screen.directory.name,
                directoryId = screen.directory.id,
                note = screen.note,
                onBack = { draft -> viewModel.onEvent(NotesUiEvent.LeaveEditor(draft)) },
                onPersist = { draft ->
                    viewModel.onEvent(NotesUiEvent.PersistNote(draft))
                },
                onSave = { updated ->
                    viewModel.onEvent(NotesUiEvent.SaveNote(updated))
                },
                onToggleFavorite = {
                    viewModel.onEvent(NotesUiEvent.ToggleNoteFavorite(screen.note.id))
                },
            )
        }
    }
}
