package com.itlab.notes.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.itlab.domain.model.ContentItem
import com.itlab.notes.media.withoutTextItems
import com.itlab.notes.ui.notes.NoteItemUi
import com.itlab.notes.ui.toSingleLineText

class EditorViewModel(
    initialNote: NoteItemUi,
) {
    private val noteId: String = initialNote.id
    private val userId: String = initialNote.userId
    private val folderId: String? = initialNote.folderId

    var title: String by mutableStateOf(initialNote.title.toSingleLineText())
        private set

    var content: String by mutableStateOf(initialNote.content)
        private set

    var attachments: List<ContentItem> by mutableStateOf(initialNote.attachments.withoutTextItems())
        private set

    var isFavorite: Boolean by mutableStateOf(initialNote.isFavorite)
        private set

    fun syncFavoriteFromNote(value: Boolean) {
        isFavorite = value
    }

    fun onTitleChange(newTitle: String) {
        title = newTitle.toSingleLineText()
    }

    fun onContentChange(newContent: String) {
        content = newContent
    }

    fun addAttachment(item: ContentItem) {
        attachments = attachments + item
    }

    fun addAttachments(items: List<ContentItem>) {
        if (items.isEmpty()) return
        attachments = attachments + items
    }

    fun removeAttachment(id: String) {
        attachments = attachments.filterNot { it.id == id }
    }

    /** Fills local/remote paths from DB after save or sync without replacing user edits. */
    fun mergeAttachmentSources(fromNote: List<ContentItem>) {
        val incomingById = fromNote.withoutTextItems().associateBy { it.id }
        if (incomingById.isEmpty()) return
        attachments =
            attachments.map { item ->
                val incoming = incomingById[item.id] ?: return@map item
                when (item) {
                    is ContentItem.Image ->
                        (incoming as? ContentItem.Image)?.let { remote ->
                            item.copy(
                                source =
                                    item.source.copy(
                                        localPath = item.source.localPath ?: remote.source.localPath,
                                        remoteUrl = item.source.remoteUrl ?: remote.source.remoteUrl,
                                    ),
                            )
                        } ?: item
                    is ContentItem.File ->
                        (incoming as? ContentItem.File)?.let { remote ->
                            item.copy(
                                source =
                                    item.source.copy(
                                        localPath = item.source.localPath ?: remote.source.localPath,
                                        remoteUrl = item.source.remoteUrl ?: remote.source.remoteUrl,
                                    ),
                            )
                        } ?: item
                    else -> item
                }
            }
    }

    fun buildUpdatedNote(): NoteItemUi =
        NoteItemUi(
            id = noteId,
            userId = userId,
            title = title,
            content = content,
            folderId = folderId,
            attachments = attachments,
            isFavorite = isFavorite,
        )
}
