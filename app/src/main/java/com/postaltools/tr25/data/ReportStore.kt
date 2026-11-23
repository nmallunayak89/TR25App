package com.postaltools.tr25.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Annexure to the fortnightly diary (items A–F and the endorsement). */
data class Annex(
    val refNo: String = "",
    val refDate: String = "",
    val inspCount: String = "",     // blank = counted from the diary
    val visitCount: String = "",    // blank = counted from the diary
    val visits: List<List<String>> = emptyList(),   // blank = taken from the diary (surprise visits)
    val reasons: String = "",
    val insp: List<List<String>> = listOf("Annual", "Re-Allotted", "2nd Inspection").map { listOf(it) + List(16) { "" } },
    val est: List<List<String>> = EST_ITEMS.map { listOf(it, "", "", "", "") },
    val fraud: List<List<String>> = emptyList(),
    val rule: List<List<String>> = emptyList(),
    val engage: List<List<String>> = listOf(listOf(
        "Presently all the engagement process is decentralised and Regular engagements are issued at DO Level", "", "", "")),
    val endtNo: String = "",
    val endtDate: String = "",
    val copyTo: String = "",
) {
    companion object {
        val EST_ITEMS = listOf(
            "Est-2 Verifications", "Revision of allowances of BPMs & other GDS", "Triennial Review of BOs",
            "Complaints received for enquiry", "SB/NSC/PLI/RPLI claims received for verification",
            "SB withdrawals for verification", "PWV in fraud & loss cases", "PWV in respect of resignation cases",
            "Cent percent verification cases",
        )
    }
}

/** Daily diary notes (date → details) and the annexure of each fortnight, kept on the phone. */
class ReportStore(context: Context) {
    private val daysFile = File(context.filesDir, "diary_days.json")
    private val annexFile = File(context.filesDir, "diary_annex.json")

    fun loadDays(): Map<String, String> = runCatching {
        val o = JSONObject(daysFile.readText())
        o.keys().asSequence().associateWith { o.optString(it, "") }
    }.getOrDefault(emptyMap())

    fun saveDays(m: Map<String, String>) {
        val o = JSONObject(); m.filterValues { it.isNotBlank() }.forEach { (k, v) -> o.put(k, v) }
        daysFile.writeText(o.toString())
    }

    private fun all(): JSONObject = runCatching { JSONObject(annexFile.readText()) }.getOrDefault(JSONObject())

    /** Annexure for [key] (e.g. "2026-07-F1"). A new one carries forward the standing items of the latest earlier one. */
    fun loadAnnex(key: String): Annex {
        val a = all()
        if (a.has(key)) return fromJson(a.getJSONObject(key))
        val prev = a.keys().asSequence().filter { it < key }.sorted().lastOrNull() ?: return Annex()
        val p = fromJson(a.getJSONObject(prev))
        return Annex(reasons = p.reasons, insp = p.insp, est = p.est.map { listOf(it[0], it.getOrElse(4) { "" }, "", "", "") },
            rule = p.rule, engage = p.engage, copyTo = p.copyTo)
    }

    fun saveAnnex(key: String, x: Annex) {
        val a = all(); a.put(key, toJson(x)); annexFile.writeText(a.toString())
    }

    private fun rows(r: List<List<String>>) = JSONArray().apply { r.forEach { put(JSONArray(it)) } }
    private fun rows(a: JSONArray?): List<List<String>> = if (a == null) emptyList() else
        (0 until a.length()).map { i -> val x = a.getJSONArray(i); (0 until x.length()).map { x.optString(it, "") } }

    private fun toJson(x: Annex) = JSONObject().apply {
        put("refNo", x.refNo); put("refDate", x.refDate); put("inspCount", x.inspCount); put("visitCount", x.visitCount)
        put("visits", rows(x.visits)); put("reasons", x.reasons); put("insp", rows(x.insp)); put("est", rows(x.est))
        put("fraud", rows(x.fraud)); put("rule", rows(x.rule)); put("engage", rows(x.engage))
        put("endtNo", x.endtNo); put("endtDate", x.endtDate); put("copyTo", x.copyTo)
    }

    private fun fromJson(o: JSONObject): Annex {
        val d = Annex()
        fun s(k: String) = o.optString(k, "")
        return Annex(
            s("refNo"), s("refDate"), s("inspCount"), s("visitCount"), rows(o.optJSONArray("visits")), s("reasons"),
            rows(o.optJSONArray("insp")).ifEmpty { d.insp }, rows(o.optJSONArray("est")).ifEmpty { d.est },
            rows(o.optJSONArray("fraud")), rows(o.optJSONArray("rule")), rows(o.optJSONArray("engage")).ifEmpty { d.engage },
            s("endtNo"), s("endtDate"), s("copyTo"),
        )
    }
}
