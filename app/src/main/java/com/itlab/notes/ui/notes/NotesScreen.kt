package com.itlab.notes.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun notesListScreen(
    directoryName: String,
    notes: List<NoteItemUi>,
    actions: NotesListActions,
) {
    val colors = MaterialTheme.colorScheme
    val selectedNoteIds = remember { mutableStateListOf<String>() }
    val isSelectionMode = selectedNoteIds.isNotEmpty()
    val selectedCount = selectedNoteIds.size
    val clearSelection = { selectedNoteIds.clear() }
    val deleteSelected = {
        notes.filter { it.id in selectedNoteIds }.forEach { note ->
            actions.onNoteDelete(note)
        }
        selectedNoteIds.clear()
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
                onDeleteSelected = deleteSelected,
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                notesFab(onAddNoteClick = actions.onAddNoteClick)
            }
        },
    ) { paddingValues ->
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
