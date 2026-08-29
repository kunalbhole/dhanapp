package com.dhan.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanRadius

/** Ports components.jsx `Card` — elevated white surface, 16dp radius, soft navy-tinted shadow. */
@Composable
fun DhanCard(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(DhanRadius.card)
    Surface(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = shape, ambientColor = DhanColor.navy.copy(alpha = 0.08f), spotColor = DhanColor.navy.copy(alpha = 0.08f))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = shape,
        color = DhanColor.bgElevated,
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}
