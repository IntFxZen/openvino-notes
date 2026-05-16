package com.itlab.notes.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.itlab.domain.model.ContentItem
import com.itlab.domain.usecase.noteusecase.ValidateDuplicateNoteTitleUseCase
import com.itlab.notes.media.ImageRegionLuminance
import com.itlab.notes.media.NoteMediaImport
import com.itlab.notes.media.imageAttachments
import com.itlab.notes.media.toCoilModel
import com.itlab.notes.ui.asDomainFolderId
import com.itlab.notes.ui.notes.NoteItemUi
import org.koin.compose.koinInject

private const val EDITOR_TOP_BAR_TITLE_MAX_LENGTH = 35

private val EditorHorizontalGutter = 15.dp
private val EditorHorizontalContentPadding = 15.dp

private data class EditorAttachmentsViewerState(
    val images: List<ContentItem.Image>,
    val initialIndex: Int,
)

private fun String.truncateForEditorTopBar(): String = take(EDITOR_TOP_BAR_TITLE_MAX_LENGTH)

private const val EDITOR_AI_UI_PREVIEW = true

private val editorAiPreviewSummary =
    "This note is about planning the product launch: goals for the week, " +
        "open questions for the team, and a short list of next steps."
private val editorAiPreviewTags =
    listOf("Work", "Planning", "Product", "Follow-up", "Study", "Study","Study",)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editorScreen(
    directoryName: String,
    directoryId: String,
    note: NoteItemUi,
    onBack: () -> Unit,
    onSave: (NoteItemUi) -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val validateDuplicateTitle: ValidateDuplicateNoteTitleUseCase = koinInject()
    val editorVm = remember(note.id) { EditorViewModel(initialNote = note) }
    var attachmentsViewer by remember { mutableStateOf<EditorAttachmentsViewerState?>(null) }
    val targetFolderId = note.folderId ?: directoryId.asDomainFolderId()
    var titleDuplicate by remember { mutableStateOf(false) }
    val trimmedTitle = editorVm.title.trim()
    val titleHasDuplicate = titleDuplicate && trimmedTitle.isNotEmpty()

    LaunchedEffect(editorVm.title, targetFolderId, note.id) {
        titleDuplicate =
            validateDuplicateTitle(
                title = editorVm.title,
                folderId = targetFolderId,
                excludeNoteId = note.id,
            )
    }

    LaunchedEffect(note.isFavorite) {
        editorVm.syncFavoriteFromNote(note.isFavorite)
    }

    val pickImages =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(),
        ) { uris: List<Uri> ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            val imported = NoteMediaImport.importImagesFromUris(context, uris)
            editorVm.addAttachments(imported)
        }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            editorTopBar(
                directoryName = directoryName,
                title = editorVm.title,
                isFavorite = note.isFavorite,
                onBack = onBack,
                onToggleFavorite = onToggleFavorite,
                onAddImage = {
                    pickImages.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
        },
        floatingActionButton = {
            editorFab(
                onClick = { onSave(editorVm.buildUpdatedNote()) },
                enabled = !titleHasDuplicate,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (EDITOR_AI_UI_PREVIEW && editorAiPreviewTags.isNotEmpty()) {
                editorAiTagsBar(
                    tags = editorAiPreviewTags,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EditorHorizontalGutter, vertical = 8.dp),
                )
            }
            editorContent(
                title = editorVm.title,
                titleHasDuplicate = titleHasDuplicate,
                content = editorVm.content,
                attachments = editorVm.attachments,
                aiSummary = if (EDITOR_AI_UI_PREVIEW) editorAiPreviewSummary else null,
                onTitleChange = editorVm::onTitleChange,
                onContentChange = editorVm::onContentChange,
                onAttachmentClick = { item ->
                    if (item !is ContentItem.Image) return@editorContent
                    val images = editorVm.attachments.imageAttachments()
                    val index = images.indexOfFirst { it.id == item.id }
                    if (index >= 0) {
                        attachmentsViewer =
                            EditorAttachmentsViewerState(
                                images = images,
                                initialIndex = index,
                            )
                    }
                },
                onRemoveAttachment = { item ->
                    if (item is ContentItem.Image) {
                        NoteMediaImport.deleteImportedFileIfOwned(context, item.source.localPath)
                    }
                    editorVm.removeAttachment(item.id)
                },
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            )
        }
    }

    attachmentsViewer?.let { viewer ->
        editorFullScreenAttachmentsViewer(
            images = viewer.images,
            initialIndex = viewer.initialIndex,
            onDismiss = { attachmentsViewer = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun editorTopBar(
    directoryName: String,
    title: String,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddImage: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val topBarTitle =
        (if (title.isBlank()) directoryName else title).truncateForEditorTopBar()
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = topBarTitle,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = colors.onSurface,
                )
            }
        },
        actions = {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription =
                        if (isFavorite) {
                            "Remove from favorites"
                        } else {
                            "Add to favorites"
                        },
                    tint = if (isFavorite) colors.primary else colors.onSurface,
                )
            }
            IconButton(onClick = onAddImage) {
                Icon(
                    Icons.Rounded.AddPhotoAlternate,
                    contentDescription = "Add images",
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
private fun editorFab(
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
        containerColor = colors.primary,
    ) {
        Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            tint = colors.onPrimary,
        )
    }
}

@Composable
private fun editorContent(
    title: String,
    titleHasDuplicate: Boolean,
    content: String,
    attachments: List<ContentItem>,
    aiSummary: String?,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onAttachmentClick: (ContentItem) -> Unit,
    onRemoveAttachment: (ContentItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = EditorHorizontalGutter, vertical = 12.dp),
    ) {
        if (!aiSummary.isNullOrBlank()) {
            editorCollapsibleSummaryCard(
                summary = aiSummary,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        editorTitleField(
            value = title,
            onValueChange = onTitleChange,
        )
        if (titleHasDuplicate) {
            Text(
                text = "A note with that name already exists.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        editorContentField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier.padding(top = 12.dp),
        )

        if (attachments.isNotEmpty()) {
            editorAttachmentsRow(
                attachments = attachments,
                onAttachmentClick = onAttachmentClick,
                onRemove = onRemoveAttachment,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(modifier = Modifier.height(88.dp))
    }
}

@Composable
private fun editorAttachmentsRow(
    attachments: List<ContentItem>,
    onAttachmentClick: (ContentItem) -> Unit,
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
                is ContentItem.Image -> editorImageThumbnail(item, onAttachmentClick, onRemove)
                is ContentItem.File ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier =
                            Modifier
                                .height(88.dp)
                                .width(120.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onAttachmentClick(item) },
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
                                Icon(Icons.Rounded.Close, contentDescription = null)
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
                                .width(120.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onAttachmentClick(item) },
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
                                Icon(Icons.Rounded.Close, contentDescription = null)
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
    onAttachmentClick: (ContentItem) -> Unit,
    onRemove: (ContentItem) -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val model =
        remember(image.id, image.source.localPath, image.source.remoteUrl) {
            image.source.toCoilModel()
        }
    var closeIconTint by remember(image.id) { mutableStateOf(Color.White) }
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
                    ) { onAttachmentClick(image) },
        ) {
            if (model != null) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(context)
                            .data(model)
                            .crossfade(true)
                            .allowHardware(false)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { state ->
                        val isLightRegion =
                            ImageRegionLuminance.isTopEndRegionLight(state.result.drawable)
                        closeIconTint = if (isLightRegion) Color.Black else Color.White
                    },
                    onError = { closeIconTint = colors.onSurfaceVariant },
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BrokenImage,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                    )
                }
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
                Icons.Rounded.Close,
                contentDescription = null,
                tint = closeIconTint,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun editorFullScreenAttachmentsViewer(
    images: List<ContentItem.Image>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (images.isEmpty()) return

    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val overlayBackground = colors.surface.copy(alpha = 0.88f)
    val safeInitialIndex = initialIndex.coerceIn(0, images.lastIndex)
    val pagerState =
        rememberPagerState(
            initialPage = safeInitialIndex,
            pageCount = { images.size },
        )

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        val dismissInteractionSource = remember { MutableInteractionSource() }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(overlayBackground)
                    .clickable(
                        interactionSource = dismissInteractionSource,
                        indication = null,
                    ) { onDismiss() },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 48.dp),
            ) { page ->
                val item = images[page]
                val model =
                    remember(item.id, item.source.localPath, item.source.remoteUrl) {
                        item.source.toCoilModel()
                    }
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember(page) { MutableInteractionSource() },
                                indication = null,
                            ) { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (model != null) {
                        val absorbImageTap = remember(item.id) { MutableInteractionSource() }
                        AsyncImage(
                            model =
                                ImageRequest.Builder(context)
                                    .data(model)
                                    .crossfade(false)
                                    .allowHardware(false)
                                    .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier =
                                Modifier
                                    .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
                                    .wrapContentSize()
                                    .clickable(
                                        interactionSource = absorbImageTap,
                                        indication = null,
                                        onClick = {},
                                    ),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.BrokenImage,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
            }
            if (images.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${images.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 20.dp, end = 8.dp),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = null,
                    tint = colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun editorAiTagsBar(
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        itemsIndexed(
            items = tags,
            key = { index, tag -> "$index-$tag" },
        ) { _, tag ->
            FilterChip(
                selected = true,
                onClick = { },
                label = {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors =
                    FilterChipDefaults.filterChipColors(
                        containerColor = colors.surfaceContainerHigh,
                        labelColor = colors.onSurface,
                        iconColor = colors.onSurface,
                        selectedContainerColor = colors.secondaryContainer,
                        selectedLabelColor = colors.onSecondaryContainer,
                        selectedLeadingIconColor = colors.onSecondaryContainer,
                    ),
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = true,
                        borderColor = colors.outline.copy(alpha = 0.35f),
                        selectedBorderColor = Color.Transparent,
                    ),
            )
        }
    }
}

@Composable
private fun editorCollapsibleSummaryCard(
    summary: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }
    val colors = MaterialTheme.colorScheme

    Surface(
        color = colors.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { expanded = !expanded }
                        .padding(horizontal = EditorHorizontalContentPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "AI Summary",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription =
                        if (expanded) {
                            "Collapse summary"
                        } else {
                            "Expand summary"
                        },
                    tint = colors.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    modifier =
                        Modifier.padding(
                            start = EditorHorizontalContentPadding,
                            end = EditorHorizontalContentPadding,
                            bottom = 14.dp,
                        ),
                )
            }
        }
    }
}

@Composable
private fun editorPlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    singleLine: Boolean = false,
    minLines: Int = 1,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = colors.onSurfaceVariant,
                    )
                },
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                    ),
                contentPadding =
                    PaddingValues(
                        horizontal = EditorHorizontalContentPadding,
                        vertical = 8.dp,
                    ),
            )
        },
    )
}

@Composable
private fun editorTitleField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    editorPlainTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = "Title",
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun editorContentField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    editorPlainTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = "Input",
        modifier = modifier,
        minLines = 12,
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}
