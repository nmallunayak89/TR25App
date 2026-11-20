package com.postaltools.tr25.ui.list

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.postaltools.tr25.TrApp
import com.postaltools.tr25.data.BillEntity
import com.postaltools.tr25.ui.theme.OkGreen
import kotlinx.coroutines.launch

/** What tapping a bill does, depending on which home-screen button opened the list. */
enum class ListMode(val title: String, val hint: String) {
    VIEW("Saved Bills", "Tap a bill to open it"),
    EDIT("Edit Existing Bill", "Tap the bill you want to change"),
    PDF("Generate PDF", "Tap a final bill to make its PDF"),
    PRINT("Print Bill", "Tap a final bill to print it"),
    SHARE("Export / Share Bill", "Tap a final bill to share its PDF"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillListScreen(
    mode: ListMode,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onPreview: (Long, String) -> Unit,
    onNew: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as TrApp
    val scope = rememberCoroutineScope()
    val all by remember { app.repo.observeAll() }.collectAsState(initial = null)
    var query by remember { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<BillEntity?>(null) }

    val shown = all.orEmpty().filter { b ->
        query.isBlank() || listOf(b.subBillNo, b.claimDate, b.officeName, b.officialName, b.designation)
            .any { it.contains(query.trim(), ignoreCase = true) }
    }

    fun open(b: BillEntity) {
        val isFinal = b.status == "FINAL"
        when (mode) {
            ListMode.EDIT -> onEdit(b.id)
            ListMode.VIEW -> if (isFinal) onPreview(b.id, "") else onEdit(b.id)
            ListMode.PDF, ListMode.PRINT, ListMode.SHARE -> {
                if (!isFinal) {
                    Toast.makeText(ctx, "This is a draft. Complete it and tap GENERATE TR-25 BILL first.", Toast.LENGTH_LONG).show()
                    onEdit(b.id)
                } else {
                    onPreview(b.id, when (mode) { ListMode.PRINT -> "print"; ListMode.SHARE -> "share"; else -> "" })
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mode.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New bill") },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text("Search bill no., date, office or name") },
                singleLine = true,
            )
            Text(
                mode.hint, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            when {
                all == null -> {}
                shown.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (all.orEmpty().isEmpty()) "No bills yet. Tap \"New bill\" to create your first TR-25 bill."
                        else "No bills match \"$query\".",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(shown, key = { it.id }) { b ->
                        BillCard(
                            b,
                            onOpen = { open(b) },
                            onEdit = { onEdit(b.id) },
                            onDuplicate = {
                                scope.launch {
                                    val newId = app.repo.duplicate(b.id)
                                    Toast.makeText(ctx, "Copied as a new draft", Toast.LENGTH_SHORT).show()
                                    onEdit(newId)
                                }
                            },
                            onDelete = { toDelete = b },
                        )
                    }
                }
            }
        }
    }

    toDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this bill?") },
            text = {
                Text("Sub-Bill No. ${b.subBillNo.ifBlank { "(none)" }} — ${b.officialName.ifBlank { "unnamed" }}.\n" +
                    "The bill and its PDF will be removed from this phone. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { app.repo.delete(b.id) }
                    toDelete = null
                    Toast.makeText(ctx, "Bill deleted", Toast.LENGTH_SHORT).show()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun BillCard(
    b: BillEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val isFinal = b.status == "FINAL"
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Sub-Bill No. ${b.subBillNo.ifBlank { "—" }}",
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false),
                    )
                    AssistChip(
                        onClick = onOpen,
                        label = { Text(if (isFinal) "FINAL" else "DRAFT", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.padding(start = 8.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = if (isFinal) OkGreen else MaterialTheme.colorScheme.tertiary,
                        ),
                    )
                }
                Text(
                    listOf(b.officialName, b.designation).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Name not entered" },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    listOf(b.claimDate, b.officeName).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Net ₹${b.netAmount}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More actions") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Open") }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Duplicate as new bill") }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
    }
}
