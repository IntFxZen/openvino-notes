@file:Suppress("TooManyFunctions")

package com.itlab.notes.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

private const val RECENT_DIRECTORY_ID = "recent"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun directoriesScreen(
    directories: List<DirectoryItemUi>,
    onCreateDirectory: (String) -> Unit,
    onDeleteDirectory: (DirectoryItemUi) -> Unit,
    onRenameDirectory: (DirectoryItemUi, String) -> Unit,
    onDirectoryClick: (DirectoryItemUi) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    focusManager.clearFocus(force = true)
                },
        containerColor = colors.background,
        topBar = {
            directoriesTopBar(
                onAddDirectoryClick = { showCreateDialog = true },
            )
        },
    ) { paddingValues ->
        directoriesList(
            directories = directories,
            onDirectoryLongClick = onDeleteDirectory,
            onDirectoryRename = onRenameDirectory,
            onDirectoryClick = onDirectoryClick,
            modifier = Modifier.padding(paddingValues),
        )
    }
    if (showCreateDialog) {
        directoriesCreateDirectoryDialog(
            onDismissRequest = { showCreateDialog = false },
            onCreateDirectory = onCreateDirectory,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun directoriesCreateDirectoryDialog(
    onDismissRequest: () -> Unit,
    onCreateDirectory: (String) -> Unit,
) {
    var directoryName by remember { mutableStateOf("") }
    universalBasicAlertDialog(
        onDismissRequest = onDismissRequest,
        slots =
            UniversalBasicAlertDialogSlots(
                icon = {
                    Box(
                        Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Icon(
                            Icons.Rounded.Folder,
                            modifier =
                                Modifier
                                    .padding(all = 14.dp)
                                    .size(32.dp),
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                title = {
                    Text(
                        text = "Create Directory",
                        fontWeight = FontWeight.W400,
                    )
                },
                input = {
                    directoryOutlinedTextField(
                        value = directoryName,
                        onValueChange = { directoryName = it },
                        placeholderText = "Enter directory name...",
                    )
                },
                actions = {
                    TextButton(
                        onClick = onDismissRequest,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = {
                            onCreateDirectory(directoryName)
                            onDismissRequest()
                        },
                        enabled = directoryName.trim().isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Create")
                    }
                },
            ),
    )
}

private data class UniversalBasicAlertDialogSlots(
    val icon: @Composable () -> Unit,
    val title: @Composable () -> Unit,
    val input: @Composable () -> Unit,
    val actions: @Composable RowScope.() -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun universalBasicAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    slots: UniversalBasicAlertDialogSlots,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier =
            modifier
                .fillMaxWidth(0.87f)
                .sizeIn(maxWidth = 560.dp),
        properties = properties,
    ) {
        val dialogFocusManager = LocalFocusManager.current
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Box {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                dialogFocusManager.clearFocus(force = true)
                            },
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    slots.icon()
                    Spacer(Modifier.height(10.dp))
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                    ) {
                        ProvideTextStyle(MaterialTheme.typography.headlineMedium) {
                            slots.title()
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    slots.input()
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        slots.actions(this)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun directoriesTopBar(onAddDirectoryClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    CenterAlignedTopAppBar(
        title = { Text("Directories", color = colors.onSurface) },
        actions = {
            IconButton(onClick = onAddDirectoryClick) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.onSurface,
                )
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

private fun Modifier.clearFocusOnTap(focusManager: FocusManager): Modifier =
    pointerInput(Unit) {
        detectTapGestures(
            onTap = { focusManager.clearFocus(force = true) },
        )
    }

@Composable
private fun directoriesList(
    directories: List<DirectoryItemUi>,
    onDirectoryLongClick: (DirectoryItemUi) -> Unit,
    onDirectoryRename: (DirectoryItemUi, String) -> Unit,
    onDirectoryClick: (DirectoryItemUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<DirectoryItemUi?>(null) }
    var pendingRename by remember { mutableStateOf<DirectoryItemUi?>(null) }
    val focusManager = LocalFocusManager.current

    val sectionData =
        remember(directories) {
            val favIds = setOf("all", "study", "cook")
            val total =
                directories.firstOrNull { it.id == "all" }?.noteCount ?: directories.sumOf { it.noteCount }
            val favs = directories.filter { it.id in favIds }
            val regs = directories.filterNot { it.id in favIds }
            val recent = DirectoryItemUi(id = RECENT_DIRECTORY_ID, name = "Recent", noteCount = total)
            Triple(favs, regs, recent)
        }

    Column(
        modifier = modifier.fillMaxWidth().clearFocusOnTap(focusManager).padding(horizontal = 12.dp),
    ) {
        directorySearchBar()
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            fun LazyListScope.addSection(
                title: String,
                dirs: List<DirectoryItemUi>,
                isRegularBlock: Boolean,
            ) {
                if (dirs.isEmpty()) return
                item { sectionTitle(title = title) }
                item {
                    directoriesBlock(
                        directories = dirs,
                        isRegularDirectoriesBlock = isRegularBlock,
                        onDirectoryClick = onDirectoryClick,
                        onDirectoryLongClick = { pendingDelete = it },
                    )
                }
            }

            item {
                directoriesHeroPanel(
                    directoriesCount = directories.count { it.id != "all" },
                    totalNotesCount = sectionData.third.noteCount,
                )
            }
            addSection("Continue working", listOf(sectionData.third), true)
            addSection("Favorite directories", sectionData.first, false)
            addSection("Regular directories", sectionData.second, true)
        }
    }

    pendingDelete?.let { dir ->
        directoryActionsDialog(
            directory = dir,
            onDelete = {
                onDirectoryLongClick(dir)
                pendingDelete = null
            },
            onRename = {
                pendingDelete = null
                pendingRename = dir
            },
            onDismiss = { pendingDelete = null },
        )
    }
    pendingRename?.let { dir ->
        directoryRenameDialog(
            directory = dir,
            onSave = { newName ->
                onDirectoryRename(dir, newName)
                pendingRename = null
            },
            onDismiss = { pendingRename = null },
        )
    }
}

@Composable
private fun directoriesHeroPanel(
    directoriesCount: Int,
    totalNotesCount: Int,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.FolderCopy,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(25.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Workspace",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = "$totalNotesCount notes • $directoriesCount directories",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun sectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 6.dp),
    )
}

@Composable
private fun directoriesBlock(
    directories: List<DirectoryItemUi>,
    isRegularDirectoriesBlock: Boolean,
    onDirectoryClick: (DirectoryItemUi) -> Unit,
    onDirectoryLongClick: (DirectoryItemUi) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            directories.forEachIndexed { index, dir ->
                directoryRow(
                    directory = dir,
                    isRegularDirectory = isRegularDirectoriesBlock,
                    onClick = {
                        if (!isSpecialDirectory(dir.id)) {
                            onDirectoryClick(dir)
                        }
                    },
                    onLongClick = {
                        if (!isSpecialDirectory(dir.id)) {
                            onDirectoryLongClick(dir)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp),
                )
                if (index < directories.lastIndex) {
                    directoriesListDivider()
                }
            }
        }
    }
}

@Composable
fun directorySearchBar(
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    appSearchField(
        value = searchQuery,
        onValueChange = {
            searchQuery = it
            onQueryChange(it)
        },
        modifier = modifier,
        placeholderText = "Search directories",
    )
}

private fun isSpecialDirectory(directoryId: String): Boolean =
    directoryId == "all" || directoryId == RECENT_DIRECTORY_ID

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun directoryRow(
    directory: DirectoryItemUi,
    isRegularDirectory: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector =
                when {
                    directory.id == RECENT_DIRECTORY_ID -> Icons.Default.AccessTimeFilled
                    isRegularDirectory -> Icons.Default.Folder
                    else -> Icons.Default.Stars
                },
            contentDescription = null,
            tint = if (isRegularDirectory) colors.onSurfaceVariant else colors.primary,
            modifier = Modifier.size(25.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = directory.name,
            color = colors.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Surface(
            color = colors.surfaceVariant,
            shape = CircleShape,
        ) {
            Text(
                text = directory.noteCount.toString(),
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.width(5.dp))
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun directoriesListDivider() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth(0.9f),
            contentAlignment = Alignment.Center,
        ) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                thickness = 1.dp,
            )
        }
    }
}

@Composable
private fun directoryActionsDialog(
    directory: DirectoryItemUi,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onDismiss: () -> Unit,
) {
    universalBasicAlertDialog(
        onDismissRequest = onDismiss,
        slots =
            UniversalBasicAlertDialogSlots(
                icon = {
                    Box(
                        Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            modifier =
                                Modifier
                                    .padding(all = 14.dp)
                                    .size(32.dp),
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                title = {
                    Text("Directory actions")
                },
                input = {
                    Text(
                        "Choose action for \"${directory.name}\"",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                actions = {
                    TextButton(onClick = onRename) {
                        Text("Rename")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = onDelete,
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
private fun directoryRenameDialog(
    directory: DirectoryItemUi,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var renameName by remember(directory.id) { mutableStateOf(directory.name) }
    universalBasicAlertDialog(
        onDismissRequest = onDismiss,
        slots =
            UniversalBasicAlertDialogSlots(
                icon = {
                    Box(
                        Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            modifier =
                                Modifier
                                    .padding(all = 14.dp)
                                    .size(32.dp),
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                title = {
                    Text("Rename directory")
                },
                input = {
                    directoryOutlinedTextField(
                        value = renameName,
                        onValueChange = { renameName = it },
                        placeholderText = "Enter directory name...",
                    )
                },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = { onSave(renameName) },
                        enabled = renameName.trim().isNotEmpty() && renameName != directory.name,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Save")
                    }
                },
            ),
    )
}

@Composable
private fun directoryOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholderText) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.TextFields,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.medium,
        suffix = {
            Icons.Default.TextFields
        },
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContainerColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.outline,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                disabledIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            ),
    )
}
