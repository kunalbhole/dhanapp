package com.dhan.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Scaffold
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.dhan.app.data.db.BillStatus
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.CategoryIcon
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanTab
import com.dhan.app.ui.components.DhanTabBar
import com.dhan.app.ui.components.SectionHead
import com.dhan.app.ui.components.StatusPill
import com.dhan.app.ui.components.StatusTone
import com.dhan.app.ui.components.TxnCategory
import com.dhan.app.ui.components.TxnRow
import com.dhan.app.ui.components.formatINR
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.Poppins
import com.dhan.app.util.DateUtils
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.map

@Composable
fun HomeScreen(nav: DhanNavActions) {
    val repo = LocalRepository.current
    val prefs = LocalUserPrefs.current
    val (monthStart, monthEnd) = remember { DateUtils.monthRange() }

    val income by repo.transactions.observeIncomeInRange(monthStart, monthEnd)
        .collectAsState(initial = 0.0)
    val expense by repo.transactions.observeExpenseInRange(monthStart, monthEnd)
        .collectAsState(initial = 0.0)
    val recent by repo.transactions.observeAll().map { it.take(4) }.collectAsState(initial = emptyList())
    val categorySpend by repo.transactions.observeCategorySpendInRange(monthStart, monthEnd)
        .collectAsState(initial = emptyList())
    val budgets by repo.budgets.observeForMonth(DateUtils.monthKey())
        .collectAsState(initial = emptyList())
    val bills by repo.bills.observeAll().map { list -> list.filter { it.status != BillStatus.PAID }.sortedBy { it.dueDateMillis }.take(4) }
        .collectAsState(initial = emptyList())
    val goals by repo.goals.observeAll().collectAsState(initial = emptyList())

    var addTxnOpen by remember { mutableStateOf<Boolean?>(null) }

    val totalCap = budgets.sumOf { it.limitRupees }
    val totalSpent = categorySpend.sumOf { it.spent }
    val balance = income - expense

    Scaffold(
        bottomBar = { DhanTabBar(active = DhanTab.HOME, onChange = { nav.toTab(it) }) },
        containerColor = DhanColor.appBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Morning,", fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg3)
                    Text(
                        "${prefs.userName.ifBlank { "there" }} 👋",
                        fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DhanColor.fg1,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconTile(icon = "plus", onClick = { addTxnOpen = false })
                    Box {
                        IconTile(icon = "bell", onClick = { nav.toNotifications() })
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .background(DhanColor.gold, RoundedCornerShape(50)),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DhanColor.navy, RoundedCornerShape(20.dp))
                    .padding(20.dp),
            ) {
                Text(
                    "BALANCE · ${DateUtils.monthLabel().uppercase()}",
                    fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    formatINR(balance),
                    fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 34.sp,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    BalancePill(icon = "arrow-down-left", text = "+ ${formatINR(income)}", bg = Color(0x26FED977), fg = DhanColor.goldSoft)
                    BalancePill(icon = "arrow-up-right", text = "− ${formatINR(expense)}", bg = Color(0x1AFFFFFF), fg = Color.White)
                }
            }

            Box(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Add expense", "minus-circle", DhanColor.expense, Modifier.weight(1f)) { addTxnOpen = false }
                QuickAction("Add income", "plus-circle", DhanColor.income, Modifier.weight(1f)) { addTxnOpen = true }
                QuickAction("Pay bill", "receipt", DhanColor.navy, Modifier.weight(1f)) { nav.toTab(DhanTab.BILLS) }
                QuickAction("Insights", "trend-up", DhanColor.info, Modifier.weight(1f)) { nav.toInsights() }
            }

            Box(Modifier.height(20.dp))

            SectionHead("This month's budget", action = "See all", onAction = { nav.toTab(DhanTab.BUDGET) })
            Box(Modifier.height(10.dp))
            DhanCard(modifier = Modifier.fillMaxWidth()) {
                if (budgets.isEmpty()) {
                    Column {
                        Text("No budget set yet", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DhanColor.fg1)
                        Text(
                            "Set monthly caps per category to track how you're doing.",
                            fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3,
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                        )
                        com.dhan.app.ui.components.DhanButton(text = "Set up budget", onClick = { nav.toEditBudget() }, size = com.dhan.app.ui.components.DhanButtonSize.SM)
                    }
                } else {
                    val pct = if (totalCap > 0) (totalSpent / totalCap * 100).coerceIn(0.0, 999.0) else 0.0
                    Text(
                        formatINR(totalSpent) + " ",
                        fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = DhanColor.fg1,
                    )
                    Text("of ${formatINR(totalCap)}", fontFamily = Poppins, fontSize = 14.sp, color = DhanColor.fg3)
                    Box(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(DhanColor.bgSurface, RoundedCornerShape(50)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((pct / 100).toFloat().coerceIn(0f, 1f))
                                .height(8.dp)
                                .background(DhanColor.navy, RoundedCornerShape(50)),
                        )
                    }
                }
            }

            Box(Modifier.height(20.dp))

            SectionHead("Recent transactions", action = "See all", onAction = { nav.toTab(DhanTab.TXN) })
            Box(Modifier.height(10.dp))
            DhanCard(modifier = Modifier.fillMaxWidth(), padding = 8.dp) {
                if (recent.isEmpty()) {
                    EmptyRow("No transactions yet — add one or link SMS/notification capture in Settings.")
                } else {
                    Column {
                        recent.forEachIndexed { i, t ->
                            TxnRow(
                                merchant = t.merchant,
                                meta = DateUtils.dayGroupLabel(t.timestampMillis) + " · " + DateUtils.timeLabel(t.timestampMillis),
                                amountRupees = t.amountRupees,
                                category = TxnCategory.fromKey(t.category),
                                last = i == recent.lastIndex,
                                onClick = { nav.toTxnDetail(t.id) },
                            )
                        }
                    }
                }
            }

            Box(Modifier.height(20.dp))

            SectionHead("Savings goals", action = "See all", onAction = { nav.toGoals() })
            Box(Modifier.height(10.dp))
            val savedTotal = goals.sumOf { it.savedRupees }
            val targetTotal = goals.sumOf { it.targetRupees }
            DhanCard(modifier = Modifier.fillMaxWidth(), onClick = { nav.toGoals() }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(DhanColor.goldBg, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(DhanIcon.of("star"), contentDescription = null, tint = DhanColor.gold, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${goals.size} active goal${if (goals.size == 1) "" else "s"}",
                            fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DhanColor.fg1,
                        )
                        Text(
                            if (goals.isEmpty()) "Set a savings target to get started" else "${formatINR(savedTotal)} saved of ${formatINR(targetTotal)}",
                            fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Icon(DhanIcon.of("caret-right"), contentDescription = null, tint = DhanColor.fg3, modifier = Modifier.size(16.dp))
                }
            }

            Box(Modifier.height(20.dp))

            SectionHead("Upcoming bills", action = "See all", onAction = { nav.toTab(DhanTab.BILLS) })
            Box(Modifier.height(10.dp))
            if (bills.isEmpty()) {
                DhanCard(modifier = Modifier.fillMaxWidth()) { EmptyRow("No upcoming bills.") }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(bills) { b ->
                        val dueInDays = ((b.dueDateMillis - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                        DhanCard(modifier = Modifier.width(160.dp)) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(32.dp).background(DhanColor.bgSurface, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(DhanIcon.of("receipt"), contentDescription = null, tint = DhanColor.navy, modifier = Modifier.size(16.dp))
                                    }
                                    StatusPill(
                                        text = if (dueInDays <= 0) "Today" else "${dueInDays}d",
                                        tone = if (b.status == BillStatus.DUE_SOON) StatusTone.WARNING else StatusTone.INFO,
                                    )
                                }
                                Text(b.name, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                                Text(DateUtils.dateLabel(b.dueDateMillis), fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg3)
                                Text(formatINR(b.amountRupees), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
            Box(Modifier.height(24.dp))
        }
    }

    AddTxnSheet(open = addTxnOpen != null, onClose = { addTxnOpen = null }, defaultIsIncome = addTxnOpen == true)
}

@Composable
private fun IconTile(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(DhanIcon.of(icon), contentDescription = null, tint = DhanColor.navy, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun BalancePill(icon: String, text: String, bg: Color, fg: Color) {
    Row(
        modifier = Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(DhanIcon.of(icon), contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(text, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = fg)
    }
}

@Composable
private fun QuickAction(label: String, icon: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DhanIcon.of(icon), contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        }
        Text(
            label, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = DhanColor.fg2,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun EmptyRow(text: String) {
    Text(text, fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3, modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp))
}
