package com.postaltools.tr25

import android.app.Application
import com.postaltools.tr25.data.AppDatabase
import com.postaltools.tr25.data.BillRepository
import com.postaltools.tr25.data.SettingsStore

class TrApp : Application() {
    lateinit var repo: BillRepository
        private set
    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        repo = BillRepository(AppDatabase.create(this).billDao())
        settings = SettingsStore(this)
    }
}
