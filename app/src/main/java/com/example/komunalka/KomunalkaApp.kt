package com.example.komunalka

import android.app.Application
import com.example.komunalka.data.AppDatabase
import com.example.komunalka.repository.UtilityRepository
import com.example.komunalka.utils.Prefs

class KomunalkaApp : Application() {

    lateinit var repository: UtilityRepository
        private set
    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        repository = UtilityRepository(database)
        prefs = Prefs(this)
        prefs.applyTheme()
    }
}
