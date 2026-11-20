package com.postaltools.tr25.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.postaltools.tr25.TrApp
import com.postaltools.tr25.ui.components.Field
import com.postaltools.tr25.ui.components.MoneyField
import com.postaltools.tr25.ui.components.Section
import com.postaltools.tr25.ui.components.SuggestField
import com.postaltools.tr25.ui.edit.DESIGNATIONS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as TrApp
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var s by remember { mutableStateOf(app.settings.load()) }
    var confirmRestore by remember { mutableStateOf<android.net.Uri?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val text = app.repo.exportBackup()
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                        ?: error("Could not open the file")
                }
            }
            snack.showSnackbar(if (r.isSuccess) "Backup saved" else "Backup failed: ${r.exceptionOrNull()?.message}")
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmRestore = uri
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
        ) {
            Section("Defaults for new bills", "Filled in automatically on every new bill. You can still change them per bill.") {
                Field("Office name", s.officeName, { s = s.copy(officeName = it) })
                Field("Name", s.name, { s = s.copy(name = it) })
                SuggestField("Designation", s.designation, DESIGNATIONS, { s = s.copy(designation = it) })
                MoneyField("Pay", s.pay, { s = s.copy(pay = it) })
                Field("Headquarters", s.headquarters, { s = s.copy(headquarters = it) })
                Field("Expenditure debitable to (Head of account)", s.debitableTo, { s = s.copy(debitableTo = it) }, singleLine = false)
                MoneyField("Road mileage rate per Km", s.mileageRate, { s = s.copy(mileageRate = it) })
                MoneyField("Daily allowance rate per day", s.daRate, { s = s.copy(daRate = it) })
                Field("Road mileage limit per day (Km)", s.mileageCapKm, { s = s.copy(mileageCapKm = it) },
                    kind = com.postaltools.tr25.ui.components.FieldKind.NUMBER)
                Text(
                    "Rates are not built into the app. Enter the rates that apply to you as per current orders.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = {
                    app.settings.save(s)
                    scope.launch { snack.showSnackbar("Defaults saved") }
                }, Modifier.fillMaxWidth()) { Text("SAVE DEFAULTS") }
            }

            Section("Login PIN", "The app asks for this PIN every time it is opened.") {
                var newPin by remember { mutableStateOf("") }
                Field("New 4-digit PIN", newPin, { v -> newPin = v.filter { it.isDigit() }.take(4) },
                    kind = com.postaltools.tr25.ui.components.FieldKind.WHOLE)
                Button(onClick = {
                    if (newPin.length == 4) {
                        app.settings.setPin(newPin); newPin = ""
                        scope.launch { snack.showSnackbar("PIN changed") }
                    } else scope.launch { snack.showSnackbar("PIN must be 4 digits") }
                }, Modifier.fillMaxWidth()) { Text("CHANGE PIN") }
            }
            Section("Backup and restore", "A backup is a single file with all your bills. Keep it somewhere safe.") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { backupLauncher.launch("TR25_Backup_${LocalDate.now()}.json") },
                        modifier = Modifier.weight(1f),
                    ) { Text("BACKUP") }
                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        modifier = Modifier.weight(1f),
                    ) { Text("RESTORE") }
                }
            }

            Section("Data safety") {
                Text(
                    "• Bills are stored only inside this app on this phone.\n" +
                        "• The app has no internet permission and never uploads anything.\n" +
                        "• Bills are excluded from Android cloud backup; use Backup above instead.\n" +
                        "• Other apps cannot read your bills. A PDF leaves the app only when you download or share it.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                "TR-25 Bill Generator 1.0 · Form T.R.-25A / G.A.R.-14A",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }

    confirmRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text("Restore from backup?") },
            text = { Text("Bills in the backup will be added to the bills already on this phone. Nothing is deleted. " +
                "A final bill whose Sub-Bill No. already exists is restored as a draft.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = null
                    scope.launch {
                        val r = withContext(Dispatchers.IO) {
                            runCatching {
                                val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                                    ?: error("Could not open the file")
                                app.repo.importBackup(text)
                            }
                        }
                        snack.showSnackbar(
                            r.fold(
                                { (ok, skipped) -> "Restored $ok bill(s)" + if (skipped > 0) ", $skipped skipped" else "" },
                                { "Restore failed: ${it.message}" },
                            )
                        )
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Cancel") } },
        )
    }
}
