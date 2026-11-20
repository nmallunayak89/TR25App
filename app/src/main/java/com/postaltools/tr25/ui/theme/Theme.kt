package com.postaltools.tr25.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PostalRed = Color(0xFFB71C1C)
val PostalRedDark = Color(0xFF8E0000)
val Ink = Color(0xFF263238)
val Paper = Color(0xFFF7F4EF)
val Amber = Color(0xFFFFB300)
val OkGreen = Color(0xFF2E7D32)

private val colors = lightColorScheme(
    primary = PostalRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF455A64),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E8EB),
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF7A5900),
    tertiaryContainer = Color(0xFFFFE08A),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDE7E3),
    onSurfaceVariant = Color(0xFF52443F),
    error = Color(0xFFBA1A1A),
)

private val type = Typography(
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun Tr25Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = type, content = content)
}
