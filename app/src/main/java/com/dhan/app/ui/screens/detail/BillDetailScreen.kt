package com.dhan.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.db.BillEntity
import com.dhan.app.data.db.BillStatus
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.CategoryIcon
import com.dhan.app.ui.components.DhanBottomSheet
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanButtonVariant
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.components.StatusPill
import com.dhan.app.ui.components.StatusTone
import com.dhan.app.ui.components.TxnCategory
import com.dhan.app.ui.components.formatINR
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins
import com.dhan.app.util.DateUtils
import kotlinx.coroutines.launch

/** Ported from screens-extra-detail.jsx's BillDetailScreen (#18). Loads the real
 *  [BillEntity] by [id]. The JSX's per-bill "last 6 payments" bar chart and fixed
 *  reminder toggles have no backing data anywhere in this schema (there's no payment
 *  history table, no reminder-schedule table) so they're omitted rather than faked —
 *  everything shown here is a real field on [BillEntity]. */
@Composable
fun BillDetailScreen(nav: DhanNavActions, id: Long) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    var bill by remember(id) { mutableStateOf<BillEntity?>(null) }
    var loaded by remember(id) { mutableStateOf(false) }
    var confirmDeleteOpen by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        bill = repo.bills.getById(id)
        loaded = true
    }

    Scaffold(containerColor = DhanColor.appBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DhanScreenHeader(title = "Bill", onBack = { nav.back() })

            val b = bill
            if (b == null) {
                if (loaded) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Bill not found", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DhanColor.fg1)
                    }
                }
                return@Column
            }

            val cat = TxnCategory.fromKey(b.category)
            val isPaid = b.status == BillStatus.PAID
            val dueInDays = ((b.dueDateMillis - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DhanColor.navy, RoundedCornerShape(DhanRadius.cardLg))
                        .padding(22.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(DhanRadius.cardSm)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(DhanIcon.of(cat.iconSlug), contentDescription = null, tint = DhanColor.goldSoft, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(b.name, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            Text(
                                if (b.repeatMonthly) "Monthly · repeats automatically" else "One-time",
                                fontFamily = Poppins, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                    Box(Modifier.height(14.dp))
                    Text(
                        "AMOUNT DUE", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Text(formatINR(b.amountRupees), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = Color.White, modifier = Modifier.padding(top = 4.dp))
                    Box(Modifier.height(12.dp))
                    val (chipBg, chipFg, chipText) = when {
                        isPaid -> Triple(Color(0x402E7D5B), Color(0xFF9CDFB6), "Paid")
                        dueInDays <= 5 -> Triple(Color(0x33D89838), Color(0xFFFFD977), "Due ${DateUtils.dateLabel(b.dueDateMillis)} · in ${dueInDays}d")
                        else -> Triple(Color.White.copy(alpha = 0.12f), Color.White, "Due ${DateUtils.dateLabel(b.dueDateMillis)} · in ${dueInDays}d")
                    }
                    Row(
                        modifier = Modifier.background(chipBg, RoundedCornerShape(DhanRadius.pill)).padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            DhanIcon.of(if (isPaid) "check-circle" else "calendar"), contentDescription = null,
                            tint = chipFg, modifier = Modifier.size(12.dp),
                        )
                        Text(chipText, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = chipFg)
                    }
                }

                if (!isPaid) {
                    Box(Modifier.height(14.dp))
                    DhanButton(
                        text = "Mark paid", full = true, icon = DhanIcon.of("check-circle"),
                        onClick = {
                            scope.launch {
                                repo.bills.setStatus(b.id, BillStatus.PAID)
                                nav.back()
                            }
                        },
                    )
                }

                Box(Modifier.height(14.dp))
                Text(
                    "DETAILS", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                DhanCard(modifier = Modifier.fillMaxWidth(), padding = 4.dp) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                        DetailRow("Category") { Text(cat.displayName, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        DetailRow("Due date") { Text(DateUtils.dateLabel(b.dueDateMillis), fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        DetailRow("Repeats") { Text(if (b.repeatMonthly) "Every month" else "One-time", fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        val (tone, label) = when (b.status) {
                            BillStatus.UPCOMING -> StatusTone.INFO to "Upcoming"
                            BillStatus.DUE_SOON -> StatusTone.WARNING to "Due soon"
                            BillStatus.OVERDUE -> StatusTone.EXPENSE to "Overdue"
                            BillStatus.PAID -> StatusTone.INCOME to "Paid"
                        }
                        DetailRow("Status", last = true) { StatusPill(text = label, tone = tone) }
                    }
                }

                Box(Modifier.height(16.dp))
                DhanButton(
                    text = "Delete bill", full = true, variant = DhanButtonVariant.DESTRUCTIVE,
                    icon = DhanIcon.of("trash"), onClick = { confirmDeleteOpen = true },
                )
                Box(Modifier.height(24.dp))
            }
        }
    }

    val current = bill
    DhanBottomSheet(open = confirmDeleteOpen, onClose = { confirmDeleteOpen = false }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Box(
                modifier = Modifier.size(56.dp).background(DhanColor.expenseBg, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DhanIcon.of("trash"), contentDescription = null, tint = DhanColor.expense, modifier = Modifier.size(26.dp))
            }
            Text("Delete this bill?", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            Text(
                "This can't be undone.",
                fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
        DhanButton(
            text = "Delete", full = true, variant = DhanButtonVariant.DESTRUCTIVE,
            onClick = {
                val b = current ?: return@DhanButton
                scope.launch {
                    repo.bills.delete(b)
                    confirmDeleteOpen = false
                    nav.back()
                }
            },
        )
        Box(Modifier.height(8.dp))
        DhanButton(text = "Cancel", full = true, variant = DhanButtonVariant.GHOST, onClick = { confirmDeleteOpen = false })
    }
}

@Composable
private fun DetailRow(label: String, last: Boolean = false, value: @Composable () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = DhanColor.fg3)
            value()
        }
        if (!last) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DhanColor.borderSubtle))
        }
    }
}
