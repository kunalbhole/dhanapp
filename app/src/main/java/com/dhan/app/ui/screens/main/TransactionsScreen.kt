package com.dhan.app.ui.screens.main

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.components.DhanChip
import com.dhan.app.ui.components.DhanFab
import com.dhan.app.ui.components.DhanField
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.components.DhanTab
import com.dhan.app.ui.components.DhanTabBar
import com.dhan.app.ui.components.TxnCategory
import com.dhan.app.ui.components.TxnRow
import com.dhan.app.ui.components.formatINR
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.Poppins
import com.dhan.app.util.DateUtils

private enum class TxnFilter(val label: String) { ALL("All"), INCOME("Income"), EXPENSE("Expenses"), FOOD("Food"), SHOPPING("Shopping"), BILLS("Bills") }

@Composable
fun TransactionsScreen(nav: DhanNavActions) {
    val repo = LocalRepository.current
    val allTxns by repo.transactions.observeAll().collectAsState(initial = emptyList())

    var filter by remember { mutableStateOf(TxnFilter.ALL) }
    var query by remember { mutableStateOf("") }
    var addTxnOpen by remember { mutableStateOf(false) }

    val filtered = allTxns.filter { t ->
        when (filter) {
            TxnFilter.ALL -> true
            TxnFilter.INCOME -> t.amountRupees > 0
            TxnFilter.EXPENSE -> t.amountRupees < 0
            TxnFilter.FOOD -> t.category == "food"
            TxnFilter.SHOPPING -> t.category == "shopping"
            TxnFilter.BILLS -> t.category == "bills"
        } && (query.isBlank() || t.merchant.contains(query, ignoreCase = true))
    }
    val income = filtered.filter { it.amountRupees > 0 }.sumOf { it.amountRupees }
    val expense = filtered.filter { it.amountRupees < 0 }.sumOf { -it.amountRupees }
    val net = income - expense
    val groups = filtered.groupBy { DateUtils.dayGroupLabel(it.timestampMillis) }

    Scaffold(
        bottomBar = { DhanTabBar(active = DhanTab.TXN, onChange = { nav.toTab(it) }) },
        floatingActionButton = { DhanFab(onClick = { addTxnOpen = true }) },
        containerColor = DhanColor.appBg,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Transactions", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = DhanColor.fg1)
                }
                Box(Modifier.height(12.dp))
                DhanField(value = query, onValueChange = { query = it }, placeholder = "Search merchants, notes…")
                Box(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TxnFilter.entries) { f ->
                        DhanChip(text = f.label, active = filter == f, onClick = { filter = f })
                    }
                }
                Box(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryTile("INCOME", income, DhanColor.income, DhanColor.incomeBg, Modifier.weight(1f))
                    SummaryTile("EXPENSE", -expense, DhanColor.expense, DhanColor.expenseBg, Modifier.weight(1f))
                    SummaryTile("NET", net, if (net >= 0) DhanColor.income else DhanColor.expense, DhanColor.bgSurface, Modifier.weight(1f))
                }
            }

            if (groups.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(DhanIcon.of("search"), contentDescription = null, tint = DhanColor.fg3, modifier = Modifier.size(40.dp))
                    Text("No transactions match.", fontFamily = Poppins, fontSize = 14.sp, color = DhanColor.fg3, modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    groups.forEach { (day, items) ->
                        item(key = "header-$day") {
                            Text(
                                day.uppercase(),
                                fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp),
                            )
                        }
                        item(key = "group-$day") {
                            DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp), padding = 8.dp) {
                                Column {
                                    items.forEachIndexed { i, t ->
                                        TxnRow(
                                            merchant = t.merchant,
                                            meta = (t.note ?: t.sourceApp ?: t.category.replaceFirstChar { it.uppercase() }) + " · " + DateUtils.timeLabel(t.timestampMillis),
                                            amountRupees = t.amountRupees,
                                            category = TxnCategory.fromKey(t.category),
                                            last = i == items.lastIndex,
                                            onClick = { nav.toTxnDetail(t.id) },
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

    AddTxnSheet(open = addTxnOpen, onClose = { addTxnOpen = false })
}

@Composable
private fun SummaryTile(label: String, value: Double, color: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DhanColor.fg2)
        Text(
            (if (value < 0) "−" else "") + formatINR(kotlin.math.abs(value)),
            fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
