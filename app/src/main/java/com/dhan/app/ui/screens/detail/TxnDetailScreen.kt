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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.db.TransactionEntity
import com.dhan.app.data.db.TxnSource
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
import kotlin.math.abs
import kotlinx.coroutines.launch

/** Ported from screens-extra-detail.jsx's TxnDetailScreen (#16). The JSX fills every row
 *  with fake account/method/reference strings since it's a static mock — here we load the
 *  real [TransactionEntity] by [id] and surface only fields the entity actually has, plus
 *  the auto-capture provenance ([TransactionEntity.source]/[sourceApp]/[rawText]) that is
 *  this app's whole reason for existing, which the JSX never modeled at all. */
@Composable
fun TxnDetailScreen(nav: DhanNavActions, id: Long) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    var txn by remember(id) { mutableStateOf<TransactionEntity?>(null) }
    var loaded by remember(id) { mutableStateOf(false) }
    var confirmDeleteOpen by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        txn = repo.transactions.getById(id)
        loaded = true
    }

    Scaffold(containerColor = DhanColor.appBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DhanScreenHeader(
                title = "Transaction",
                onBack = { nav.back() },
                right = {
                    if (txn != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(DhanRadius.control))
                                .clickable { confirmDeleteOpen = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                DhanIcon.of("trash"),
                                contentDescription = "Delete transaction",
                                tint = DhanColor.expense,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
            )

            val t = txn
            if (t == null) {
                if (loaded) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Transaction not found", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DhanColor.fg1)
                    }
                }
                return@Column
            }

            val cat = TxnCategory.fromKey(t.category)
            val isIncome = t.amountRupees > 0
            val monthKey = remember(t.id) { DateUtils.monthKey(t.timestampMillis) }
            val (mStart, mEnd) = remember(t.id) { DateUtils.monthRange(t.timestampMillis) }
            val budgets by repo.budgets.observeForMonth(monthKey).collectAsState(initial = emptyList())
            val spendRows by repo.transactions.observeCategorySpendInRange(mStart, mEnd).collectAsState(initial = emptyList())
            val budgetForCat = budgets.firstOrNull { it.category.equals(t.category, ignoreCase = true) }
            val spentForCat = spendRows.firstOrNull { it.category.equals(t.category, ignoreCase = true) }?.spent ?: 0.0

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                // Hero
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CategoryIcon(cat, size = 64.dp, tint = true)
                    Box(Modifier.height(14.dp))
                    Text(t.merchant, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DhanColor.fg1)
                    Text(
                        DateUtils.dayGroupLabel(t.timestampMillis),
                        fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        (if (isIncome) "+" else "−") + formatINR(abs(t.amountRupees)),
                        fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 38.sp,
                        color = if (isIncome) DhanColor.income else DhanColor.fg1,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }

                // Detail rows
                DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), padding = 4.dp) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                        DetailRow("Category") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CategoryIcon(cat, size = 22.dp, tint = true)
                                Text(cat.displayName, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                        DetailRow("Date") { Text(DateUtils.dateLabel(t.timestampMillis), fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        DetailRow("Time") { Text(DateUtils.timeLabel(t.timestampMillis), fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        DetailRow("Logged via") {
                            val label = when (t.source) {
                                TxnSource.MANUAL -> "Manual entry"
                                TxnSource.SMS -> "SMS auto-capture"
                                TxnSource.NOTIFICATION -> "Notification auto-capture"
                            }
                            Text(label, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (t.sourceApp != null) {
                            DetailRow("Source app") { Text(t.sourceApp, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        }
                        if (t.accountHint != null) {
                            DetailRow("Account") { Text(t.accountHint, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        }
                        DetailRow("Status", last = true) { StatusPill(text = "Posted", tone = StatusTone.INCOME) }
                    }
                }

                // Note
                if (!t.note.isNullOrBlank()) {
                    SectionLabel("Note")
                    DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text(t.note, fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg2, lineHeight = 19.sp)
                    }
                }

                // Provenance — the point of the auto-capture engine: let the user verify
                // what Dhan actually read off the SMS/notification.
                if (t.source != TxnSource.MANUAL && !t.rawText.isNullOrBlank()) {
                    SectionLabel("Detected from ${t.sourceApp ?: if (t.source == TxnSource.SMS) "SMS" else "a notification"}")
                    DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text(
                            "“${t.rawText}”",
                            fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg2, lineHeight = 18.sp,
                        )
                    }
                }

                // Budget impact (real, from this month's budget for this category)
                if (budgetForCat != null && budgetForCat.limitRupees > 0) {
                    SectionLabel("Impact on ${cat.displayName} budget")
                    DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        val pct = (spentForCat / budgetForCat.limitRupees * 100).coerceIn(0.0, 999.0)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "${formatINR(spentForCat)} of ${formatINR(budgetForCat.limitRupees)}",
                                fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DhanColor.fg2,
                            )
                            Text("${pct.toInt()}%", fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DhanColor.fg3)
                        }
                        Box(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(DhanColor.bgSurface, RoundedCornerShape(50))) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((pct / 100).toFloat().coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .background(if (pct > 100) DhanColor.expense else cat.color, RoundedCornerShape(50)),
                            )
                        }
                    }
                }

                Box(Modifier.height(4.dp))
                DhanButton(
                    text = "Delete transaction",
                    full = true,
                    variant = DhanButtonVariant.DESTRUCTIVE,
                    icon = DhanIcon.of("trash"),
                    onClick = { confirmDeleteOpen = true },
                )
                Box(Modifier.height(24.dp))
            }
        }
    }

    val current = txn
    DhanBottomSheet(open = confirmDeleteOpen, onClose = { confirmDeleteOpen = false }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Box(
                modifier = Modifier.size(56.dp).background(DhanColor.expenseBg, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DhanIcon.of("trash"), contentDescription = null, tint = DhanColor.expense, modifier = Modifier.size(26.dp))
            }
            Text("Delete this transaction?", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
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
                val t = current ?: return@DhanButton
                scope.launch {
                    repo.transactions.delete(t)
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
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
    )
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
