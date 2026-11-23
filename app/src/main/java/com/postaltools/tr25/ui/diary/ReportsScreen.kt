package com.postaltools.tr25.ui.diary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.postaltools.tr25.TrApp
import com.postaltools.tr25.data.Annex
import com.postaltools.tr25.ui.components.Field
import com.postaltools.tr25.util.HtmlPrinter
import com.postaltools.tr25.util.Period
import com.postaltools.tr25.util.ReportHeader
import com.postaltools.tr25.util.ReportHtml
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(month: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as TrApp
    val st = app.settings
    val s0 = remember { st.load() }
    var ym by remember { mutableStateOf(runCatching { YearMonth.parse(month) }.getOrDefault(YearMonth.now())) }
    var part by remember { mutableStateOf(1) }
    val period = Period(ym, part)
    var annex by remember(period.key) { mutableStateOf(app.reports.loadAnnex(period.key)) }
    fun setAnnex(a: Annex) { annex = a; app.reports.saveAnnex(period.key, a) }

    // Header lines (kept for every report)
    var officeTitle by remember { mutableStateOf(st.get("officeTitle", s0.officeName.uppercase())) }
    var place by remember { mutableStateOf(st.get("place", s0.headquarters)) }
    var submittedTo by remember { mutableStateOf(st.get("submittedTo", "The SSPOs\n")) }
    var sigOffice by remember { mutableStateOf(st.get("sigOffice", s0.officeName)) }
    var sigName by remember { mutableStateOf(st.get("sigName", s0.name)) }
    var sigDesig by remember { mutableStateOf(st.get("sigDesig", s0.designation.ifBlank { "Inspector Posts" })) }
    fun header() = ReportHeader(officeTitle.trim(), place.trim(), submittedTo.trim(), sigName.trim(), sigDesig.trim(), sigOffice.trim())

    val journeys = remember { app.diary.load() }
    val notes = remember { app.reports.loadDays() }
    val js = ReportHtml.journeysIn(journeys, period)

    var pendingWord by remember { mutableStateOf("") }
    val saveWord = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/msword")) { uri ->
        if (uri != null && pendingWord.isNotEmpty()) {
            val ok = runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(pendingWord.toByteArray()) } }.isSuccess
            android.widget.Toast.makeText(ctx, if (ok) "Word file saved" else "Could not save the file", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val tag = (if (part == 0) "" else "_${part}FN") + "_" + period.monthText
    fun diaryHtml(word: Boolean) = ReportHtml.diary(period, header(), journeys, notes, annex, word)
    fun tourHtml(word: Boolean) = ReportHtml.tourMoment(period, header(), journeys, annex, word)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diary & Tour Moment reports") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { ym = ym.minusMonths(1) }) { Text("‹") }
                Text(period.monthText, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                OutlinedButton(onClick = { ym = ym.plusMonths(1) }) { Text("›") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1 to "1st FN (1–15)", 2 to "2nd FN (16–end)", 0 to "Monthly").forEach { (p, l) ->
                    FilterChip(selected = part == p, onClick = { part = p }, label = { Text(l) })
                }
            }
            Text("${js.size} journeys · ${ReportHtml.countInspections(js)} inspections · ${ReportHtml.countVisits(js)} visits found in the diary",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // ------- Print / Word
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Diary – ${period.title.lowercase()}", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button({ HtmlPrinter.print(ctx, diaryHtml(false), "Diary$tag", landscape = false) }, Modifier.weight(1f)) { Text("PDF / PRINT") }
                        OutlinedButton({ pendingWord = diaryHtml(true); saveWord.launch("Diary$tag.doc") }, Modifier.weight(1f)) { Text("WORD") }
                    }
                    Text("Tour Moment (journey particulars)", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button({ HtmlPrinter.print(ctx, tourHtml(false), "Tour_Moment$tag", landscape = true) }, Modifier.weight(1f)) { Text("PDF / PRINT") }
                        OutlinedButton({ pendingWord = tourHtml(true); saveWord.launch("Tour_Moment$tag.doc") }, Modifier.weight(1f)) { Text("WORD") }
                    }
                    Text("PDF / PRINT opens the print screen – choose \"Save as PDF\".", style = MaterialTheme.typography.bodySmall)
                }
            }

            Section("Reference") {
                Field("No. (e.g. IP GBD/Diary/FNR/JULY/2026)", annex.refNo, { setAnnex(annex.copy(refNo = it)) })
                Field("Dated", annex.refDate, { setAnnex(annex.copy(refDate = it)) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("No. of inspections (blank = auto)", annex.inspCount, { setAnnex(annex.copy(inspCount = it)) }, modifier = Modifier.weight(1f))
                    Field("No. of visits (blank = auto)", annex.visitCount, { setAnnex(annex.copy(visitCount = it)) }, modifier = Modifier.weight(1f))
                }
            }

            Section("Header & signature (kept for all reports)") {
                Field("Office line, e.g. GAURIBIDANUR SUB DIVISION GAURIBIDANUR-561 208", officeTitle, { officeTitle = it; st.put("officeTitle", it) })
                Field("Place for 'dated at', e.g. Gauribidanur-561 208", place, { place = it; st.put("place", it) })
                Field("Submitted to (one line each)", submittedTo, { submittedTo = it; st.put("submittedTo", it) }, singleLine = false)
                Field("Name for signature", sigName, { sigName = it; st.put("sigName", it) })
                Field("Designation", sigDesig, { sigDesig = it; st.put("sigDesig", it) })
                Field("Office under signature", sigOffice, { sigOffice = it; st.put("sigOffice", it) })
            }

            if (part != 0) {
                Text("Annexure to the fortnightly diary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Standing items (B, C, E, F, reasons, copy to) are carried forward from the last fortnight.",
                    style = MaterialTheme.typography.bodySmall)
                RowsEditor("A. Surprise visits (blank = taken from diary)", listOf("Office visited", "Type (SO/BO)", "Date", "Purpose", "Remarks"),
                    annex.visits, { setAnnex(annex.copy(visits = it)) })
                Section("Reasons for not carrying out adequate monthly visits") {
                    Field("Reasons", annex.reasons, { setAnnex(annex.copy(reasons = it)) }, singleLine = false)
                }
                RowsEditor("B. Inspections", INSP_COLS, annex.insp, { setAnnex(annex.copy(insp = it)) }, fixed = true)
                RowsEditor("C. Establishment work", listOf("Item of work", "Pending at beginning / as per yearly programme",
                    "Received in F/N", "Carried out in F/N", "Pending at end of F/N"), annex.est, { setAnnex(annex.copy(est = it)) }, fixed = true)
                RowsEditor("D. Fraud cases (blank = \"No fraud cases detected\")", listOf("Brief of the case", "Date",
                    "Amount of fraud reported", "Date of commencement of PWV"), annex.fraud, { setAnnex(annex.copy(fraud = it)) })
                RowsEditor("E. Rule 10/14 cases (IO / PO)", listOf("Brief particulars of the case", "Date of appointment as IO/PO",
                    "No. of hearings held", "Witnesses examined", "Yet to be examined", "Date by which enquiry would be completed",
                    "IO/PO report would be submitted", "Reasons for delay"), annex.rule, { setAnnex(annex.copy(rule = it)) })
                RowsEditor("F. Provisional / regular engagements", listOf("Particulars of post with name of office", "Date of vacancy",
                    "Date of finalisation of engagement", "Reasons for delay"), annex.engage, { setAnnex(annex.copy(engage = it)) })
                Section("Endorsement") {
                    Field("Endt. No.", annex.endtNo, { setAnnex(annex.copy(endtNo = it)) })
                    Field("Dated", annex.endtDate, { setAnnex(annex.copy(endtDate = it)) })
                    Field("1. Copy submitted to", annex.copyTo, { setAnnex(annex.copy(copyTo = it)) }, singleLine = false)
                }
            }
        }
    }
}

private val INSP_COLS = listOf("Type of inspection") + listOf(
    "Approved for the year", "Done in the FN", "Previous arrears", "Arrears of current month", "Total pending",
    "Total to be done till current month", "Done (progressive)", "IRs pending to be released",
).flatMap { listOf("$it – SO", "$it – BO") }

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

/** Simple table editor: one card per row, one box per column. */
@Composable
private fun RowsEditor(
    title: String, cols: List<String>, rows: List<List<String>>, onChange: (List<List<String>>) -> Unit, fixed: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                TextButton(onClick = { open = !open }) { Text(if (open) "Hide" else "Edit (${rows.size})") }
            }
            if (open) {
                rows.forEachIndexed { i, r ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Row ${i + 1}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                if (!fixed) TextButton(onClick = { onChange(rows.filterIndexed { k, _ -> k != i }) }) { Text("Remove") }
                            }
                            cols.forEachIndexed { c, label ->
                                Field(label, r.getOrElse(c) { "" }, { v ->
                                    val nr = List(maxOf(cols.size, r.size)) { k -> if (k == c) v else r.getOrElse(k) { "" } }
                                    onChange(rows.mapIndexed { k, old -> if (k == i) nr else old })
                                }, singleLine = c != 0 || !fixed)
                            }
                        }
                    }
                }
                if (!fixed) OutlinedButton(onClick = { onChange(rows + listOf(List(cols.size) { "" })) }, Modifier.fillMaxWidth()) { Text("+ Add row") }
            }
        }
    }
}
