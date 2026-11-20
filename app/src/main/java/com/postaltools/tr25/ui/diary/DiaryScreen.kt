package com.postaltools.tr25.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.postaltools.tr25.data.JourneyRow
import com.postaltools.tr25.logic.Dates
import com.postaltools.tr25.logic.Money
import com.postaltools.tr25.logic.Tours
import com.postaltools.tr25.ui.components.DateField
import com.postaltools.tr25.ui.components.Field
import com.postaltools.tr25.ui.components.FieldKind
import com.postaltools.tr25.ui.components.MoneyField
import com.postaltools.tr25.ui.components.SuggestField
import com.postaltools.tr25.ui.edit.MODES
import com.postaltools.tr25.ui.edit.PURPOSES
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MONTH = DateTimeFormatter.ofPattern("MMMM yyyy")

/** Journeys of [ym], plus new rows that have no date yet. */
fun journeysOf(all: List<JourneyRow>, ym: YearMonth): List<JourneyRow> =
    all.filter { j -> Dates.parseDateTime(j.depDateTime)?.let { YearMonth.from(it) == ym } ?: false }
        .sortedBy { Dates.parseDateTime(it.depDateTime) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(onBack: () -> Unit, onCreateBill: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as TrApp
    val hq = remember { app.settings.load().headquarters }
    var all by remember { mutableStateOf(app.diary.load()) }
    var ym by remember { mutableStateOf(YearMonth.now()) }
    var editing by remember { mutableIntStateOf(-1) }      // index in [all]
    var draft by remember { mutableStateOf<JourneyRow?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun persist(list: List<JourneyRow>) { all = list; app.diary.save(list) }

    val month = journeysOf(all, ym)
    val tours = Tours.tours(month, hq)
    val daDays = Tours.daDays(tours)

    fun startNew(base: JourneyRow) { editing = -1; draft = base }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tour Diary")
                        Text("Day-to-day tour movement · HQ: ${hq.ifBlank { "set in Settings" }}",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { onCreateBill(ym.toString()) },
                    enabled = month.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                ) { Text("CREATE TR-25 BILL FOR ${ym.format(MONTH).uppercase()}") }
            }
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Month bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { ym = ym.minusMonths(1); draft = null }) { Text("‹") }
                Text(ym.format(MONTH), Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                OutlinedButton(onClick = { ym = ym.plusMonths(1); draft = null }) { Text("›") }
            }
            Text("${month.size} journeys · ${tours.size} tours · ${daDays.size} DA days",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Entry form
            draft?.let { d ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (editing >= 0) "Edit journey" else "New journey", fontWeight = FontWeight.Bold)
                        fun upd(f: (JourneyRow) -> JourneyRow) { draft = f(d) }
                        DateField("Departure date and time", d.depDateTime, { v -> upd { it.copy(depDateTime = v) } }, withTime = true)
                        Field("From", d.fromPlace, { v -> upd { it.copy(fromPlace = v) } })
                        DateField("Arrival date and time", d.arrDateTime, { v -> upd { it.copy(arrDateTime = v) } }, withTime = true)
                        Field("To", d.toPlace, { v -> upd { it.copy(toPlace = v) } })
                        SuggestField("Mode of travel and class", d.modeClass, MODES, { v -> upd { it.copy(modeClass = v) } })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MoneyField("Fare paid", d.fare, { v -> upd { it.copy(fare = v) } }, modifier = Modifier.weight(1f))
                            Field("Distance Km", d.distanceKm, { v -> upd { it.copy(distanceKm = v) } },
                                kind = FieldKind.NUMBER, modifier = Modifier.weight(1f))
                        }
                        SuggestField("Work type", d.workType, com.postaltools.tr25.ui.edit.WORK_TYPES, { v -> upd { it.copy(workType = v) } })
                        Field("Office / establishment inspected or visited", d.office, { v -> upd { it.copy(office = v) } })
                        Field("Case No. / reference (if any)", d.caseRef, { v -> upd { it.copy(caseRef = v) } })
                        SuggestField("Purpose / work done", d.purpose, PURPOSES, { v -> upd { it.copy(purpose = v) } })
                        Field("Remarks", d.remarks, { v -> upd { it.copy(remarks = v) } }, singleLine = false)
                        var err by remember { mutableStateOf("") }
                        if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val dep = Dates.parseDateTime(d.depDateTime)
                                val arr = Dates.parseDateTime(d.arrDateTime)
                                err = when {
                                    d.fromPlace.isBlank() || d.toPlace.isBlank() -> "Enter From and To"
                                    dep == null || arr == null -> "Pick departure and arrival date and time"
                                    arr.isBefore(dep) -> "Arrival is before departure"
                                    else -> ""
                                }
                                if (err.isEmpty()) {
                                    val list = all.toMutableList()
                                    if (editing >= 0) list[editing] = d else list.add(d)
                                    persist(list)
                                    ym = YearMonth.from(dep!!)
                                    draft = null; editing = -1
                                }
                            }, Modifier.weight(1f)) { Text("SAVE") }
                            OutlinedButton(onClick = { draft = null; editing = -1 }, Modifier.weight(1f)) { Text("CANCEL") }
                            if (editing >= 0) IconButton(onClick = { confirmDelete = true }) {
                                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            if (draft == null) {
                val last = month.lastOrNull()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        startNew(JourneyRow(fromPlace = if (last == null || Tours.isHq(last.toPlace, hq)) hq else last.toPlace,
                            modeClass = last?.modeClass ?: ""))
                    }, Modifier.weight(1f)) { Text("+ Journey") }
                    OutlinedButton(onClick = {
                        startNew(JourneyRow(fromPlace = last?.toPlace ?: "", toPlace = hq, modeClass = last?.modeClass ?: "",
                            fare = last?.fare ?: "", distanceKm = last?.distanceKm ?: "", purpose = "Return to headquarters"))
                    }, Modifier.weight(1f), enabled = last != null) { Text("+ Return to HQ") }
                }
            }

            if (month.isEmpty()) Text("No journeys in ${ym.format(MONTH)}. Tap + Journey to add one.",
                Modifier.padding(vertical = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Tours with their journeys
            val tf = DateTimeFormatter.ofPattern("dd MMM, HH:mm")
            val df = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            tours.forEach { t ->
                val ok = t.returnedToHq && t.startsFromHq
                Card(colors = CardDefaults.cardColors(
                    containerColor = if (ok) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tour ${t.no} · left HQ ${t.start.format(tf)} → " +
                            (if (t.returnedToHq) "reached HQ ${t.end.format(tf)}" else "return to HQ not entered"),
                            fontWeight = FontWeight.Bold)
                        val days = daDays.filter { t.no in it.tours }
                        Text("${Tours.hm(t.minutes)} away · DA: " + days.joinToString(", ") { "${it.date.format(df)} ${it.pct}%" },
                            style = MaterialTheme.typography.bodyMedium)
                        if (!t.startsFromHq) Text("First journey is not from HQ", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium)
                        t.legs.forEach { j ->
                            val idx = all.indexOf(j)
                            Card(
                                onClick = { editing = idx; draft = j },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text("${j.fromPlace} ${j.depDateTime.substringAfter(' ')} → ${j.toPlace} ${j.arrDateTime.substringAfter(' ')}",
                                        fontWeight = FontWeight.SemiBold)
                                    Text(listOf(j.depDateTime.substringBefore(' '), j.modeClass,
                                        Money.parse(j.fare)?.takeIf { it.signum() > 0 }?.let { "₹" + Money.format(it) } ?: "",
                                        if (j.distanceKm.isNotBlank()) "${j.distanceKm} km" else "", j.purpose)
                                        .filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                                    val work = listOf(j.workType, j.office, if (j.caseRef.isNotBlank()) "Case: ${j.caseRef}" else "")
                                        .filter { it.isNotBlank() }.joinToString(" · ")
                                    if (work.isNotBlank()) Text(work, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    if (j.remarks.isNotBlank()) Text("Remarks: ${j.remarks}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this journey?") },
            confirmButton = {
                TextButton(onClick = {
                    if (editing >= 0) persist(all.toMutableList().also { it.removeAt(editing) })
                    confirmDelete = false; draft = null; editing = -1
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
