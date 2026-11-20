package com.postaltools.tr25

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.postaltools.tr25.ui.AppNav
import com.postaltools.tr25.ui.lock.LockScreen
import com.postaltools.tr25.ui.theme.Tr25Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tr25Theme {
                // Asks for the PIN each time the app is started.
                var unlocked by rememberSaveable { mutableStateOf(false) }
                if (unlocked) AppNav() else LockScreen(onUnlocked = { unlocked = true })
            }
        }
    }
}
