package com.dhan.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.Poppins
import kotlin.math.abs

/** Ports components.jsx `TxnRow`. amountRupees > 0 renders as incoming (green, "+"). */
@Composable
fun TxnRow(
    merchant: String,
    meta: String,
    amountRupees: Double,
    category: TxnCategory,
    modifier: Modifier = Modifier,
    last: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val isIn = amountRupees > 0
    val borderColor = DhanColor.borderSubtle
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .drawBehind {
                if (!last) {
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryIcon(category)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                merchant,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = DhanColor.fg1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                meta,
                fontFamily = Poppins,
                fontSize = 12.sp,
                color = DhanColor.fg3,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = (if (isIn) "+" else "−") + formatINR(abs(amountRupees)),
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = if (isIn) DhanColor.income else DhanColor.fg1,
        )
    }
}
