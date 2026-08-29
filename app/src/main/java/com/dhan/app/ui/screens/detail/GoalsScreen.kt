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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.db.GoalEntity
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanBottomSheet
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.components.DhanField
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.components.formatINR
import com.dhan.app.ui.screens.main.avatarColorFor
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins
import com.dhan.app.util.DateUtils
import kotlinx.coroutines.launch

/** Ported from screens-extra-detail.jsx's GoalsScreen (#20). The JSX's four goals (ETA,
 *  monthly contribution, per-goal icon/color) are hardcoded sample data; here goals come
 *  from the real [GoalEntity] table via [repo].goals. [GoalEntity] has no icon or monthly-
 *  contribution field, so each goal gets a name-derived accent color (same trick as
 *  [avatarColorFor] elsewhere) and no contribution/ETA line unless a target date was set. */
@Composable
fun GoalsScreen(nav: DhanNavActions) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val goals by repo.goals.observeAll().collectAsState(initial = emptyList())
    var addOpen by remember { mutableStateOf(false) }
    var contributeGoal by remember { mutableStateOf<GoalEntity?>(null) }

    val totalSaved = goals.sumOf { it.savedRupees }
    val totalTarget = goals.sumOf { it.targetRupees }.coerceAtLeast(0.0)

    Scaffold(containerColor = DhanColor.appBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DhanScreenHeader(
                title = "Savings goals",
                onBack = { nav.back() },
                right = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(DhanColor.navy, RoundedCornerShape(DhanRadius.control))
                            .clickable { addOpen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(DhanIcon.of("plus"), contentDescription = "New goal", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                },
            )

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f),
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DhanColor.navy, RoundedCornerShape(DhanRadius.cardLg))
                            .padding(20.dp)
                            .padding(bottom = 4.dp),
                    ) {
                        Text(
                            "SAVED ACROSS ALL GOALS", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Text(
                            formatINR(totalSaved), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 32.sp,
                            color = Color.White, modifier = Modifier.padding(top = 4.dp),
                        )
                        val pct = if (totalTarget > 0) (totalSaved / totalTarget * 100) else 0.0
                        Text(
                            if (goals.isEmpty()) "No goals yet" else "of ${formatINR(totalTarget)} target · ${pct.toInt()}% there",
                            fontFamily = Poppins, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp),
                        )
                        Box(Modifier.height(14.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))) {
                            Box(modifier = Modifier.fillMaxWidth(pct.toFloat().coerceIn(0f, 1f) / 100f).height(6.dp).background(DhanColor.gold, RoundedCornerShape(50)))
                        }
                        Box(Modifier.height(6.dp))
                    }
                    Box(Modifier.height(16.dp))
                    Text(
                        "ACTIVE · ${goals.size}", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                }

                if (goals.isEmpty()) {
                    item {
                        Text(
                            "No savings goals yet. Tap + to start one.",
                            fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg3,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }

                items(goals, key = { it.id }) { g ->
                    val pct = if (g.targetRupees > 0) (g.savedRupees / g.targetRupees * 100) else 0.0
                    val color = avatarColorFor(g.name)
                    DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { contributeGoal = g }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(44.dp).background(color.copy(alpha = 0.13f), RoundedCornerShape(DhanRadius.control)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(DhanIcon.of("wallet"), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(g.name, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    g.targetDateMillis?.let { "By ${DateUtils.dateLabel(it)}" } ?: "No target date",
                                    fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg3, modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Text("${pct.toInt()}%", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Box(Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(DhanColor.bgSurface, RoundedCornerShape(50))) {
                            Box(modifier = Modifier.fillMaxWidth(pct.toFloat().coerceIn(0f, 1f) / 100f).height(8.dp).background(color, RoundedCornerShape(50)))
                        }
                        Box(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatINR(g.savedRupees), fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = DhanColor.fg2)
                            Text("of ${formatINR(g.targetRupees)}", fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg3)
                        }
                    }
                }
                item { Box(Modifier.height(8.dp)) }
            }
        }
    }

    AddGoalSheet(open = addOpen, onClose = { addOpen = false })
    ContributeSheet(goal = contributeGoal, onClose = { contributeGoal = null })
}

@Composable
private fun AddGoalSheet(open: Boolean, onClose: () -> Unit) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    var name by remember(open) { mutableStateOf("") }
    var targetText by remember(open) { mutableStateOf("") }

    DhanBottomSheet(open = open, onClose = onClose, title = "New savings goal") {
        DhanField(value = name, onValueChange = { name = it }, label = "Goal name", placeholder = "e.g. Emergency fund")
        DhanField(
            value = targetText,
            onValueChange = { new -> targetText = new.filter { it.isDigit() || it == '.' } },
            label = "Target amount",
            placeholder = "0",
            prefix = "₹",
            keyboardType = KeyboardType.Decimal,
        )
        DhanButton(
            text = "Create goal",
            full = true,
            onClick = {
                val target = targetText.toDoubleOrNull() ?: return@DhanButton
                if (name.isBlank()) return@DhanButton
                scope.launch {
                    repo.addGoal(GoalEntity(name = name, targetRupees = target, savedRupees = 0.0))
                    onClose()
                }
            },
        )
    }
}

@Composable
private fun ContributeSheet(goal: GoalEntity?, onClose: () -> Unit) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    var amountText by remember(goal?.id) { mutableStateOf("") }

    DhanBottomSheet(open = goal != null, onClose = onClose, title = goal?.let { "Add to ${it.name}" } ?: "Add") {
        DhanField(
            value = amountText,
            onValueChange = { new -> amountText = new.filter { it.isDigit() || it == '.' } },
            label = "Amount to add",
            placeholder = "0",
            prefix = "₹",
            keyboardType = KeyboardType.Decimal,
        )
        DhanButton(
            text = "Add to goal",
            full = true,
            onClick = {
                val g = goal ?: return@DhanButton
                val amount = amountText.toDoubleOrNull() ?: return@DhanButton
                scope.launch {
                    repo.goals.update(g.copy(savedRupees = g.savedRupees + amount))
                    onClose()
                }
            },
        )
    }
}
