package com.dhan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon

enum class TxnCategory(val displayName: String, val iconSlug: String, val color: Color) {
    FOOD("Food", "fork-knife", DhanColor.catFood),
    TRANSPORT("Transport", "car-simple", DhanColor.catTransport),
    SHOPPING("Shopping", "shopping-bag", DhanColor.catShopping),
    BILLS("Bills", "receipt", DhanColor.catBills),
    ENTERTAINMENT("Entertainment", "film-strip", DhanColor.catEnt),
    HEALTH("Health", "heartbeat", DhanColor.catHealth),
    EDUCATION("Education", "graduation-cap", DhanColor.catEducation),
    GROCERIES("Groceries", "basket", DhanColor.catGroceries),
    RENT("Rent", "house", DhanColor.catRent),
    TRAVEL("Travel", "airplane-takeoff", DhanColor.catTravel),
    INCOME("Income", "arrow-down-left", DhanColor.income),
    OTHER("Other", "dots-three", DhanColor.catOther);

    companion object {
        fun fromKey(key: String?): TxnCategory =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: OTHER
    }
}

@Composable
fun CategoryIcon(
    category: TxnCategory,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Boolean = false,
) {
    val cornerPct = 0.30f
    if (tint) {
        Box(
            modifier = modifier
                .size(size)
                .background(category.color.copy(alpha = 0.13f), RoundedCornerShape(percent = (cornerPct * 100).toInt())),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = DhanIcon.of(category.iconSlug),
                contentDescription = category.displayName,
                tint = category.color,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(category.color, RoundedCornerShape(percent = (cornerPct * 100).toInt())),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = DhanIcon.of(category.iconSlug),
                contentDescription = category.displayName,
                tint = Color.White,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}
