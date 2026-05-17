package com.itlab.notes

import android.app.Application
import com.itlab.appModule
import com.itlab.data.di.dataModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@NotesApplication)
            modules(listOf(appModule, dataModule))
        }
    }
}
