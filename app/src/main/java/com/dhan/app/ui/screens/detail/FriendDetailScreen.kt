package com.dhan.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.db.DebtEntryEntity
import com.dhan.app.data.db.FriendEntity
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.components.formatINR
import com.dhan.app.ui.screens.main.avatarColorFor
import com.dhan.app.ui.screens.main.initialsFor
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.Poppins
import com.dhan.app.util.DateUtils
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun FriendDetailScreen(nav: DhanNavActions, id: Long) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    var friend by remember(id) { mutableStateOf<FriendEntity?>(null) }
    LaunchedEffect(id) { friend = repo.friends.getById(id) }
    val entries by repo.friends.observeEntries(id).collectAsState(initial = emptyList())

    Scaffold(containerColor = DhanColor.appBg) { padding ->
        val f = friend
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DhanScreenHeader(title = f?.name ?: "Friend", onBack = { nav.back() })
            if (f == null) return@Column
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(72.dp).background(avatarColorFor(f.name), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(initialsFor(f.name), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Color.White)
                    }
                    Text(
                        when { f.balanceRupees > 0 -> "OWES YOU"; f.balanceRupees < 0 -> "YOU OWE"; else -> "SETTLED" },
                        fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DhanColor.fg3,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                        formatINR(abs(f.balanceRupees)),
                        fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 32.sp,
                        color = if (f.balanceRupees > 0) DhanColor.income else if (f.balanceRupees < 0) DhanColor.expense else DhanColor.fg1,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                DhanButton(
                    text = if (f.balanceRupees == 0.0) "All settled" else "Settle up",
                    full = true,
                    enabled = f.balanceRupees != 0.0,
                    onClick = {
                        scope.launch {
                            repo.friends.insertEntry(
                                DebtEntryEntity(
                                    friendId = f.id,
                                    description = "Settled up",
                                    amountRupees = -f.balanceRupees,
                                    timestampMillis = System.currentTimeMillis(),
                                ),
                            )
                            repo.friends.update(f.copy(balanceRupees = 0.0))
                            friend = f.copy(balanceRupees = 0.0)
                        }
                    },
                )
                Box(Modifier.height(20.dp))
                Text(
                    "HISTORY", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                DhanCard(modifier = Modifier.fillMaxWidth(), padding = 8.dp) {
                    if (entries.isEmpty()) {
                        Text("No shared expenses yet.", fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg3, modifier = Modifier.padding(16.dp))
                    } else {
                        Column {
                            entries.forEachIndexed { i, t ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(t.description, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(DateUtils.dateLabel(t.timestampMillis), fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg3, modifier = Modifier.padding(top = 2.dp))
                                    }
                                    Text(
                                        (if (t.amountRupees > 0) "+" else "−") + formatINR(abs(t.amountRupees)),
                                        fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                        color = if (t.amountRupees > 0) DhanColor.income else DhanColor.fg1,
                                    )
                                }
                            }
                        }
                    }
                }
                Box(Modifier.height(24.dp))
            }
        }
    }
}
