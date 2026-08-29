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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.db.FriendEntity
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.components.DhanTab
import com.dhan.app.ui.components.DhanTabBar
import com.dhan.app.ui.components.StatusPill
import com.dhan.app.ui.components.StatusTone
import com.dhan.app.ui.components.formatINR
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.Poppins
import kotlin.math.abs

@Composable
fun DebtScreen(nav: DhanNavActions) {
    val repo = LocalRepository.current
    val prefs = LocalUserPrefs.current
    val friends by repo.friends.observeAll().collectAsState(initial = emptyList())
    val isPlus = prefs.isPlus

    val owed = friends.filter { it.balanceRupees > 0 }.sumOf { it.balanceRupees }
    val owedCount = friends.count { it.balanceRupees > 0 }
    val owe = friends.filter { it.balanceRupees < 0 }.sumOf { -it.balanceRupees }
    val oweCount = friends.count { it.balanceRupees < 0 }

    Scaffold(
        bottomBar = { DhanTabBar(active = DhanTab.MORE, onChange = { nav.toTab(it) }) },
        containerColor = DhanColor.appBg,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DhanScreenHeader(title = "Who owes who", onBack = { nav.toTab(DhanTab.HOME) })
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                    DebtSummary("YOU'RE OWED", owed, owedCount, DhanColor.income, DhanColor.incomeBg, "arrow-down-left", Modifier.weight(1f))
                    DebtSummary("YOU OWE", owe, oweCount, DhanColor.expense, DhanColor.expenseBg, "arrow-up-right", Modifier.weight(1f))
                }
            }

            if (!isPlus) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.blur(5.dp).fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    ) {
                        items(friends) { f -> FriendRow(f, onClick = null) }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(DhanColor.navy, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(DhanIcon.of("star"), contentDescription = null, tint = DhanColor.gold, modifier = Modifier.size(24.dp))
                        }
                        Text("Unlock debt tracker", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                        Text(
                            "Split bills with friends, track what you're owed, and settle up — a Dhan Plus feature.",
                            fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg2, modifier = Modifier.padding(bottom = 14.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        DhanButton(text = "Upgrade to Plus", full = true, onClick = { nav.toPaywall() })
                    }
                }
            } else {
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    if (friends.isEmpty()) {
                        item {
                            Text(
                                "No friends yet. Split a bill to start tracking who owes who.",
                                fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg3,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    }
                    items(friends) { f -> FriendRow(f, onClick = { nav.toFriend(f.id) }) }
                }
            }
        }
    }
}

@Composable
private fun DebtSummary(label: String, amount: Double, count: Int, color: Color, bg: Color, icon: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(bg, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(DhanIcon.of(icon), contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(label, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color)
        }
        Text(formatINR(amount), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color, modifier = Modifier.padding(top = 6.dp))
        Text("from $count ${if (count == 1) "person" else "people"}", fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg2, modifier = Modifier.padding(top = 2.dp))
    }
}

fun avatarColorFor(name: String): Color {
    val palette = listOf(DhanColor.catFood, DhanColor.catTransport, DhanColor.catShopping, DhanColor.catBills, DhanColor.catEnt, DhanColor.catHealth)
    return palette[abs(name.hashCode()) % palette.size]
}

fun initialsFor(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

@Composable
fun FriendRow(f: FriendEntity, onClick: (() -> Unit)?) {
    val owesYou = f.balanceRupees > 0
    val settled = f.balanceRupees == 0.0
    DhanCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        padding = 12.dp,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(44.dp).background(avatarColorFor(f.name), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(initialsFor(f.name), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(f.name, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (settled) {
                StatusPill(text = "Settled", tone = StatusTone.NEUTRAL)
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (owesYou) "OWES YOU" else "YOU OWE",
                        fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                        color = if (owesYou) DhanColor.income else DhanColor.expense,
                    )
                    Text(
                        formatINR(abs(f.balanceRupees)),
                        fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = if (owesYou) DhanColor.income else DhanColor.expense,
                    )
                }
            }
        }
    }
}
