package com.postaltools.tr25.data

import com.postaltools.tr25.logic.Calculator
import com.postaltools.tr25.logic.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DuplicateBillNumberException(val subBillNo: String) :
    Exception("A final bill with Sub-Bill No. $subBillNo already exists")

class BillRepository(private val dao: BillDao) {

    fun observeAll(): Flow<List<BillEntity>> = dao.observeAll()

    suspend fun load(id: Long): TaBill? = withContext(Dispatchers.IO) {
        dao.getById(id)?.let { BillJson.fromJson(JSONObject(it.json), it.id) }
    }

    suspend fun entity(id: Long): BillEntity? = withContext(Dispatchers.IO) { dao.getById(id) }

    /** True if another FINAL bill already uses this Sub-Bill No. (blank numbers are never duplicates). */
    suspend fun isDuplicateSubBillNo(no: String, excludeId: Long): Boolean = withContext(Dispatchers.IO) {
        no.isNotBlank() && dao.countFinalWithSubBillNo(no.trim(), excludeId) > 0
    }

    /**
     * Saves the bill and returns its id. Final bills must have a unique Sub-Bill No.
     * Saving keeps any earlier PDF path, but marks it stale by clearing it so a fresh PDF is generated.
     */
    suspend fun save(bill: TaBill, status: BillStatus): Long = withContext(Dispatchers.IO) {
        if (status == BillStatus.FINAL && isDuplicateSubBillNo(bill.subBillNo, bill.id)) {
            throw DuplicateBillNumberException(bill.subBillNo.trim())
        }
        val now = System.currentTimeMillis()
        val existing = if (bill.id != 0L) dao.getById(bill.id) else null
        val toSave = bill.copy(
            status = status,
            subBillNo = bill.subBillNo.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        val net = Calculator.compute(toSave).net
        val entity = BillEntity(
            id = existing?.id ?: 0,
            subBillNo = toSave.subBillNo,
            claimDate = toSave.claimDate,
            officeName = toSave.officeName,
            officialName = toSave.name,
            designation = toSave.designation,
            status = status.name,
            netAmount = Money.format(net),
            pdfPath = null,
            json = BillJson.toJson(toSave).toString(),
            createdAt = toSave.createdAt,
            updatedAt = now,
        )
        if (existing == null) {
            dao.insert(entity)
        } else {
            existing.pdfPath?.let { File(it).delete() }
            dao.update(entity)
            existing.id
        }
    }

    suspend fun setPdfPath(id: Long, path: String) = withContext(Dispatchers.IO) { dao.setPdfPath(id, path) }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dao.getById(id)?.pdfPath?.let { File(it).delete() }
        dao.delete(id)
    }

    /** Copies a bill as a new draft with a blank Sub-Bill No. and dates, ready for the next claim. */
    suspend fun duplicate(id: Long): Long = withContext(Dispatchers.IO) {
        val src = load(id) ?: error("Bill not found")
        val copy = src.copy(id = 0, subBillNo = "", claimDate = "", advanceVoucherNo = "", advanceVoucherDate = "")
        save(copy, BillStatus.DRAFT)
    }

    // ---------- Backup / Restore ----------

    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        dao.getAll().forEach { arr.put(JSONObject(it.json)) }
        JSONObject().apply {
            put("app", "TR25BillGenerator")
            put("backupVersion", 1)
            put("exportedAt", System.currentTimeMillis())
            put("bills", arr)
        }.toString(2)
    }

    /** Imports every bill in the backup as a new record. Returns (imported, skipped). */
    suspend fun importBackup(text: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val root = JSONObject(text)
        require(root.optString("app") == "TR25BillGenerator") { "This file is not a TR-25 Bill Generator backup" }
        val arr = root.getJSONArray("bills")
        var ok = 0; var skipped = 0
        for (i in 0 until arr.length()) {
            val bill = BillJson.fromJson(arr.getJSONObject(i), id = 0)
            val status = if (bill.status == BillStatus.FINAL &&
                isDuplicateSubBillNo(bill.subBillNo, 0)) BillStatus.DRAFT else bill.status
            runCatching { save(bill, status) }.onSuccess { ok++ }.onFailure { skipped++ }
        }
        ok to skipped
    }
}
