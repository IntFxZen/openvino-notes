package com.itlab.notes.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun notesListScreen(
    directoryName: String,
    notes: List<NoteItemUi>,
    directories: List<DirectoryItemUi>,
    actions: NotesListActions,
) {
    val colors = MaterialTheme.colorScheme
    val selectedNoteIds = remember { mutableStateListOf<String>() }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isSelectionMode = selectedNoteIds.isNotEmpty()
    val selectedCount = selectedNoteIds.size
    val clearSelection = {
        selectedNoteIds.clear()
        showMoveDialog = false
        showDeleteDialog = false
    }
    val deleteSelected = {
        notes.filter { it.id in selectedNoteIds }.forEach { note ->
            actions.onNoteDelete(note)
        }
        clearSelection()
    }
    val handleBack = {
        if (isSelectionMode) {
            clearSelection()
        } else {
            actions.onBack()
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            notesTopBar(
                directoryName = directoryName,
                selectedCount = selectedCount,
                onBack = handleBack,
                onMoveSelected = { showMoveDialog = true },
                onDeleteSelected = { showDeleteDialog = true },
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                notesFab(onAddNoteClick = actions.onAddNoteClick)
            }
        },
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            notesListContent(
                notes = notes,
                paddingValues = paddingValues,
                selectedNoteIds = selectedNoteIds,
                actions =
                    NotesListContentActions(
                        onNoteDelete = actions.onNoteDelete,
                        onNoteClick = actions.onNoteClick,
                    ),
            )
            if (showMoveDialog && selectedNoteIds.isNotEmpty()) {
                notesMoveNotesDialog(
                    directories = directories,
                    onDismissRequest = { showMoveDialog = false },
                    onFolderChosen = { folderId ->
                        selectedNoteIds.forEach { noteId -> actions.onNoteMove(noteId, folderId) }
                        selectedNoteIds.clear()
                        showMoveDialog = false
                    },
                )
            }
            if (showDeleteDialog && selectedNoteIds.isNotEmpty()) {
                notesDeleteConfirmationDialog(
                    selectedCount = selectedCount,
                    onDismissRequest = { showDeleteDialog = false },
                    onConfirmDelete = deleteSelected,
                )
            }
        }
    }
}

data class NotesListActions(
    val onBack: () -> Unit,
    val onAddNoteClick: () -> Unit,
    val onNoteDelete: (NoteItemUi) -> Unit,
    val onNoteMove: (noteId: String, directoryId: String) -> Unit,
    val onNoteClick: (NoteItemUi) -> Unit,
)

private data class NotesListContentActions(
    val onNoteDelete: (NoteItemUi) -> Unit,
    val onNoteClick: (NoteItemUi) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun notesTopBar(
    directoryName: String,
    selectedCount: Int,
    onBack: () -> Unit,
    onMoveSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = if (selectedCount > 0) "$selectedCount selected" else directoryName,
                color = colors.onSurface,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector =
                        if (selectedCount > 0) {
                            Icons.Default.Close
                        } else {
                            Icons.AutoMirrored.Filled.ArrowBack
                        },
                    contentDescription = null,
                    tint = colors.onSurface,
                )
            }
        },
        actions = {
            if (selectedCount > 0) {
                IconButton(onClick = onMoveSelected) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        tint = colors.onSurface,
                    )
                }
                IconButton(onClick = onDeleteSelected) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = colors.onSurface,
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Unspecified,
                navigationIconContentColor = Color.Unspecified,
                titleContentColor = Color.Unspecified,
                actionIconContentColor = Color.Unspecified,
            ),
    )
}

@Composable
private fun notesMoveNotesDialog(
    directories: List<DirectoryItemUi>,
    onDismissRequest: () -> Unit,
    onFolderChosen: (String) -> Unit,
) {
    val moveTargets = remember(directories) { directories.filter { it.id != "all" } }
    universalBasicAlertDialog(
        onDismissRequest = onDismissRequest,
        slots =
            UniversalBasicAlertDialogSlots(
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                iconContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                iconTintColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = {
                    Text(
                        text = "Move to folder",
                        fontWeight = FontWeight.W400,
                    )
                },
                input = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(
                            items = moveTargets,
                            key = { it.id },
                        ) { dir ->
                            TextButton(
                                onClick = { onFolderChosen(dir.id) },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(
                                    text = dir.name,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = onDismissRequest,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text("Cancel")
                    }
                },
            ),
    )
}

@Composable
private fun notesDeleteConfirmationDialog(
    selectedCount: Int,
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    universalBasicAlertDialog(
        onDismissRequest = onDismissRequest,
        slots =
            UniversalBasicAlertDialogSlots(
                icon = Icons.Default.Delete,
                iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                iconTintColor = MaterialTheme.colorScheme.onErrorContainer,
                title = {
                    Text(
                        text = "Delete selected notes?",
                    )
                },
                input = {
                    Text(
                        text = "This will permanently delete $selectedCount note(s).",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                actions = {
                    TextButton(
                        onClick = onDismissRequest,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onConfirmDelete,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                    ) {
                        Text("Delete")
                    }
                },
            ),
    )
}

@Composable
private fun notesFab(onAddNoteClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FloatingActionButton(
        onClick = onAddNoteClick,
        containerColor = colors.primary,
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            tint = colors.onPrimary,
        )
    }
}

@Composable
private fun notesListContent(
    notes: List<NoteItemUi>,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    selectedNoteIds: MutableList<String>,
    actions: NotesListContentActions,
) {
    val isSelectionMode = selectedNoteIds.isNotEmpty()
    Column(
        modifier =
            Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
    ) {
        searchField()

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            items(
                items = notes,
                key = { note -> note.id },
            ) { note ->
                notesListItem(
                    note = note,
                    isSelected = note.id in selectedNoteIds,
                    onClick = {
                        if (isSelectionMode) {
                            if (note.id in selectedNoteIds) {
                                selectedNoteIds.remove(note.id)
                            } else {
                                selectedNoteIds.add(note.id)
                            }
                        } else {
                            actions.onNoteClick(note)
                        }
                    },
                    onLongClick = {
                        if (note.id !in selectedNoteIds) {
                            selectedNoteIds.add(note.id)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun notesListItem(
    note: NoteItemUi,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    noteCard(
        note = note,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun noteCard(
    note: NoteItemUi,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        colors.surfaceContainerHighest
                    } else {
                        colors.surfaceContainer
                    },
            ),
        shape = MaterialTheme.shapes.large,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = note.title,
                color =
                    if (isSelected) {
                        colors.onPrimaryContainer
                    } else {
                        colors.onSurface
                    },
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = note.content,
                color =
                    if (isSelected) {
                        colors.onPrimaryContainer
                    } else {
                        colors.onSurfaceVariant
                    },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
            )
        }
    }
}

@Composable
private fun searchField() {
    var searchQuery by remember { mutableStateOf("") }
    appSearchField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier.padding(vertical = 16.dp),
        placeholderText = "Search notes",
    )
}
