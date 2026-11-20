package com.postaltools.tr25

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.postaltools.tr25.ui.AppNav
import com.postaltools.tr25.ui.theme.Tr25Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tr25Theme {
                AppNav()
            }
        }
    }
}
