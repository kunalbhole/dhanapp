package com.dhan.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ports components.jsx `MiniSpark` — tiny sparkline. */
@Composable
fun MiniSpark(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    width: Dp = 120.dp,
    height: Dp = 30.dp,
) {
    if (data.size < 2) return
    val max = data.max()
    val min = data.min()
    val range = if (max - min == 0f) 1f else max - min
    Canvas(modifier = modifier.size(width, height)) {
        val w = width.toPx()
        val h = height.toPx()
        val points = data.mapIndexed { i, v ->
            val x = (i / (data.size - 1).toFloat()) * w
            val y = h - ((v - min) / range) * h
            Offset(x, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = color.copy(alpha = 0.85f),
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}
