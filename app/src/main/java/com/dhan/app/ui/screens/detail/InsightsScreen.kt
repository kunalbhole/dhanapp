package com.dhan.app.ui.screens.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.components.TxnCategory
import com.dhan.app.ui.components.formatINR
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins
import com.dhan.app.util.DateUtils
import java.util.Calendar
import kotlin.math.abs

private enum class InsightPeriod(val label: String) { WEEK("Week"), MONTH("Month"), YEAR("Year") }

private fun rangeFor(period: InsightPeriod, atMillis: Long = System.currentTimeMillis()): Pair<Long, Long> = when (period) {
    InsightPeriod.WEEK -> {
        val end = Calendar.getInstance().apply {
            timeInMillis = atMillis
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val start = Calendar.getInstance().apply { timeInMillis = end; add(Calendar.DAY_OF_MONTH, -7) }.timeInMillis
        start to end
    }
    InsightPeriod.MONTH -> DateUtils.monthRange(atMillis)
    InsightPeriod.YEAR -> {
        val start = Calendar.getInstance().apply {
            timeInMillis = atMillis
            set(Calendar.MONTH, 0); set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply { timeInMillis = start; add(Calendar.YEAR, 1) }.timeInMillis
        start to end
    }
}

private fun previousRangeFor(period: InsightPeriod): Pair<Long, Long> {
    val (start, _) = rangeFor(period)
    val justBefore = start - 1
    return rangeFor(period, justBefore)
}

/** Ported from screens-extra-detail.jsx's InsightsScreen (#19). The JSX's category spend,
 *  hero total, and week-over-week trend are all hardcoded sample numbers; here they're
 *  real aggregates from [repo].transactions for the selected period, plus a real
 *  vs-previous-period comparison (no fabricated sparkline model, since there's no daily
 *  aggregation this app can vouch for beyond simple sums). The "Smart insights" section
 *  stays Plus-gated like the JSX, but since there's no AI/anomaly-detection backend, Plus
 *  users see a plainly labeled "coming soon" card instead of invented insight text. */
@Composable
fun InsightsScreen(nav: DhanNavActions) {
    val repo = LocalRepository.current
    val prefs = LocalUserPrefs.current
    val isPlus = prefs.isPlus
    var period by remember { mutableStateOf(InsightPeriod.MONTH) }

    val (start, end) = remember(period) { rangeFor(period) }
    val (prevStart, prevEnd) = remember(period) { previousRangeFor(period) }
    val totalSpent by repo.transactions.observeExpenseInRange(start, end).collectAsState(initial = 0.0)
    val prevSpent by repo.transactions.observeExpenseInRange(prevStart, prevEnd).collectAsState(initial = 0.0)
    val catSpend by repo.transactions.observeCategorySpendInRange(start, end).collectAsState(initial = emptyList())
    val sortedCats = remember(catSpend) { catSpend.filter { it.spent > 0 }.sortedByDescending { it.spent } }
    val maxCat = sortedCats.maxOfOrNull { it.spent } ?: 1.0
    val totalCatSpend = sortedCats.sumOf { it.spent }.let { if (it > 0) it else 1.0 }

    val pctChange = if (prevSpent > 0) ((totalSpent - prevSpent) / prevSpent * 100) else null

    Scaffold(containerColor = DhanColor.appBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DhanScreenHeader(title = "Insights", onBack = { nav.back() })
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            ) {
                // Period switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DhanColor.bgSurface, RoundedCornerShape(DhanRadius.control))
                        .padding(4.dp),
                ) {
                    InsightPeriod.entries.forEach { p ->
                        val active = p == period
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable { period = p }
                                .background(if (active) Color.White else Color.Transparent, RoundedCornerShape(DhanRadius.control)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                p.label, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                color = if (active) DhanColor.navy else DhanColor.fg3,
                            )
                        }
                    }
                }

                Box(Modifier.height(14.dp))

                // Hero stat
                DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text(
                        "TOTAL SPENT · ${period.label.uppercase()}", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        color = DhanColor.fg3,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text(formatINR(totalSpent), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                        if (pctChange != null) {
                            Box(Modifier.width(10.dp))
                            val down = pctChange <= 0
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(
                                    DhanIcon.of(if (down) "trend-down" else "trend-up"), contentDescription = null,
                                    tint = if (down) DhanColor.income else DhanColor.expense, modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    "${abs(pctChange).toInt()}% vs last ${period.label.lowercase()}",
                                    fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                    color = if (down) DhanColor.income else DhanColor.expense,
                                )
                            }
                        }
                    }
                    if (pctChange == null) {
                        Text(
                            "No spend last ${period.label.lowercase()} to compare against",
                            fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg3, modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                // Where it goes
                Text(
                    "WHERE IT GOES", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    if (sortedCats.isEmpty()) {
                        Text(
                            "No spending logged for this period yet.",
                            fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg3,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        Column {
                            sortedCats.forEach { row ->
                                val cat = TxnCategory.fromKey(row.category)
                                val w = (row.spent / maxCat).toFloat().coerceIn(0f, 1f)
                                val pct = (row.spent / totalCatSpend * 100).toInt()
                                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.height(10.dp).width(10.dp).background(cat.color, RoundedCornerShape(3.dp)))
                                            Text(cat.displayName, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        }
                                        Text(
                                            "${formatINR(row.spent)} · $pct%",
                                            fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg2,
                                        )
                                    }
                                    Box(Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(DhanColor.bgSurface, RoundedCornerShape(50))) {
                                        Box(modifier = Modifier.fillMaxWidth(w).height(8.dp).background(cat.color, RoundedCornerShape(50)))
                                    }
                                }
                            }
                        }
                    }
                }

                // Smart insights — Plus gated
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("SMART INSIGHTS", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3)
                    if (!isPlus) {
                        Row(
                            modifier = Modifier.background(DhanColor.goldBg, RoundedCornerShape(DhanRadius.pill)).padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(DhanIcon.of("star"), contentDescription = null, tint = Color(0xFF8E7420), modifier = Modifier.size(9.dp))
                            Text("PLUS", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color(0xFF8E7420))
                        }
                    }
                }

                if (isPlus) {
                    DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Icon(DhanIcon.of("sparkle"), contentDescription = null, tint = DhanColor.gold, modifier = Modifier.size(26.dp))
                            Text(
                                "AI-powered insights are coming soon", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            Text(
                                "Anomaly detection and savings tips aren't built yet — this space is reserved for them.",
                                fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3, textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                } else {
                    Box {
                        DhanCard(modifier = Modifier.fillMaxWidth().blur(4.dp).padding(bottom = 16.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().height(120.dp))
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.White, RoundedCornerShape(DhanRadius.card))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(DhanIcon.of("sparkle"), contentDescription = null, tint = DhanColor.gold, modifier = Modifier.size(28.dp))
                            Text("Unlock smart insights", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                            Text(
                                "AI-powered nudges, anomaly detection, savings tips.",
                                fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg2, textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                            )
                            DhanButton(text = "Upgrade to Plus", onClick = { nav.toPaywall() })
                        }
                    }
                    Box(Modifier.height(8.dp))
                }
                Box(Modifier.height(8.dp))
            }
        }
    }
}
