package com.postaltools.tr25

import android.app.Application
import com.postaltools.tr25.data.AppDatabase
import com.postaltools.tr25.data.BillRepository
import com.postaltools.tr25.data.DiaryStore
import com.postaltools.tr25.data.ReportStore
import com.postaltools.tr25.data.SettingsStore

class TrApp : Application() {
    lateinit var repo: BillRepository
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var diary: DiaryStore
        private set
    lateinit var reports: ReportStore
        private set

    override fun onCreate() {
        super.onCreate()
        repo = BillRepository(AppDatabase.create(this).billDao())
        settings = SettingsStore(this)
        diary = DiaryStore(this)
        reports = ReportStore(this)
        com.postaltools.tr25.pdf.Fonts.load(this)
    }
}
