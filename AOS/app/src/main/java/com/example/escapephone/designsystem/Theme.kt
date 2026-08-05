package com.example.escapephone.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF080D14)
val Surface = Color(0xFF121B25)
val Primary = Color(0xFF35D0BA)
val Success = Color(0xFF56D364)
val Error = Color(0xFFFF6B6B)
val TextPrimary = Color(0xFFF4F7FA)
val TextSecondary = Color(0xFFAAB6C2)

@Composable fun EscapePhoneTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(primary = Primary, background = Background, surface = Surface, error = Error, onPrimary = Color.Black, onBackground = TextPrimary, onSurface = TextPrimary)
    MaterialTheme(colorScheme = colors, content = content)
}
