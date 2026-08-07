package com.example.escapephone.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 문제별 힌트 보기 UI. 팝업(AlertDialog) 대신 현재 화면 안에 인라인으로 힌트를
 * 쌓아 보여주고, "N/최대" 형태로 몇 개를 이미 봤는지 항상 표시한다. 힌트 상태는
 * 이 컴포저블의 remember 상태이므로, 뒤로가기나 홈 이동으로 화면이 컴포지션에서
 * 빠졌다가 다시 생성되면 자동으로 0으로 리셋된다 — 별도 초기화 로직이 필요 없다.
 *
 * 본문 콘텐츠(Card/Surface 배경)와 겹치지 않도록 톤 다운된 배경, 점선 테두리,
 * 전구 이모지로 "이건 부가 정보"라는 인상을 명확히 준다.
 */
@Composable
fun HintSection(hints: List<String>, onReveal: (level: Int) -> Unit) {
    var level by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            onClick = {
                level = (level + 1).coerceAtMost(hints.size)
                onReveal(level)
            },
            enabled = level < hints.size,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("💡", fontSize = 13.sp)
                Text(if (level >= hints.size) "힌트 모두 확인함 ($level/${hints.size})" else "힌트 보기 ($level/${hints.size})", color = TextSecondary, fontSize = 13.sp)
            }
        }
        hints.take(level).forEachIndexed { index, hint ->
            val dashColor = TextSecondary.copy(alpha = 0.35f)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .drawBehind {
                        val stroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                        drawRoundRect(color = dashColor, style = stroke, cornerRadius = CornerRadius(12.dp.toPx()))
                    }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔆", fontSize = 14.sp)
                Text("힌트 ${index + 1}. $hint", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}
