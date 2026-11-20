package com.postaltools.tr25.data

import android.content.Context

/** Defaults that pre-fill every new bill. Stored only on this device. */
data class AppSettings(
    val officeName: String = "",
    val name: String = "",
    val designation: String = "",
    val pay: String = "",
    val headquarters: String = "",
    val debitableTo: String = "",
    val mileageRate: String = "",
    val daRate: String = "",
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("tr25_settings", Context.MODE_PRIVATE)

    fun load() = AppSettings(
        officeName = prefs.getString("officeName", "") ?: "",
        name = prefs.getString("name", "") ?: "",
        designation = prefs.getString("designation", "") ?: "",
        pay = prefs.getString("pay", "") ?: "",
        headquarters = prefs.getString("headquarters", "") ?: "",
        debitableTo = prefs.getString("debitableTo", "") ?: "",
        mileageRate = prefs.getString("mileageRate", "") ?: "",
        daRate = prefs.getString("daRate", "") ?: "",
    )

    fun save(s: AppSettings) {
        prefs.edit()
            .putString("officeName", s.officeName.trim())
            .putString("name", s.name.trim())
            .putString("designation", s.designation.trim())
            .putString("pay", s.pay.trim())
            .putString("headquarters", s.headquarters.trim())
            .putString("debitableTo", s.debitableTo.trim())
            .putString("mileageRate", s.mileageRate.trim())
            .putString("daRate", s.daRate.trim())
            .apply()
    }

    /** A blank bill with the saved defaults filled in. */
    fun newBill(today: String): TaBill {
        val s = load()
        return TaBill(
            officeName = s.officeName, name = s.name, designation = s.designation,
            pay = s.pay, headquarters = s.headquarters, debitableTo = s.debitableTo,
            mileageRate = s.mileageRate,
            daSlabs = listOf(DaSlab(rate = s.daRate), DaSlab(), DaSlab()),
            claimDate = today,
        )
    }
}
