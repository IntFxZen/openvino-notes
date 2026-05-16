package com.itlab.notes.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.itlab.domain.model.ContentItem
import com.itlab.notes.media.withoutTextItems
import com.itlab.notes.ui.notes.NoteItemUi

class EditorViewModel(
    initialNote: NoteItemUi,
) {
    private val noteId: String = initialNote.id
    private val folderId: String? = initialNote.folderId

    var title: String by mutableStateOf(initialNote.title)
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
        title = newTitle
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

    fun buildUpdatedNote(): NoteItemUi =
        NoteItemUi(
            id = noteId,
            title = title,
            content = content,
            folderId = folderId,
            attachments = attachments,
            isFavorite = isFavorite,
        )
}
