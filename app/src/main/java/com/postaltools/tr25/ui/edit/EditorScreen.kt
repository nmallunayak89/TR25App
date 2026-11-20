package com.postaltools.tr25.ui.edit

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.postaltools.tr25.TrApp
import com.postaltools.tr25.data.BillStatus
import com.postaltools.tr25.logic.Issue
import com.postaltools.tr25.logic.Money
import kotlinx.coroutines.launch

private val TABS = listOf("1–4 Basic", "5 Journeys", "6–8 Details", "9–12 Other", "Part B Totals")

/** Which tab holds the field with this error key. */
private fun tabFor(key: String): Int = when {
    key.startsWith("journey") -> 1
    key.startsWith("hotel") || key.startsWith("higher") || key.startsWith("road") || key == "advanceAmount" -> 3
    key.startsWith("da.") || key in setOf("fareOverride", "mileageKmOverride", "mileageRate", "actualExpenses") -> 4
    else -> 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(billId: Long, onBack: () -> Unit, onGenerated: (Long) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as TrApp
    val vm: EditorViewModel = viewModel(key = "editor-$billId", factory = EditorViewModel.factory(app, billId))
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var problems by remember { mutableStateOf<List<Issue>?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    BackHandler(enabled = vm.dirty) { confirmLeave = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (billId == 0L) "New TR-25 Bill" else "Edit TR-25 Bill")
                        Text(
                            if (vm.bill.status == BillStatus.FINAL) "Final · T.R.-25A" else "Draft · T.R.-25A",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (vm.dirty) confirmLeave = true else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    val t = vm.totals
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross ${Money.rupees(t.gross)}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Net ${Money.rupees(t.net)}", style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            enabled = !busy,
                            onClick = {
                                scope.launch {
                                    busy = true
                                    runCatching { vm.saveDraft() }
                                        .onSuccess { Toast.makeText(ctx, "Draft saved", Toast.LENGTH_SHORT).show() }
                                        .onFailure { Toast.makeText(ctx, "Could not save: ${it.message}", Toast.LENGTH_LONG).show() }
                                    busy = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("SAVE DRAFT") }
                        Button(
                            enabled = !busy,
                            onClick = {
                                scope.launch {
                                    busy = true
                                    when (val r = vm.generate()) {
                                        is GenerateResult.Ok -> onGenerated(r.id)
                                        is GenerateResult.Problems -> problems = r.issues
                                    }
                                    busy = false
                                }
                            },
                            modifier = Modifier.weight(1.4f),
                        ) { Text("GENERATE TR-25 BILL") }
                    }
                }
            }
        },
    ) { pad ->
        if (!vm.loaded) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(Modifier.padding(pad).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                TABS.forEachIndexed { i, title ->
                    val hasError = vm.issues.keys.any { tabFor(it) == i } || vm.totals.errors.keys.any { tabFor(it) == i }
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = { Text(if (hasError) "$title ⚠" else title) },
                    )
                }
            }
            HorizontalDivider()
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                when (tab) {
                    0 -> BasicTab(vm)
                    1 -> JourneysTab(vm)
                    2 -> DetailsTab(vm)
                    3 -> OtherTab(vm)
                    else -> PartBTab(vm)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (tab > 0) TextButton(onClick = { tab-- }) { Text("‹ ${TABS[tab - 1]}") } else Box(Modifier)
                    if (tab < TABS.lastIndex) TextButton(onClick = { tab++ }) { Text("${TABS[tab + 1]} ›") }
                }
            }
        }
    }

    problems?.let { list ->
        AlertDialog(
            onDismissRequest = { problems = null },
            title = { Text("Please correct ${list.size} item${if (list.size == 1) "" else "s"}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    list.take(15).forEach { Text("• ${it.message}", Modifier.padding(vertical = 2.dp)) }
                    if (list.size > 15) Text("…and ${list.size - 15} more")
                }
            },
            confirmButton = {
                TextButton(onClick = { tab = tabFor(list.first().key); problems = null }) { Text("Go to first problem") }
            },
            dismissButton = { TextButton(onClick = { problems = null }) { Text("Close") } },
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave without saving?") },
            text = { Text("You have changes that are not saved.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    scope.launch {
                        runCatching { vm.saveDraft() }
                        Toast.makeText(ctx, "Draft saved", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                }) { Text("Save draft & leave") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmLeave = false; onBack() }) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { confirmLeave = false }) { Text("Stay") }
                }
            },
        )
    }
}
