package com.dhan.app

import android.app.Application
import com.dhan.app.data.db.AppDatabase
import com.dhan.app.data.prefs.UserPrefs
import com.dhan.app.data.repo.DhanRepository

class DhanApplication : Application() {
    lateinit var repository: DhanRepository
        private set
    lateinit var userPrefs: UserPrefs
        private set

    override fun onCreate() {
        super.onCreate()
        repository = DhanRepository(AppDatabase.get(this))
        userPrefs = UserPrefs(this)
    }
}
