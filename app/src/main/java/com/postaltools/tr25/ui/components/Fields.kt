package com.postaltools.tr25.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.postaltools.tr25.logic.Dates
import java.time.LocalDate
import java.time.LocalDateTime

/** A titled white card that groups related fields. */
@Composable
fun Section(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

enum class FieldKind { TEXT, NUMBER, WHOLE }

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    kind: FieldKind = FieldKind.TEXT,
    error: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    prefix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v ->
            when (kind) {
                FieldKind.TEXT -> onChange(v)
                // Only allow characters that can make a number; the calculator does the final check.
                FieldKind.NUMBER -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' })
                FieldKind.WHOLE -> onChange(v.filter { it.isDigit() })
            }
        },
        modifier = modifier,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        prefix = if (prefix != null) { { Text(prefix) } } else null,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = when (kind) {
                FieldKind.TEXT -> KeyboardType.Text
                FieldKind.NUMBER -> KeyboardType.Decimal
                FieldKind.WHOLE -> KeyboardType.Number
            },
            capitalization = if (kind == FieldKind.TEXT) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            imeAction = ImeAction.Next,
        ),
    )
}

@Composable
fun MoneyField(label: String, value: String, onChange: (String) -> Unit, error: String? = null,
               modifier: Modifier = Modifier.fillMaxWidth(), placeholder: String? = null) =
    Field(label, value, onChange, modifier, FieldKind.NUMBER, error, placeholder, prefix = "₹ ")

/** Read-only field that opens the date (and optionally time) picker. Format dd-MM-yyyy[ HH:mm]. */
@Composable
fun DateField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    withTime: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val ctx = LocalContext.current
    fun open() {
        val now = LocalDateTime.now()
        val current = if (withTime) Dates.parseDateTime(value) else Dates.parseDate(value)?.atStartOfDay()
        val start = current ?: now
        DatePickerDialog(ctx, { _, y, m, d ->
            val date = LocalDate.of(y, m + 1, d)
            if (!withTime) {
                onChange(date.format(Dates.DATE))
            } else {
                TimePickerDialog(ctx, { _, h, min ->
                    onChange(date.atTime(h, min).format(Dates.DATE_TIME))
                }, start.hour, start.minute, true).show()
            }
        }, start.year, start.monthValue - 1, start.dayOfMonth).show()
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
                placeholder = { Text(if (withTime) "Tap to pick date & time" else "Tap to pick date") },
                trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                isError = error != null,
                supportingText = if (error != null) { { Text(error) } } else null,
                singleLine = true,
            )
            // Transparent layer so a tap anywhere on the field opens the picker.
            Box(Modifier.matchParentSize().clickable { open() })
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Clear, contentDescription = "Clear $label") }
        }
    }
}

/** Editable text field with a dropdown of common values (the user can still type anything). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestField(
    label: String,
    value: String,
    options: List<String>,
    onChange: (String) -> Unit,
    error: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            label = { Text(label) },
            singleLine = true,
            isError = error != null,
            supportingText = if (error != null) { { Text(error) } } else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        )
        val filtered = options.filter { value.isBlank() || it.contains(value, ignoreCase = true) }.ifEmpty { options }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            filtered.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onChange(opt); expanded = false })
            }
        }
    }
}

/** Yes / No choice; tapping the selected one again clears it. */
@Composable
fun YesNo(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Yes", "No").forEach { opt ->
                FilterChip(
                    selected = value == opt,
                    onClick = { onChange(if (value == opt) "" else opt) },
                    label = { Text(opt, fontWeight = FontWeight.SemiBold) },
                )
            }
        }
    }
}

@Composable
fun VSpace(h: Int = 8) = Spacer(Modifier.height(h.dp))
