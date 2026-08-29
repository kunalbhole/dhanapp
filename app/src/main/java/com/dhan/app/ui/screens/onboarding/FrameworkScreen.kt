package com.dhan.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins

private data class FrameworkChip(val label: String, val value: Int, val color: Color)
private data class FrameworkOption(
    val id: String,
    val title: String,
    val sub: String,
    val body: String,
    val popular: Boolean = false,
    val chips: List<FrameworkChip> = emptyList(),
)

/** Ports screens-onboarding.jsx `FrameworkScreen` (04) — the 50/30/20-style budget
 *  framework picker that closes out onboarding. Completing it marks hasOnboarded and
 *  clears the auth back-stack onto Home. */
@Composable
fun FrameworkScreen(nav: DhanNavActions) {
    val prefs = LocalUserPrefs.current
    var selected by remember { mutableStateOf("50/30/20") }
    val options = remember {
        listOf(
            FrameworkOption(
                "50/30/20", "50/30/20", "Needs · Wants · Savings", "The classic. Good starting point.",
                popular = true,
                chips = listOf(
                    FrameworkChip("Needs", 50, DhanColor.navy),
                    FrameworkChip("Wants", 30, DhanColor.gold),
                    FrameworkChip("Savings", 20, DhanColor.income),
                ),
            ),
            FrameworkOption(
                "70/20/10", "70/20/10", "Spending · Savings · Debt", "If you're paying off loans or EMIs.",
                chips = listOf(
                    FrameworkChip("Spend", 70, DhanColor.navy),
                    FrameworkChip("Save", 20, DhanColor.income),
                    FrameworkChip("Debt", 10, DhanColor.expense),
                ),
            ),
            FrameworkOption(
                "60/20/20", "60/20/20", "Commit · Save · Play", "Balanced, flexible mid-ground.",
                chips = listOf(
                    FrameworkChip("Commit", 60, DhanColor.navy),
                    FrameworkChip("Save", 20, DhanColor.income),
                    FrameworkChip("Play", 20, DhanColor.gold),
                ),
            ),
            FrameworkOption(
                "zero", "Zero-based", "Every rupee has a job", "Assign income down to zero each month.",
                chips = listOf(FrameworkChip("Assigned", 100, DhanColor.navy)),
            ),
            FrameworkOption(
                "custom", "Custom", "Build your own mix", "Define your own categories and caps.",
            ),
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(DhanColor.bgSurface)) {
        DhanScreenHeader(title = "Budget framework", onBack = { nav.back() })
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)) {
            Text(
                "How do you want to budget?", fontFamily = Poppins, fontWeight = FontWeight.Bold,
                fontSize = 24.sp, color = DhanColor.fg1, modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                "Pick a framework. You can change it anytime.", fontFamily = Poppins,
                fontSize = 14.sp, color = DhanColor.fg2, modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 0.dp),
        ) {
            items(options) { option ->
                FrameworkCard(option = option, active = selected == option.id, onClick = { selected = option.id })
                Box(Modifier.height(10.dp))
            }
            item { Box(Modifier.height(6.dp)) }
        }
        Column(
            modifier = Modifier.fillMaxWidth().background(DhanColor.bgSurface)
                .border(width = 1.dp, color = DhanColor.borderSubtle)
                .padding(horizontal = 16.dp, vertical = 12.dp).padding(bottom = 12.dp),
        ) {
            DhanButton(
                text = "Let's go", full = true, size = com.dhan.app.ui.components.DhanButtonSize.LG,
                iconRight = DhanIcon.of("arrow-right"),
                onClick = {
                    prefs.hasOnboarded = true
                    nav.toHomeFresh()
                },
            )
        }
    }
}

@Composable
private fun FrameworkCard(option: FrameworkOption, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(DhanRadius.card))
            .border(
                width = if (active) 2.dp else 0.dp,
                color = if (active) DhanColor.navy else Color.Transparent,
                shape = RoundedCornerShape(DhanRadius.card),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        if (option.popular) {
            Text(
                "POPULAR", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                color = DhanColor.navy, letterSpacing = 0.6.sp,
                modifier = Modifier.align(Alignment.TopEnd)
                    .background(DhanColor.goldBg, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(22.dp)
                        .background(if (active) DhanColor.navy else Color.Transparent, CircleShape)
                        .border(2.dp, if (active) DhanColor.navy else DhanColor.borderStrong, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (active) {
                        Icon(DhanIcon.of("check"), contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
                Text(option.title, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DhanColor.fg1)
                Text(option.sub, fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3)
            }
            Text(
                option.body, fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg2,
                modifier = Modifier.padding(start = 34.dp, top = 6.dp, bottom = if (option.chips.isNotEmpty()) 12.dp else 0.dp),
            )
            if (option.chips.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 34.dp).height(8.dp)
                        .background(DhanColor.bgSurface, RoundedCornerShape(999.dp)),
                ) {
                    option.chips.forEach { chip ->
                        Box(
                            modifier = Modifier.weight(chip.value.toFloat()).fillMaxSize()
                                .background(chip.color),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 34.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    option.chips.forEach { chip ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(chip.color, RoundedCornerShape(2.dp)))
                            Text(
                                "${chip.value}% ${chip.label}", fontFamily = Poppins, fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp, color = DhanColor.fg2,
                            )
                        }
                    }
                }
            }
        }
    }
}
