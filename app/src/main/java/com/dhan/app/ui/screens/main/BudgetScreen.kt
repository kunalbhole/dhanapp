package com.dhan.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Scaffold
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.CategoryIcon
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanRing
import com.dhan.app.ui.components.DhanTab
import com.dhan.app.ui.components.DhanTabBar
import com.dhan.app.ui.components.StatusPill
import com.dhan.app.ui.components.StatusTone
import com.dhan.app.ui.components.TxnCategory
import com.dhan.app.ui.components.formatINR
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.Poppins
import com.dhan.app.util.DateUtils
import java.util.Calendar

@Composable
fun BudgetScreen(nav: DhanNavActions) {
    val repo = LocalRepository.current
    var monthOffset by remember { mutableIntStateOf(0) }

    val atMillis = remember(monthOffset) {
        Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) }.timeInMillis
    }
    val (monthStart, monthEnd) = remember(monthOffset) { DateUtils.monthRange(atMillis) }
    val monthKey = remember(monthOffset) { DateUtils.monthKey(atMillis) }

    val budgets by repo.budgets.observeForMonth(monthKey).collectAsState(initial = emptyList())
    val spend by repo.transactions.observeCategorySpendInRange(monthStart, monthEnd).collectAsState(initial = emptyList())
    val spendByCategory = remember(spend) { spend.associate { it.category to it.spent } }

    val totalCap = budgets.sumOf { it.limitRupees }
    val totalSpent = budgets.sumOf { spendByCategory[it.category] ?: 0.0 }
    val overallPct = if (totalCap > 0) (totalSpent / totalCap * 100) else 0.0

    Scaffold(
        bottomBar = { DhanTabBar(active = DhanTab.BUDGET, onChange = { nav.toTab(it) }) },
        containerColor = DhanColor.appBg,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Budget", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = DhanColor.fg1)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(12.dp))
                            .clickable { nav.toEditBudget() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(DhanIcon.of("pencil"), contentDescription = "Edit budget", tint = DhanColor.fg1, modifier = Modifier.size(18.dp))
                    }
                }
                Box(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clickable { monthOffset-- },
                        contentAlignment = Alignment.Center,
                    ) { Icon(DhanIcon.of("arrow-left"), contentDescription = "Previous month", tint = DhanColor.fg2, modifier = Modifier.size(16.dp)) }
                    Text(DateUtils.monthLabel(atMillis) + " " + monthKey.take(4), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .let { if (monthOffset < 0) it.clickable { monthOffset++ } else it },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            DhanIcon.of("arrow-right"), contentDescription = "Next month",
                            tint = if (monthOffset < 0) DhanColor.fg2 else DhanColor.fg4,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f),
            ) {
                item {
                    DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                        if (budgets.isEmpty()) {
                            Column {
                                Text("No budget set for this month", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DhanColor.fg1)
                                Text(
                                    "Tap the pencil to set monthly caps per category.",
                                    fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                DhanRing(
                                    value = totalSpent.toFloat(), max = totalCap.toFloat().coerceAtLeast(1f), size = 120.dp,
                                    color = if (overallPct > 100) DhanColor.expense else DhanColor.navy,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${overallPct.toInt()}%", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                        Text("of budget", fontFamily = Poppins, fontSize = 10.sp, color = DhanColor.fg3)
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("SPENT", fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DhanColor.fg3)
                                    Text(formatINR(totalSpent), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                    Text("of ${formatINR(totalCap)} total", fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3, modifier = Modifier.padding(top = 2.dp))
                                    if (monthOffset == 0) {
                                        Text(
                                            "${formatINR((totalCap - totalSpent).coerceAtLeast(0.0))} left · ${DateUtils.daysRemainingInMonth()} days to go",
                                            fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg2,
                                            modifier = Modifier.padding(top = 10.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (budgets.isNotEmpty()) {
                    item {
                        Text(
                            "BY CATEGORY", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                    }
                    items(budgets) { b ->
                        val cat = TxnCategory.fromKey(b.category)
                        val spent = spendByCategory[b.category] ?: 0.0
                        val pct = if (b.limitRupees > 0) (spent / b.limitRupees * 100) else 0.0
                        val over = spent > b.limitRupees
                        val warning = pct >= 80 && !over
                        val tone = if (over) StatusTone.EXPENSE else if (warning) StatusTone.WARNING else StatusTone.INCOME
                        val label = if (over) "Overspent" else if (warning) "Warning" else "On track"
                        DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        CategoryIcon(cat, size = 32.dp, tint = true)
                                        Column {
                                            Text(cat.displayName, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                "${formatINR(spent)} of ${formatINR(b.limitRupees)}",
                                                fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg3,
                                            )
                                        }
                                    }
                                    StatusPill(text = label, tone = tone)
                                }
                                Box(Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(6.dp).background(DhanColor.bgSurface, RoundedCornerShape(50)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((pct / 100).toFloat().coerceIn(0f, 1f))
                                            .height(6.dp)
                                            .background(if (over) DhanColor.expense else if (warning) DhanColor.warning else cat.color, RoundedCornerShape(50)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
