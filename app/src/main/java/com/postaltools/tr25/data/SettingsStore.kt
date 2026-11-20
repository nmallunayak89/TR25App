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
    val mileageCapKm: String = "200",
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
        mileageCapKm = prefs.getString("mileageCapKm", "200") ?: "200",
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
            .putString("mileageCapKm", s.mileageCapKm.trim())
            .apply()
    }

    // ---------- PIN login (stored only as a hash) ----------
    private fun hash(x: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(("tr25::" + x).toByteArray()).joinToString("") { "%02x".format(it) }
    }
    fun hasPin() = prefs.getString("pinHash", null) != null
    fun checkPin(pin: String) = prefs.getString("pinHash", null) == hash(pin)
    fun checkRecovery(word: String) = prefs.getString("recHash", null) == hash(word.trim().lowercase())
    fun setPin(pin: String, recovery: String? = null) {
        val e = prefs.edit().putString("pinHash", hash(pin))
        if (recovery != null) e.putString("recHash", hash(recovery.trim().lowercase()))
        e.apply()
    }

    /** A blank bill with the saved defaults filled in. */
    fun newBill(today: String): TaBill {
        val s = load()
        return TaBill(
            officeName = s.officeName, name = s.name, designation = s.designation,
            pay = s.pay, headquarters = s.headquarters, debitableTo = s.debitableTo,
            mileageRate = s.mileageRate.ifBlank { "15" },
            daSlabs = listOf(DaSlab(rate = s.daRate), DaSlab(), DaSlab()),
            autoDa = true, daRate = s.daRate, mileageCapKm = s.mileageCapKm,
            claimDate = today,
        )
    }
}
