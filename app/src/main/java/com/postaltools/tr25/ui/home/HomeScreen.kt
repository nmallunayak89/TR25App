package com.postaltools.tr25.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.postaltools.tr25.TrApp
import com.postaltools.tr25.ui.list.ListMode
import com.postaltools.tr25.ui.theme.PostalRed
import com.postaltools.tr25.ui.theme.PostalRedDark

@Composable
fun HomeScreen(onNew: () -> Unit, onList: (ListMode) -> Unit, onSettings: () -> Unit, onDiary: () -> Unit) {
    val app = LocalContext.current.applicationContext as TrApp
    val bills by remember { app.repo.observeAll() }.collectAsState(initial = emptyList())
    val drafts = bills.count { it.status == "DRAFT" }
    val finals = bills.size - drafts

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Banner
        Column(
            Modifier.fillMaxWidth().background(PostalRed).statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Text("TR-25 BILL GENERATOR", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(
                "T.R.-25A / G.A.R.-14A · Travelling Allowance Bill for Tour",
                color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("$finals", "Final bills")
                Stat("$drafts", "Drafts")
            }
        }

        Column(Modifier.padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("★", "Tour Diary", "Enter tours day by day, month-wise; make the bill from it", primary = true, onClick = onDiary)
            Tile("A", "Create New TR-25 Bill", "Enter the details once; totals are worked out for you", primary = true, onClick = onNew)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile("B", "View Saved Bills", "Search, open, copy or delete", Modifier.weight(1f)) { onList(ListMode.VIEW) }
                Tile("C", "Edit Existing Bill", "Correct or complete a bill", Modifier.weight(1f)) { onList(ListMode.EDIT) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile("D", "Generate PDF", "Make the A4 bill to download", Modifier.weight(1f)) { onList(ListMode.PDF) }
                Tile("E", "Print Bill", "Send to a printer", Modifier.weight(1f)) { onList(ListMode.PRINT) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile("F", "Export / Share Bill", "WhatsApp, e-mail, Drive…", Modifier.weight(1f)) { onList(ListMode.SHARE) }
                Tile("G", "Settings", "Your defaults, backup & restore", Modifier.weight(1f), onClick = onSettings)
            }
            Text(
                "All bills are stored only on this phone. The app works without internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(
        Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(label, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Tile(
    letter: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (primary) 96.dp else 128.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (primary) PostalRedDark else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(34.dp).background(
                    if (primary) Color.White else MaterialTheme.colorScheme.primaryContainer, CircleShape
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    title, style = MaterialTheme.typography.titleMedium,
                    color = if (primary) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = if (primary) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
