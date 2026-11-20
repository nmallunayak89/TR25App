package com.postaltools.tr25.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Day-to-day tour diary: every journey entered, kept on the phone in one JSON file. */
class DiaryStore(context: Context) {
    private val file = File(context.filesDir, "diary.json")

    fun load(): List<JourneyRow> = runCatching {
        val a = JSONArray(file.readText())
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            fun s(k: String) = o.optString(k, "")
            JourneyRow(s("depDateTime"), s("fromPlace"), s("arrDateTime"), s("toPlace"), s("modeClass"), s("fare"),
                s("distanceKm"), s("haltDays"), s("haltHours"), s("purpose"), s("remarks"),
                s("workType"), s("office"), s("caseRef"))
        }
    }.getOrDefault(emptyList())

    fun save(list: List<JourneyRow>) {
        val a = JSONArray()
        list.forEach { j ->
            a.put(JSONObject().apply {
                put("depDateTime", j.depDateTime); put("fromPlace", j.fromPlace)
                put("arrDateTime", j.arrDateTime); put("toPlace", j.toPlace)
                put("modeClass", j.modeClass); put("fare", j.fare); put("distanceKm", j.distanceKm)
                put("haltDays", j.haltDays); put("haltHours", j.haltHours)
                put("purpose", j.purpose); put("remarks", j.remarks)
                put("workType", j.workType); put("office", j.office); put("caseRef", j.caseRef)
            })
        }
        file.writeText(a.toString())
    }
}
