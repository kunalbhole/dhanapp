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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.db.BudgetEntity
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.CategoryIcon
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.components.TxnCategory
import com.dhan.app.ui.components.formatINR
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins
import com.dhan.app.util.DateUtils
import kotlinx.coroutines.launch

/** Ported from screens-extra-detail.jsx's EditBudgetScreen (#17). The JSX seeds sliders
 *  from hardcoded caps and a fake ₹82,500 income — here caps are seeded from the real
 *  current-month [BudgetEntity] rows and "income" is the real sum of this month's positive
 *  transactions, and Save upserts/deletes the real rows via [repo].budgets.
 *
 *  Deviation: [DhanScreenHeader]'s `right` slot is a fixed 40dp square (shared component,
 *  not ours to change), so "Save" is a checkmark icon button rather than the JSX's text
 *  pill — same affordance, square-icon convention already used elsewhere (e.g. BudgetScreen's
 *  pencil button). "Add custom category" from the JSX is omitted: [TxnCategory] is a fixed
 *  enum with no backing store for arbitrary user categories, so a working custom-category
 *  button isn't possible without inventing a schema change. */
@Composable
fun EditBudgetScreen(nav: DhanNavActions) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val monthKey = remember { DateUtils.monthKey() }
    val (monthStart, monthEnd) = remember { DateUtils.monthRange() }

    val budgets by repo.budgets.observeForMonth(monthKey).collectAsState(initial = emptyList())
    val income by repo.transactions.observeIncomeInRange(monthStart, monthEnd).collectAsState(initial = 0.0)

    var caps by remember { mutableStateOf<Map<TxnCategory, Double>?>(null) }
    var seeded by remember { mutableStateOf(false) }
    LaunchedEffect(budgets) {
        if (!seeded) {
            caps = budgets.associate { TxnCategory.fromKey(it.category) to it.limitRupees }
            seeded = true
        }
    }
    val capsMap = caps ?: emptyMap()
    val categories = remember { TxnCategory.entries.filter { it != TxnCategory.INCOME } }
    val total = capsMap.values.sum()

    fun setCap(cat: TxnCategory, value: Double) {
        caps = capsMap.toMutableMap().apply { this[cat] = value.coerceIn(0.0, 200000.0) }
    }

    Scaffold(containerColor = DhanColor.appBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DhanScreenHeader(
                title = "Edit budget",
                onBack = { nav.back() },
                right = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(DhanColor.navy, RoundedCornerShape(DhanRadius.control))
                            .clickable {
                                scope.launch {
                                    categories.forEach { cat ->
                                        val existing = budgets.firstOrNull { it.category.equals(cat.name, ignoreCase = true) }
                                        val v = capsMap[cat] ?: 0.0
                                        if (v > 0) {
                                            repo.budgets.upsert(
                                                BudgetEntity(
                                                    id = existing?.id ?: 0,
                                                    category = cat.name.lowercase(),
                                                    monthKey = monthKey,
                                                    limitRupees = v,
                                                ),
                                            )
                                        } else if (existing != null) {
                                            repo.budgets.delete(existing)
                                        }
                                    }
                                    nav.back()
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(DhanIcon.of("check"), contentDescription = "Save", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                },
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DhanColor.navy, RoundedCornerShape(DhanRadius.card))
                        .padding(14.dp),
                ) {
                    Text(
                        "TOTAL MONTHLY CAP", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(formatINR(total), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
                        Text(
                            "  of ${formatINR(income)} income",
                            fontFamily = Poppins, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                    Box(Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))) {
                        val pct = if (income > 0) (total / income).toFloat().coerceIn(0f, 1f) else 0f
                        Box(modifier = Modifier.fillMaxWidth(pct).height(6.dp).background(DhanColor.gold, RoundedCornerShape(50)))
                    }
                    Box(Modifier.height(6.dp))
                    Text(
                        when {
                            income <= 0 -> "No income logged this month yet"
                            total < income -> "${formatINR(income - total)} unallocated · keep saving"
                            else -> "Over by ${formatINR(total - income)}"
                        },
                        fontFamily = Poppins, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                modifier = Modifier.weight(1f),
            ) {
                item {
                    Text(
                        "CAPS BY CATEGORY", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                }
                items(categories) { cat ->
                    val v = capsMap[cat] ?: 0.0
                    DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CategoryIcon(cat, size = 36.dp, tint = true)
                                Column(Modifier.weight(1f)) {
                                    Text(cat.displayName, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        if (income > 0) "${(v / income * 100).toInt()}% of income" else "No income logged",
                                        fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg3,
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.background(DhanColor.bgSurface, RoundedCornerShape(DhanRadius.control)).padding(horizontal = 4.dp, vertical = 2.dp),
                                ) {
                                    StepButton("−") { setCap(cat, v - 500) }
                                    Text(
                                        formatINR(v), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                    )
                                    StepButton("+") { setCap(cat, v + 500) }
                                }
                            }
                            Slider(
                                value = v.toFloat(),
                                onValueChange = { setCap(cat, it.toDouble()) },
                                valueRange = 0f..20000f,
                                steps = 79,
                                colors = SliderDefaults.colors(thumbColor = cat.color, activeTrackColor = cat.color, inactiveTrackColor = DhanColor.bgSurface),
                            )
                        }
                    }
                }
                item { Box(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(Color.White, RoundedCornerShape(DhanRadius.input))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DhanColor.fg2)
    }
}
