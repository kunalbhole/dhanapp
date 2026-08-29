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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.db.BillStatus
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanButtonSize
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanTab
import com.dhan.app.ui.components.DhanTabBar
import com.dhan.app.ui.components.SectionHead
import com.dhan.app.ui.components.StatusPill
import com.dhan.app.ui.components.StatusTone
import com.dhan.app.ui.components.formatINR
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.Poppins
import com.dhan.app.util.DateUtils
import kotlinx.coroutines.launch

@Composable
fun BillsScreen(nav: DhanNavActions) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val bills by repo.bills.observeAll().collectAsState(initial = emptyList())
    var addBillOpen by remember { mutableStateOf(false) }

    val dueSoon = bills.filter { it.status == BillStatus.DUE_SOON }
    val unpaid = bills.filter { it.status != BillStatus.PAID }
    val totalUnpaid = unpaid.sumOf { it.amountRupees }
    val totalMonthly = bills.sumOf { it.amountRupees }
    val allSorted = bills.sortedBy { it.dueDateMillis }

    Scaffold(
        bottomBar = { DhanTabBar(active = DhanTab.BILLS, onChange = { nav.toTab(it) }) },
        containerColor = DhanColor.appBg,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Bills & subs", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = DhanColor.fg1)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(DhanColor.navy, RoundedCornerShape(12.dp))
                            .clickable { addBillOpen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(DhanIcon.of("plus"), contentDescription = "Add bill", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Box(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DhanColor.navy, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "STILL DUE THIS MONTH", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Text(formatINR(totalUnpaid), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Color.White, modifier = Modifier.padding(top = 2.dp))
                        Text(
                            "${bills.count { it.status == BillStatus.PAID }} of ${bills.size} paid · ${formatINR(totalMonthly)} total",
                            fontFamily = Poppins, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Box(
                        modifier = Modifier.size(52.dp).background(Color(0x33C9A84C), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(DhanIcon.of("receipt"), contentDescription = null, tint = DhanColor.goldSoft, modifier = Modifier.size(22.dp))
                    }
                }
            }

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (dueSoon.isNotEmpty()) {
                    item { SectionHead("Due soon") }
                    items(dueSoon, key = { "due-${it.id}" }) { b ->
                        val dueInDays = ((b.dueDateMillis - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                        DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, top = 8.dp)) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(DhanColor.warningBg, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(DhanIcon.of("receipt"), contentDescription = null, tint = DhanColor.warning, modifier = Modifier.size(20.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(b.name, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "Due ${DateUtils.dateLabel(b.dueDateMillis)} · in ${dueInDays}d",
                                            fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DhanColor.warning,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                    Text(formatINR(b.amountRupees), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Box(Modifier.height(10.dp))
                                DhanButton(
                                    text = "Mark as paid", full = true, size = DhanButtonSize.SM,
                                    icon = DhanIcon.of("check-circle"),
                                    onClick = { scope.launch { repo.bills.setStatus(b.id, BillStatus.PAID) } },
                                )
                            }
                        }
                    }
                }

                item { SectionHead("All bills", modifier = Modifier.padding(top = 12.dp)) }
                if (allSorted.isEmpty()) {
                    item {
                        Text(
                            "No bills yet. Tap + to add your first one.",
                            fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg3,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
                items(allSorted, key = { "all-${it.id}" }) { b ->
                    val (tone, label) = when (b.status) {
                        BillStatus.UPCOMING -> StatusTone.INFO to "Upcoming"
                        BillStatus.DUE_SOON -> StatusTone.WARNING to "Due soon"
                        BillStatus.OVERDUE -> StatusTone.EXPENSE to "Overdue"
                        BillStatus.PAID -> StatusTone.INCOME to "Paid"
                    }
                    DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), onClick = { nav.toBillDetail(b.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(40.dp).background(DhanColor.bgSurface, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(DhanIcon.of("receipt"), contentDescription = null, tint = DhanColor.navy, modifier = Modifier.size(20.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(b.name, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Due ${DateUtils.dateLabel(b.dueDateMillis)}", fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3, modifier = Modifier.padding(top = 2.dp))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatINR(b.amountRupees), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Box(Modifier.height(4.dp))
                                StatusPill(text = label, tone = tone)
                            }
                        }
                    }
                }
            }
        }
    }

    AddBillSheet(open = addBillOpen, onClose = { addBillOpen = false })
}
