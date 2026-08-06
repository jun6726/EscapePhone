package com.example.escapephone.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Elevated = Color(0xFF1A2530)
val Hairline = Color(0x14FFFFFF)

@Composable
fun BackToThemeSelectionButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = Elevated, contentColor = TextPrimary),
        shape = CircleShape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("‹ 테마 선택으로", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DesignPrimaryAction(text: String, enabled: Boolean = true, accent: Color = Primary, action: () -> Unit) {
    Button(action, Modifier.fillMaxWidth().height(52.dp), enabled = enabled, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DesignSecondaryAction(text: String, action: () -> Unit) {
    OutlinedButton(
        action,
        Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Elevated, contentColor = TextPrimary),
        border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)
    ) {
        Text(text)
    }
}

@Composable
fun DesignGhostAction(text: String, color: Color = TextSecondary, action: () -> Unit) {
    TextButton(onClick = action) { Text(text, color = color, fontWeight = FontWeight.Medium) }
}

@Composable
fun DesignConfirmDialog(title: String, text: String, cancel: () -> Unit, confirm: () -> Unit, confirmLabel: String = "확인", confirmColor: Color = Error) {
    AlertDialog(
        onDismissRequest = cancel,
        confirmButton = { TextButton(confirm) { Text(confirmLabel, color = confirmColor) } },
        dismissButton = { TextButton(cancel) { Text("취소") } },
        title = { Text(title) },
        text = { Text(text) },
        containerColor = Surface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}

@Composable
fun DesignInfoDialog(title: String, text: String, close: () -> Unit) {
    AlertDialog(
        onDismissRequest = close,
        confirmButton = { TextButton(close) { Text("닫기") } },
        title = { Text(title) },
        text = { Text(text) },
        containerColor = Surface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}
