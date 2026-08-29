package com.dhan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.DhanLayout
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins

enum class DhanTab(val route: String, val iconSlug: String, val label: String) {
    HOME("home", "house", "Home"),
    TXN("txn", "list-bullets", "Txns"),
    BUDGET("budget", "chart-pie-slice", "Budget"),
    BILLS("bills", "receipt", "Bills"),
    MORE("more", "list", "More"),
}

/** Ports components.jsx `TabBar` — 5-tab bottom nav. */
@Composable
fun DhanTabBar(active: DhanTab, onChange: (DhanTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DhanLayout.tabBarHeight)
            .background(Color.White)
            .border(width = 1.dp, color = DhanColor.borderSubtle)
            .padding(top = 8.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        DhanTab.entries.forEach { tab ->
            val isActive = tab == active
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clickable { onChange(tab) }
                    .padding(horizontal = 4.dp),
            ) {
                Icon(
                    imageVector = DhanIcon.of(tab.iconSlug, filled = isActive),
                    contentDescription = tab.label,
                    tint = if (isActive) DhanColor.navy else DhanColor.fg3,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    tab.label,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = if (isActive) DhanColor.navy else DhanColor.fg3,
                )
            }
        }
    }
}

/** Ports components.jsx `ScreenHeader` — back button + centered title + trailing slot. */
@Composable
fun DhanScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    right: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White, RoundedCornerShape(DhanRadius.control))
                .border(1.dp, DhanColor.borderSubtle, RoundedCornerShape(DhanRadius.control))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DhanIcon.of("arrow-left"), contentDescription = "Back", tint = DhanColor.fg1, modifier = Modifier.size(20.dp))
        }
        Text(title, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DhanColor.fg1)
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            right?.invoke()
        }
    }
}

/** Ports components.jsx `SectionHead` — section title with optional "See all" action. */
@Composable
fun SectionHead(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DhanColor.fg1)
        if (action != null) {
            Text(
                action,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = DhanColor.navy,
                modifier = Modifier.clickable { onAction?.invoke() },
            )
        }
    }
}
