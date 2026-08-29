package com.dhan.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhan.app.ui.theme.DhanColor
import kotlin.math.min

/** Ports components.jsx `Ring` — circular progress indicator. */
@Composable
fun DhanRing(
    value: Float,
    max: Float,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    stroke: Dp = 12.dp,
    color: Color = DhanColor.navy,
    track: Color = DhanColor.bgSurface,
    content: @Composable () -> Unit = {},
) {
    val pct by animateFloatAsState(
        targetValue = if (max <= 0f) 0f else min(1f, value / max),
        animationSpec = tween(600),
        label = "ring-progress",
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val diameter = min(this.size.width, this.size.height) - strokePx
            val topLeft = androidx.compose.ui.geometry.Offset(
                (this.size.width - diameter) / 2f,
                (this.size.height - diameter) / 2f,
            )
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * pct,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
        content()
    }
}
