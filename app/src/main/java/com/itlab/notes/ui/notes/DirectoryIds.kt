package com.itlab.notes.ui.notes

internal const val ALL_DIRECTORY_ID = "all"
internal const val RECENT_DIRECTORY_ID = "recent"
internal const val FAVORITES_DIRECTORY_ID = "favorites"

internal fun isVirtualDirectory(directoryId: String): Boolean =
    directoryId == ALL_DIRECTORY_ID ||
        directoryId == RECENT_DIRECTORY_ID ||
        directoryId == FAVORITES_DIRECTORY_ID
