package com.postaltools.tr25.ui.edit

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.postaltools.tr25.TrApp
import com.postaltools.tr25.data.BillStatus
import com.postaltools.tr25.data.DuplicateBillNumberException
import com.postaltools.tr25.data.TaBill
import com.postaltools.tr25.logic.Calculator
import com.postaltools.tr25.logic.Dates
import com.postaltools.tr25.logic.Issue
import com.postaltools.tr25.logic.Totals
import com.postaltools.tr25.logic.Validator
import kotlinx.coroutines.launch

sealed interface GenerateResult {
    data class Ok(val id: Long) : GenerateResult
    data class Problems(val issues: List<Issue>) : GenerateResult
}

class EditorViewModel(private val app: TrApp, private val billId: Long, private val month: String = "") : ViewModel() {

    var bill by mutableStateOf(TaBill())
        private set
    var loaded by mutableStateOf(false)
        private set
    var dirty by mutableStateOf(false)
        private set
    /** Problems found by the last "Generate" attempt, shown next to the fields. */
    var issues by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    val totals: Totals by derivedStateOf { Calculator.compute(bill) }

    init {
        viewModelScope.launch {
            bill = if (billId == 0L) {
                val fresh = app.settings.newBill(Dates.today())
                val ym = runCatching { java.time.YearMonth.parse(month) }.getOrNull()
                if (ym == null) fresh else {
                    // Bill for one month, filled from the Tour Diary.
                    val rows = com.postaltools.tr25.ui.diary.journeysOf(app.diary.load(), ym)
                    if (rows.isEmpty()) fresh else fresh.copy(journeys = rows, autoDa = true)
                }
            } else app.repo.load(billId)?.let { old ->
                // Bills made before auto DA: switch it on if no DA lines were typed, and fill blank rates from Settings.
                val s = app.settings.load()
                val noSlabs = old.daSlabs.all { it.days.isBlank() }
                old.copy(
                    autoDa = old.autoDa || noSlabs,
                    daRate = old.daRate.ifBlank { s.daRate },
                    mileageRate = old.mileageRate.ifBlank { s.mileageRate.ifBlank { "15" } },
                    mileageCapKm = old.mileageCapKm.ifBlank { s.mileageCapKm },
                )
            } ?: app.settings.newBill(Dates.today())
            loaded = true
        }
    }

    /** Replaces item 5 with the Tour Diary journeys of [ym]. Returns how many were loaded. */
    fun loadFromDiary(ym: java.time.YearMonth): Int {
        val rows = com.postaltools.tr25.ui.diary.journeysOf(app.diary.load(), ym)
        if (rows.isNotEmpty()) update { it.copy(journeys = rows) }
        return rows.size
    }

    fun update(change: (TaBill) -> TaBill) {
        bill = change(bill)
        dirty = true
        // After a failed Generate, keep the error list live so messages disappear as fields are fixed.
        if (issues.isNotEmpty()) {
            issues = Validator.validateForFinal(bill).associate { it.key to it.message }
        }
    }

    /** Error to show for a field: a live number error, or one from the last Generate attempt. */
    fun errorFor(key: String): String? = totals.errors[key] ?: issues[key]

    suspend fun saveDraft(): Long {
        val id = app.repo.save(bill, BillStatus.DRAFT)
        bill = bill.copy(id = id, status = BillStatus.DRAFT)
        dirty = false
        return id
    }

    suspend fun generate(): GenerateResult {
        val found = Validator.validateForFinal(bill, totals).toMutableList()
        if (app.repo.isDuplicateSubBillNo(bill.subBillNo, bill.id)) {
            found += Issue("subBillNo", "Sub-Bill No. ${bill.subBillNo.trim()} is already used by another final bill")
        }
        issues = found.associate { it.key to it.message }
        if (found.isNotEmpty()) return GenerateResult.Problems(found)
        return try {
            val id = app.repo.save(bill, BillStatus.FINAL)
            bill = bill.copy(id = id, status = BillStatus.FINAL)
            dirty = false
            GenerateResult.Ok(id)
        } catch (e: DuplicateBillNumberException) {
            val i = Issue("subBillNo", e.message ?: "Duplicate Sub-Bill No.")
            issues = mapOf(i.key to i.message)
            GenerateResult.Problems(listOf(i))
        }
    }

    companion object {
        fun factory(app: TrApp, id: Long, month: String = ""): ViewModelProvider.Factory = viewModelFactory {
            initializer { EditorViewModel(app, id, month) }
        }
    }
}
