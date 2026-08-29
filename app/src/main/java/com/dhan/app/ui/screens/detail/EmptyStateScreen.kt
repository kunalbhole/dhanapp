package com.dhan.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanChip
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins

private data class EmptyState(val icon: String, val color: Color, val title: String, val body: String, val cta: String, val ctaIcon: String)

/** Ported from screens-extra-detail.jsx's EmptyStateScreen (#25) — a small reference
 *  gallery of the empty/error illustrations used elsewhere in the app (first-run home, no
 *  search results, offline, load error). Purely a design-system reference, same as the
 *  JSX; the CTA buttons here are inert since each represents a different real screen's
 *  action rather than something this gallery screen itself performs. */
@Composable
fun EmptyStateScreen(nav: DhanNavActions) {
    val states = remember {
        listOf(
            EmptyState(
                "receipt-x", DhanColor.navy, "No transactions yet",
                "Once your bank sends an SMS, we'll log it here automatically. Add one manually to get started.",
                "Add transaction", "plus",
            ),
            EmptyState(
                "magnifying-glass", DhanColor.fg3, "Nothing matches",
                "Try a different filter or clear your search.",
                "Clear filters", "plus",
            ),
            EmptyState(
                "wifi-slash", DhanColor.warning, "You're offline",
                "Some data may be out of date. We'll sync as soon as you're back online.",
                "Try again", "arrow-clockwise",
            ),
            EmptyState(
                "warning-octagon", DhanColor.expense, "Couldn't load this",
                "Something went wrong on our end. We've logged it and you can try again.",
                "Retry", "arrow-clockwise",
            ),
        )
    }
    var active by remember { mutableIntStateOf(0) }
    val s = states[active]

    Scaffold(containerColor = DhanColor.appBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DhanScreenHeader(title = "Empty & error states", onBack = { nav.back() })

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                states.forEachIndexed { i, st ->
                    DhanChip(text = st.title.split(" ").take(2).joinToString(" "), active = active == i, onClick = { active = i })
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(s.color.copy(alpha = 0.1f), RoundedCornerShape(DhanRadius.cardLg)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(DhanIcon.of(s.icon), contentDescription = null, tint = s.color, modifier = Modifier.size(44.dp))
                }
                Text(
                    s.title, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DhanColor.fg1,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                )
                Text(
                    s.body, fontFamily = Poppins, fontSize = 14.sp, color = DhanColor.fg2, lineHeight = 20.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 24.dp),
                )
                DhanButton(text = s.cta, icon = DhanIcon.of(s.ctaIcon), onClick = {})
            }
        }
    }
}
