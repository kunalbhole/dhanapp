package com.dhan.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.db.TxnSource
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.components.formatINR
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.Poppins
import com.dhan.app.util.DateUtils

/** Ports the source design's static "notification feed" concept, but backed by the real
 *  capture-engine audit trail (CaptureEventEntity) rather than fabricated alerts — this
 *  is where a user can see exactly what the SMS/notification listener has scanned and
 *  whether it turned into a logged transaction. */
@Composable
fun NotificationsScreen(nav: DhanNavActions) {
    val repo = LocalRepository.current
    val events by repo.captureEvents.observeRecent().collectAsState(initial = emptyList())
    val groups = events.groupBy { DateUtils.dayGroupLabel(it.timestampMillis) }

    Scaffold(containerColor = DhanColor.appBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DhanScreenHeader(title = "Activity", onBack = { nav.back() })
            if (events.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(DhanIcon.of("bell"), contentDescription = null, tint = DhanColor.fg3, modifier = Modifier.size(40.dp))
                    Text(
                        "No activity yet. Once SMS or notification capture is on in Settings, scanned messages will show up here.",
                        fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg3,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            } else {
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    groups.forEach { (day, items) ->
                        item(key = "h-$day") {
                            Text(
                                day.uppercase(), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
                                modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 6.dp),
                            )
                        }
                        items(items, key = { it.id }) { e ->
                            val matched = e.matched
                            val icon = if (e.source == TxnSource.SMS) "device-mobile" else "bell-ringing"
                            val (bg, fg) = if (matched) DhanColor.incomeBg to DhanColor.income else DhanColor.bgSurface to DhanColor.fg3
                            DhanCard(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                onClick = if (matched && e.createdTxnId != null) ({ nav.toTxnDetail(e.createdTxnId) }) else null,
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(bg, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(DhanIcon.of(icon), contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            if (matched) "Transaction detected · ${e.sourceApp ?: e.source.name}" else "Message scanned, not a transaction",
                                            fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                        )
                                        Text(
                                            e.rawText, fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg2,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                    Text(DateUtils.timeLabel(e.timestampMillis), fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg3)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
