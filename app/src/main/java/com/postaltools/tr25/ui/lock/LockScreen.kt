package com.postaltools.tr25.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.postaltools.tr25.TrApp
import com.postaltools.tr25.ui.theme.PostalRed

private enum class Mode { SETUP, UNLOCK, RESET }

/** Offline PIN login. First run creates the PIN and a recovery word. */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val app = LocalContext.current.applicationContext as TrApp
    var mode by remember { mutableStateOf(if (app.settings.hasPin()) Mode.UNLOCK else Mode.SETUP) }
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var word by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }

    @Composable
    fun pinField(label: String, v: String, set: (String) -> Unit) = OutlinedTextField(
        value = v, onValueChange = { x -> set(x.filter { it.isDigit() }.take(4)) },
        label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )

    @Composable
    fun textField(label: String, v: String, set: (String) -> Unit) = OutlinedTextField(
        value = v, onValueChange = set, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    )

    Box(Modifier.fillMaxSize().background(PostalRed).imePadding(), contentAlignment = Alignment.Center) {
        Card(Modifier.padding(20.dp).widthIn(max = 420.dp)) {
            Column(
                Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("TR-25 Bill Generator", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = PostalRed)
                when (mode) {
                    Mode.SETUP -> {
                        Text("Create a 4-digit PIN to protect your bills.", style = MaterialTheme.typography.bodyMedium)
                        pinField("New PIN", pin) { pin = it }
                        pinField("Confirm PIN", pin2) { pin2 = it }
                        textField("Recovery word (to reset a forgotten PIN)", word) { word = it }
                        Button(onClick = {
                            err = when {
                                pin.length != 4 -> "PIN must be 4 digits"
                                pin != pin2 -> "PINs do not match"
                                word.trim().length < 3 -> "Recovery word: at least 3 letters"
                                else -> { app.settings.setPin(pin, word); onUnlocked(); "" }
                            }
                        }, Modifier.fillMaxWidth()) { Text("CREATE LOGIN") }
                    }
                    Mode.UNLOCK -> {
                        Text("Enter your PIN", style = MaterialTheme.typography.bodyMedium)
                        pinField("PIN", pin) {
                            pin = it
                            if (it.length == 4) {
                                if (app.settings.checkPin(it)) onUnlocked() else { err = "Wrong PIN"; pin = "" }
                            }
                        }
                        Button(onClick = {
                            if (app.settings.checkPin(pin)) onUnlocked() else { err = "Wrong PIN"; pin = "" }
                        }, Modifier.fillMaxWidth()) { Text("LOGIN") }
                        TextButton(onClick = { mode = Mode.RESET; err = ""; pin = "" }) { Text("Forgot PIN?") }
                    }
                    Mode.RESET -> {
                        textField("Recovery word", word) { word = it }
                        pinField("New PIN", pin) { pin = it }
                        Button(onClick = {
                            err = when {
                                !app.settings.checkRecovery(word) -> "Recovery word does not match"
                                pin.length != 4 -> "PIN must be 4 digits"
                                else -> { app.settings.setPin(pin); onUnlocked(); "" }
                            }
                        }, Modifier.fillMaxWidth()) { Text("SET NEW PIN") }
                        TextButton(onClick = { mode = Mode.UNLOCK; err = "" }) { Text("Back") }
                    }
                }
                if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
