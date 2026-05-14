package com.itlab.notes.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.itlab.domain.model.ContentItem
import com.itlab.domain.model.DataSource
import com.itlab.notes.media.NoteMediaImport
import com.itlab.notes.ui.notes.NoteItemUi
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editorScreen(
    directoryName: String,
    note: NoteItemUi,
    onBack: () -> Unit,
    onSave: (NoteItemUi) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val editorVm = remember(note.id) { EditorViewModel(initialNote = note) }
    var fullscreenImage by remember { mutableStateOf<ContentItem.Image?>(null) }

    val pickImage =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching { NoteMediaImport.importImageFromUri(context, uri) }
                .onSuccess { editorVm.addAttachment(it) }
        }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            editorTopBar(
                directoryName = directoryName,
                title = editorVm.title,
                onBack = onBack,
                onAddImage = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
        },
        floatingActionButton = {
            editorFab(
                onClick = { onSave(editorVm.buildUpdatedNote()) },
            )
        },
    ) { paddingValues ->
        editorContent(
            title = editorVm.title,
            content = editorVm.content,
            attachments = editorVm.attachments,
            onTitleChange = editorVm::onTitleChange,
            onContentChange = editorVm::onContentChange,
            onImageClick = { fullscreenImage = it },
            onRemoveAttachment = { item ->
                if (item is ContentItem.Image) {
                    NoteMediaImport.deleteImportedFileIfOwned(context, item.source.localPath)
                }
                editorVm.removeAttachment(item.id)
            },
            modifier = Modifier.padding(paddingValues),
        )
    }

    fullscreenImage?.let { image ->
        editorFullScreenImageViewer(
            image = image,
            onDismiss = { fullscreenImage = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun editorTopBar(
    directoryName: String,
    title: String,
    onBack: () -> Unit,
    onAddImage: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = if (title.isBlank()) directoryName else title,
                color = colors.onSurface,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colors.onSurface,
                )
            }
        },
        actions = {
            IconButton(onClick = onAddImage) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Add image",
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

@Composable
private fun editorFab(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FloatingActionButton(
        onClick = onClick,
        containerColor = colors.primary,
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = colors.onPrimary,
        )
    }
}

@Composable
private fun editorContent(
    title: String,
    content: String,
    attachments: List<ContentItem>,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onImageClick: (ContentItem.Image) -> Unit,
    onRemoveAttachment: (ContentItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        editorTitleField(
            value = title,
            onValueChange = onTitleChange,
        )

        editorContentField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier.padding(top = 12.dp),
        )

        if (attachments.isNotEmpty()) {
            editorAttachmentsRow(
                attachments = attachments,
                onImageClick = onImageClick,
                onRemove = onRemoveAttachment,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun editorAttachmentsRow(
    attachments: List<ContentItem>,
    onImageClick: (ContentItem.Image) -> Unit,
    onRemove: (ContentItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(
            items = attachments,
            key = { it.id },
        ) { item ->
            when (item) {
                is ContentItem.Image -> editorImageThumbnail(item, onImageClick, onRemove)
                is ContentItem.File ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier =
                            Modifier
                                .height(88.dp)
                                .width(120.dp),
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 3,
                                modifier =
                                    Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 8.dp, vertical = 20.dp),
                            )
                            IconButton(
                                onClick = { onRemove(item) },
                                modifier = Modifier.align(Alignment.TopEnd),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    }
                is ContentItem.Link ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier =
                            Modifier
                                .height(88.dp)
                                .width(120.dp),
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Text(
                                text = item.title ?: item.url,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 3,
                                modifier =
                                    Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 8.dp, vertical = 20.dp),
                            )
                            IconButton(
                                onClick = { onRemove(item) },
                                modifier = Modifier.align(Alignment.TopEnd),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    }
                is ContentItem.Text -> { }
            }
        }
    }
}

@Composable
private fun editorImageThumbnail(
    image: ContentItem.Image,
    onImageClick: (ContentItem.Image) -> Unit,
    onRemove: (ContentItem) -> Unit,
) {
    val context = LocalContext.current
    val model =
        remember(image.id, image.source.localPath, image.source.remoteUrl) {
            imageDataForCoil(image.source)
        }
    Box {
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            modifier =
                Modifier
                    .size(88.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onImageClick(image) },
        ) {
            if (model != null) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(context)
                            .data(model)
                            .crossfade(true)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        IconButton(
            onClick = { onRemove(image) },
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun editorFullScreenImageViewer(
    image: ContentItem.Image,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val model =
        remember(image.id, image.source.localPath, image.source.remoteUrl) {
            imageDataForCoil(image.source)
        }
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onDismiss() },
        ) {
            if (model != null) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(context)
                            .data(model)
                            .crossfade(false)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 48.dp)
                            .align(Alignment.Center)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onDismiss() },
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    }
}

private fun imageDataForCoil(source: DataSource): Any? =
    when {
        !source.localPath.isNullOrBlank() -> File(source.localPath!!)
        !source.remoteUrl.isNullOrBlank() -> source.remoteUrl!!
        else -> null
    }

@Composable
private fun editorTitleField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Title") },
        singleLine = true,
        colors =
            TextFieldDefaults.colors(
                focusedTextColor = colors.onSurface,
                unfocusedTextColor = colors.onSurface,
                focusedPlaceholderColor = colors.onSurfaceVariant,
                unfocusedPlaceholderColor = colors.onSurfaceVariant,
                focusedContainerColor = colors.background,
                unfocusedContainerColor = colors.background,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
    )
}

@Composable
private fun editorContentField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text("Input") },
        minLines = 12,
        colors =
            TextFieldDefaults.colors(
                focusedTextColor = colors.onSurface,
                unfocusedTextColor = colors.onSurface,
                focusedPlaceholderColor = colors.onSurfaceVariant,
                unfocusedPlaceholderColor = colors.onSurfaceVariant,
                focusedContainerColor = colors.background,
                unfocusedContainerColor = colors.background,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
    )
}
