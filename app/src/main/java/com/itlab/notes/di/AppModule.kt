package com.itlab.notes.di

import com.itlab.domain.usecase.folderusecase.CreateFolderUseCase
import com.itlab.domain.usecase.folderusecase.DeleteFolderUseCase
import com.itlab.domain.usecase.folderusecase.GetFolderUseCase
import com.itlab.domain.usecase.folderusecase.ObserveFoldersUseCase
import com.itlab.domain.usecase.folderusecase.UpdateFolderUseCase
import com.itlab.domain.usecase.noteusecase.CreateNoteUseCase
import com.itlab.domain.usecase.noteusecase.DeleteNoteUseCase
import com.itlab.domain.usecase.noteusecase.GetAllFavoritesUseCase
import com.itlab.domain.usecase.noteusecase.MoveNoteToFolderUseCase
import com.itlab.domain.usecase.noteusecase.SwitchFavoriteUseCase
import com.itlab.domain.usecase.noteusecase.ObserveNotesByFolderUseCase
import com.itlab.domain.usecase.noteusecase.ObserveNotesUseCase
import com.itlab.domain.usecase.noteusecase.SearchNotesUseCase
import com.itlab.domain.usecase.noteusecase.UpdateNoteUseCase
import com.itlab.domain.usecase.noteusecase.ValidateDuplicateNoteTitleUseCase
import com.itlab.notes.ui.NotesUseCases
import com.itlab.notes.ui.NotesViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {
        factory { ValidateDuplicateNoteTitleUseCase(get()) }
        factory { CreateNoteUseCase(get(), get()) }
        factory { CreateFolderUseCase(get()) }
        factory { DeleteFolderUseCase(get(), get()) }
        factory { DeleteNoteUseCase(get()) }
        factory { UpdateNoteUseCase(get(), get()) }
        factory { UpdateFolderUseCase(get()) }
        factory { GetFolderUseCase(get()) }
        factory { ObserveNotesByFolderUseCase(get()) }
        factory { ObserveFoldersUseCase(get()) }
        factory { MoveNoteToFolderUseCase(get(), get()) }
        factory { ObserveNotesUseCase(get()) }
        factory { SearchNotesUseCase(get()) }
        factory { SwitchFavoriteUseCase(get()) }
        factory { GetAllFavoritesUseCase(get()) }
        factory { UpdateFolderUseCase(get()) }
        factory { GetFolderUseCase(get()) }
        factory {
            NotesUseCases(
                createFolderUseCase = get(),
                deleteFolderUseCase = get(),
                createNoteUseCase = get(),
                deleteNoteUseCase = get(),
                updateNoteUseCase = get(),
                observeNotesByFolderUseCase = get(),
                observeFoldersUseCase = get(),
                updateFolderUseCase = get(),
                getFolderUseCase = get(),
                moveNoteToFolderUseCase = get(),
                observeNotesUseCase = get(),
                searchNotesUseCase = get(),
                switchFavoriteUseCase = get(),
                getAllFavoritesUseCase = get(),
            )
        }

        viewModel {
            NotesViewModel(
                useCases = get(),
            )
        }
    }
