package com.dhan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.Poppins

/** Ports components.jsx `Chip`. */
@Composable
fun DhanChip(text: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = if (active) Color.White else DhanColor.fg2,
        modifier = modifier
            .background(if (active) DhanColor.navy else DhanColor.bgSurface, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

enum class StatusTone { INFO, INCOME, EXPENSE, WARNING, NEUTRAL }

private fun toneColors(tone: StatusTone): Pair<Color, Color> = when (tone) {
    StatusTone.INFO -> DhanColor.infoBg to DhanColor.info
    StatusTone.INCOME -> DhanColor.incomeBg to DhanColor.income
    StatusTone.EXPENSE -> DhanColor.expenseBg to DhanColor.expense
    StatusTone.WARNING -> DhanColor.warningBg to DhanColor.warning
    StatusTone.NEUTRAL -> DhanColor.bgSurface to DhanColor.fg2
}

/** Ports components.jsx `StatusPill`. */
@Composable
fun StatusPill(text: String, tone: StatusTone = StatusTone.INFO, modifier: Modifier = Modifier) {
    val (bg, fg) = toneColors(tone)
    Text(
        text = text,
        fontFamily = Poppins,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        color = fg,
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}
