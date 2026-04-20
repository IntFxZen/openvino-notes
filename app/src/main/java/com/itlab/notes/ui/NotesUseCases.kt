package com.itlab.notes.ui

import com.itlab.domain.usecase.CreateFolderUseCase
import com.itlab.domain.usecase.CreateNoteUseCase
import com.itlab.domain.usecase.DeleteFolderUseCase
import com.itlab.domain.usecase.DeleteNoteUseCase
import com.itlab.domain.usecase.GetFolderUseCase
import com.itlab.domain.usecase.MoveNoteToFolderUseCase
import com.itlab.domain.usecase.ObserveFoldersUseCase
import com.itlab.domain.usecase.ObserveNotesByFolderUseCase
import com.itlab.domain.usecase.ObserveNotesUseCase
import com.itlab.domain.usecase.UpdateFolderUseCase
import com.itlab.domain.usecase.UpdateNoteUseCase

data class NotesUseCases(
    val createFolderUseCase: CreateFolderUseCase,
    val deleteFolderUseCase: DeleteFolderUseCase,
    val createNoteUseCase: CreateNoteUseCase,
    val deleteNoteUseCase: DeleteNoteUseCase,
    val updateNoteUseCase: UpdateNoteUseCase,
    val observeNotesByFolderUseCase: ObserveNotesByFolderUseCase,
    val observeFoldersUseCase: ObserveFoldersUseCase,
    val updateFolderUseCase: UpdateFolderUseCase,
    val getFolderUseCase: GetFolderUseCase,
    val moveNoteToFolderUseCase: MoveNoteToFolderUseCase,
    val observeNotesUseCase: ObserveNotesUseCase,
)
