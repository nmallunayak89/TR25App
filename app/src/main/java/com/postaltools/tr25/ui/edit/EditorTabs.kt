package com.postaltools.tr25.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.postaltools.tr25.data.DaSlab
import com.postaltools.tr25.data.HigherClassRow
import com.postaltools.tr25.data.HotelRow
import com.postaltools.tr25.data.JourneyRow
import com.postaltools.tr25.data.RoadRow
import com.postaltools.tr25.logic.Money
import com.postaltools.tr25.ui.components.DateField
import com.postaltools.tr25.ui.components.Field
import com.postaltools.tr25.ui.components.FieldKind
import com.postaltools.tr25.ui.components.MoneyField
import com.postaltools.tr25.ui.components.Section
import com.postaltools.tr25.ui.components.SuggestField
import com.postaltools.tr25.ui.components.YesNo

private fun <T> List<T>.set(i: Int, v: T): List<T> = toMutableList().also { it[i] = v }
private fun <T> List<T>.without(i: Int): List<T> = toMutableList().also { it.removeAt(i) }

val DESIGNATIONS = listOf(
    "Postal Assistant", "Sorting Assistant", "Postman", "Mail Guard", "Multi Tasking Staff",
    "Gramin Dak Sevak", "Branch Postmaster", "Sub Postmaster", "Postmaster", "Senior Postmaster",
    "Inspector of Posts", "Assistant Superintendent of Posts", "Superintendent of Posts",
)
val MODES = listOf(
    "Rail - Sleeper Class", "Rail - AC 3 Tier", "Rail - AC 2 Tier", "Rail - AC Chair Car", "Rail - Second Class",
    "Bus - Ordinary", "Bus - Express", "Bus - AC", "Air - Economy", "Taxi", "Auto rickshaw",
    "Own vehicle", "Departmental vehicle",
)
val PURPOSES = listOf("Inspection", "Training", "Official meeting", "Investigation", "Audit", "Return to headquarters")
val TRAIN_TYPES = listOf("Mail", "Express", "Superfast", "Ordinary")
val CONVEYANCE = listOf(
    "Govt. transport", "Taxi", "Single seat in bus", "Other public conveyance",
    "Shared car of another Govt. servant",
)

@Composable
private fun RowCard(title: String, onDelete: (() -> Unit)?, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove $title", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun AddButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Text(text, Modifier.padding(start = 6.dp))
    }
}

// ------------------------------------------------------------------ Items 1–4

@Composable
fun BasicTab(vm: EditorViewModel) {
    val b = vm.bill
    Section("Bill reference", "Office name is kept for searching; it is not printed on the form.") {
        Field("Sub-Bill No.", b.subBillNo, { v -> vm.update { it.copy(subBillNo = v) } }, error = vm.errorFor("subBillNo"))
        Field("Office name", b.officeName, { v -> vm.update { it.copy(officeName = v) } })
        DateField("Date of claim (under the certificate)", b.claimDate, { v -> vm.update { it.copy(claimDate = v) } },
            error = vm.errorFor("claimDate"))
    }
    Section("PART A — Government servant") {
        Field("1. Name *", b.name, { v -> vm.update { it.copy(name = v) } }, error = vm.errorFor("name"))
        SuggestField("2. Designation *", b.designation, DESIGNATIONS, { v -> vm.update { it.copy(designation = v) } },
            error = vm.errorFor("designation"))
        MoneyField("3. Pay *", b.pay, { v -> vm.update { it.copy(pay = v) } }, error = vm.errorFor("pay"))
        Field("4. Headquarters *", b.headquarters, { v -> vm.update { it.copy(headquarters = v) } },
            error = vm.errorFor("headquarters"))
    }
}

// ------------------------------------------------------------------ Item 5

@Composable
fun JourneysTab(vm: EditorViewModel) {
    val b = vm.bill
    Section("5. Details and purpose of journey(s) performed",
        "One card per journey. Fares and distances add up into Part B automatically.") {
        vm.errorFor("journeys")?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        b.journeys.forEachIndexed { i, j ->
            fun upd(f: (JourneyRow) -> JourneyRow) = vm.update { it.copy(journeys = it.journeys.set(i, f(it.journeys[i]))) }
            RowCard("Journey ${i + 1}", if (b.journeys.size > 1) ({ vm.update { it.copy(journeys = it.journeys.without(i)) } }) else null) {
                Text("Departure", style = MaterialTheme.typography.labelLarge)
                DateField("(1) Date and time", j.depDateTime, { v -> upd { it.copy(depDateTime = v) } }, withTime = true,
                    error = vm.errorFor("journey.$i.depDateTime"))
                Field("(2) From *", j.fromPlace, { v -> upd { it.copy(fromPlace = v) } }, error = vm.errorFor("journey.$i.fromPlace"))
                Text("Arrival", style = MaterialTheme.typography.labelLarge)
                DateField("(3) Date and time", j.arrDateTime, { v -> upd { it.copy(arrDateTime = v) } }, withTime = true,
                    error = vm.errorFor("journey.$i.arrDateTime"))
                Field("(4) To *", j.toPlace, { v -> upd { it.copy(toPlace = v) } }, error = vm.errorFor("journey.$i.toPlace"))
                SuggestField("(5) Mode of travel and class", j.modeClass, MODES, { v -> upd { it.copy(modeClass = v) } })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneyField("(6) Fare paid", j.fare, { v -> upd { it.copy(fare = v) } },
                        error = vm.errorFor("journey.$i.fare"), modifier = Modifier.weight(1f))
                    Field("(7) Distance Km", j.distanceKm, { v -> upd { it.copy(distanceKm = v) } },
                        kind = FieldKind.NUMBER, error = vm.errorFor("journey.$i.distanceKm"), modifier = Modifier.weight(1f))
                }
                Text("(8) Duration of halt", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Days", j.haltDays, { v -> upd { it.copy(haltDays = v) } }, kind = FieldKind.WHOLE,
                        error = vm.errorFor("journey.$i.haltDays"), modifier = Modifier.weight(1f))
                    Field("Hours", j.haltHours, { v -> upd { it.copy(haltHours = v) } }, kind = FieldKind.WHOLE,
                        error = vm.errorFor("journey.$i.haltHours"), modifier = Modifier.weight(1f))
                }
                SuggestField("(9) Purpose of journey", j.purpose, PURPOSES, { v -> upd { it.copy(purpose = v) } })
            }
        }
        AddButton("Add journey") { vm.update { it.copy(journeys = it.journeys + JourneyRow()) } }
        AddButton("Add return journey (swap From / To of last)") {
            vm.update {
                val last = it.journeys.lastOrNull() ?: JourneyRow()
                it.copy(journeys = it.journeys + JourneyRow(
                    fromPlace = last.toPlace, toPlace = last.fromPlace, modeClass = last.modeClass,
                    fare = last.fare, distanceKm = last.distanceKm, purpose = "Return to headquarters",
                ))
            }
        }
    }
}

// ------------------------------------------------------------------ Items 6–8

@Composable
fun DetailsTab(vm: EditorViewModel) {
    val b = vm.bill
    Section("6. Mode of Journey", "Fill only the parts that apply.") {
        Text("(i) Air", style = MaterialTheme.typography.labelLarge)
        YesNo("(a) Exchange voucher arranged by officer", b.airVoucherByOfficer) { v -> vm.update { it.copy(airVoucherByOfficer = v) } }
        Field("(b) Ticket/Exchange voucher arranged by", b.airTicketArrangedBy, { v -> vm.update { it.copy(airTicketArrangedBy = v) } })
        HorizontalDivider()
        Text("(ii) Rail", style = MaterialTheme.typography.labelLarge)
        SuggestField("(a) Mail / express / ordinary train?", b.railTrainType, TRAIN_TYPES, { v -> vm.update { it.copy(railTrainType = v) } })
        YesNo("(b) Whether return tickets available?", b.railReturnAvailable) { v -> vm.update { it.copy(railReturnAvailable = v) } }
        YesNo("(c) If available, whether return tickets purchased?", b.railReturnPurchased) { v -> vm.update { it.copy(railReturnPurchased = v) } }
        if (b.railReturnPurchased == "No") {
            Field("If not, state reasons", b.railNotPurchasedReason, { v -> vm.update { it.copy(railNotPurchasedReason = v) } }, singleLine = false)
        }
        HorizontalDivider()
        Text("(iii) Road", style = MaterialTheme.typography.labelLarge)
        SuggestField("Mode of conveyance used", b.roadConveyance, CONVEYANCE, { v -> vm.update { it.copy(roadConveyance = v) } })
    }
    Section("7. Dates of absence from place of halt", "Type dates, e.g. 05-10-2026, 06-10-2026") {
        Field("(a) R.H. and C.L.", b.absenceRhCl, { v -> vm.update { it.copy(absenceRhCl = v) } })
        Field("(b) Not in camp on Sundays and Holidays", b.absenceNotInCamp, { v -> vm.update { it.copy(absenceNotInCamp = v) } })
    }
    Section("8. Dates of free board and/or lodging provided by the State") {
        Field("(a) Board only", b.freeBoardOnly, { v -> vm.update { it.copy(freeBoardOnly = v) } })
        Field("(b) Lodging only", b.freeLodgingOnly, { v -> vm.update { it.copy(freeLodgingOnly = v) } })
        Field("(c) Board and lodging", b.freeBoardAndLodging, { v -> vm.update { it.copy(freeBoardAndLodging = v) } })
    }
}

// ------------------------------------------------------------------ Items 9–12

@Composable
fun OtherTab(vm: EditorViewModel) {
    val b = vm.bill
    Section("9. Hotel stay (higher rate of D.A. claimed)", "Leave empty if not applicable — the bill prints \"Nil\".") {
        b.hotels.forEachIndexed { i, h ->
            fun upd(f: (HotelRow) -> HotelRow) = vm.update { it.copy(hotels = it.hotels.set(i, f(it.hotels[i]))) }
            RowCard("Hotel ${i + 1}", { vm.update { it.copy(hotels = it.hotels.without(i)) } }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) { DateField("From", h.fromDate, { v -> upd { it.copy(fromDate = v) } }) }
                    Column(Modifier.weight(1f)) {
                        DateField("To", h.toDate, { v -> upd { it.copy(toDate = v) } }, error = vm.errorFor("hotel.$i.toDate"))
                    }
                }
                Field("Name of hotel", h.hotelName, { v -> upd { it.copy(hotelName = v) } })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneyField("Daily rate", h.dailyRate, { v -> upd { it.copy(dailyRate = v) } },
                        error = vm.errorFor("hotel.$i.dailyRate"), modifier = Modifier.weight(1f))
                    MoneyField("Total paid", h.totalPaid, { v -> upd { it.copy(totalPaid = v) } },
                        error = vm.errorFor("hotel.$i.totalPaid"), modifier = Modifier.weight(1f))
                }
            }
        }
        if (b.hotels.isNotEmpty()) Text("Total paid for hotels: ${Money.rupees(vm.totals.hotelTotal)}")
        AddButton("Add hotel stay") { vm.update { it.copy(hotels = it.hotels + HotelRow()) } }
    }

    Section("10. Journeys in a higher class than entitled") {
        b.higherClass.forEachIndexed { i, r ->
            fun upd(f: (HigherClassRow) -> HigherClassRow) = vm.update { it.copy(higherClass = it.higherClass.set(i, f(it.higherClass[i]))) }
            RowCard("Row ${i + 1}", { vm.update { it.copy(higherClass = it.higherClass.without(i)) } }) {
                DateField("Date", r.date, { v -> upd { it.copy(date = v) } })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("From", r.fromPlace, { v -> upd { it.copy(fromPlace = v) } }, modifier = Modifier.weight(1f))
                    Field("To", r.toPlace, { v -> upd { it.copy(toPlace = v) } }, modifier = Modifier.weight(1f))
                }
                SuggestField("Mode of conveyance used", r.mode, MODES, { v -> upd { it.copy(mode = v) } })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Class entitled", r.classEntitled, { v -> upd { it.copy(classEntitled = v) } }, modifier = Modifier.weight(1f))
                    Field("Class travelled", r.classTravelled, { v -> upd { it.copy(classTravelled = v) } }, modifier = Modifier.weight(1f))
                }
                MoneyField("Fare of the entitled class", r.entitledFare, { v -> upd { it.copy(entitledFare = v) } },
                    error = vm.errorFor("higher.$i.entitledFare"))
            }
        }
        AddButton("Add higher-class journey") { vm.update { it.copy(higherClass = it.higherClass + HigherClassRow()) } }
        Field("Sanction No. and date (if approved by competent authority)", b.higherClassSanction,
            { v -> vm.update { it.copy(higherClassSanction = v) } })
    }

    Section("11. Road journeys between places connected by rail") {
        b.roadJourneys.forEachIndexed { i, r ->
            fun upd(f: (RoadRow) -> RoadRow) = vm.update { it.copy(roadJourneys = it.roadJourneys.set(i, f(it.roadJourneys[i]))) }
            RowCard("Road journey ${i + 1}", { vm.update { it.copy(roadJourneys = it.roadJourneys.without(i)) } }) {
                DateField("Date", r.date, { v -> upd { it.copy(date = v) } })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("From", r.fromPlace, { v -> upd { it.copy(fromPlace = v) } }, modifier = Modifier.weight(1f))
                    Field("To", r.toPlace, { v -> upd { it.copy(toPlace = v) } }, modifier = Modifier.weight(1f))
                }
                MoneyField("Fare paid", r.farePaid, { v -> upd { it.copy(farePaid = v) } }, error = vm.errorFor("road.$i.farePaid"))
            }
        }
        AddButton("Add road journey") { vm.update { it.copy(roadJourneys = it.roadJourneys + RoadRow()) } }
    }

    Section("12. T.A. advance") {
        MoneyField("Amount of T.A. advance drawn (if any)", b.advanceAmount, { v -> vm.update { it.copy(advanceAmount = v) } },
            error = vm.errorFor("advanceAmount"))
        Text("This amount is deducted in Part B (e).", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ------------------------------------------------------------------ Part B

@Composable
private fun CalcLine(label: String, amount: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(amount, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun PartBTab(vm: EditorViewModel) {
    val b = vm.bill
    val t = vm.totals
    Section("PART B — Calculation", "Totals update as you type. All rates are entered by you; please verify them.") {
        Text("(a) Railways/Air/Bus/Steamer fare", style = MaterialTheme.typography.labelLarge)
        Text("Total of fares in item 5: ${Money.rupees(t.fareAuto)}", style = MaterialTheme.typography.bodyMedium)
        MoneyField("Different amount for (a)? (leave blank to use the total)", b.fareOverride,
            { v -> vm.update { it.copy(fareOverride = v) } }, error = vm.errorFor("fareOverride"))

        HorizontalDivider()
        Text("(b) Road mileage", style = MaterialTheme.typography.labelLarge)
        Text("Total distance in item 5: ${Money.qty(t.kmAuto)} Km", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Kms (blank = total)", b.mileageKmOverride, { v -> vm.update { it.copy(mileageKmOverride = v) } },
                kind = FieldKind.NUMBER, error = vm.errorFor("mileageKmOverride"), modifier = Modifier.weight(1f))
            MoneyField("Rate per Km", b.mileageRate, { v -> vm.update { it.copy(mileageRate = v) } },
                error = vm.errorFor("mileageRate"), modifier = Modifier.weight(1f))
        }

        HorizontalDivider()
        Text("(c) Daily allowance", style = MaterialTheme.typography.labelLarge)
        val roman = listOf("(i)", "(ii)", "(iii)", "(iv)", "(v)", "(vi)")
        b.daSlabs.forEachIndexed { i, s ->
            fun upd(f: (DaSlab) -> DaSlab) = vm.update { it.copy(daSlabs = it.daSlabs.set(i, f(it.daSlabs[i]))) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(roman.getOrElse(i) { "(${i + 1})" }, Modifier.padding(end = 2.dp))
                Field("Days", s.days, { v -> upd { it.copy(days = v) } }, kind = FieldKind.NUMBER,
                    error = vm.errorFor("da.$i.days"), modifier = Modifier.weight(1f))
                MoneyField("Rate/day", s.rate, { v -> upd { it.copy(rate = v) } },
                    error = vm.errorFor("da.$i.rate"), modifier = Modifier.weight(1.3f))
                if (i >= 3) IconButton(onClick = { vm.update { it.copy(daSlabs = it.daSlabs.without(i)) } }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove line", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (b.daSlabs.size < 6) AddButton("Add another DA line") { vm.update { it.copy(daSlabs = it.daSlabs + DaSlab()) } }

        HorizontalDivider()
        MoneyField("(d) Actual expenses", b.actualExpenses, { v -> vm.update { it.copy(actualExpenses = v) } },
            error = vm.errorFor("actualExpenses"))

        HorizontalDivider()
        Text("(e) T.A. advance (from item 12): ${Money.rupees(t.advance)}", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Voucher No.", b.advanceVoucherNo, { v -> vm.update { it.copy(advanceVoucherNo = v) } }, modifier = Modifier.weight(1f))
            Column(Modifier.weight(1.3f)) {
                DateField("Voucher date", b.advanceVoucherDate, { v -> vm.update { it.copy(advanceVoucherDate = v) } })
            }
        }
        Field("The expenditure is debitable to (Head of account)", b.debitableTo,
            { v -> vm.update { it.copy(debitableTo = v) } }, singleLine = false)
    }

    Section("Check before generating") {
        CalcLine("(a) Fare", Money.rupees(t.fare))
        CalcLine("(b) Mileage ${Money.qty(t.km)} Km × ${Money.rupees(t.mileageRate)}", Money.rupees(t.mileage))
        t.daLines.forEachIndexed { i, v ->
            if (v.signum() != 0) CalcLine("(c) DA line ${i + 1}", Money.rupees(v))
        }
        CalcLine("(c) Daily allowance total", Money.rupees(t.daTotal))
        CalcLine("(d) Actual expenses", Money.rupees(t.actualExpenses))
        HorizontalDivider()
        CalcLine("Gross amount", Money.rupees(t.gross), bold = true)
        CalcLine("(e) Less T.A. advance", "− " + Money.rupees(t.advance))
        HorizontalDivider()
        CalcLine("Net amount", Money.rupees(t.net), bold = true)
        t.warnings.forEach {
            Text("⚠ $it", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyMedium)
        }
        if (t.errors.isNotEmpty()) {
            Text("Some numbers are not valid and are counted as ₹0 until corrected:",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            t.errors.values.take(8).forEach {
                Text("• $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
