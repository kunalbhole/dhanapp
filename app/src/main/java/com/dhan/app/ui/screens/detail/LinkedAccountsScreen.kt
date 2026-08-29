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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.capture.notif.TxnNotificationListenerService
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanButtonVariant
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.components.StatusPill
import com.dhan.app.ui.components.StatusTone
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins

/** Ported from screens-extra-detail.jsx's LinkedAccountsScreen (#22). The JSX shows three
 *  bank accounts with fake balances and "synced N minutes ago" timestamps — this app has
 *  no real bank-linking backend to source that from. Instead this screen lists the real
 *  set of bank/UPI apps the on-device capture engine is scoped to read notifications from
 *  ([TxnNotificationListenerService.TRACKED_PACKAGES]), and shows the real on/off state of
 *  SMS and notification capture from [LocalUserPrefs]. */
@Composable
fun LinkedAccountsScreen(nav: DhanNavActions) {
    val prefs = LocalUserPrefs.current
    val trackedApps = remember { TxnNotificationListenerService.TRACKED_PACKAGES.values.distinct().sorted() }

    Scaffold(containerColor = DhanColor.appBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DhanScreenHeader(title = "Linked accounts", onBack = { nav.back() })

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f),
            ) {
                item {
                    val anyOn = prefs.smsCaptureEnabled || prefs.notifCaptureEnabled
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (anyOn) DhanColor.incomeBg else DhanColor.warningBg, RoundedCornerShape(DhanRadius.control))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            DhanIcon.of(if (anyOn) "check-circle" else "warning"), contentDescription = null,
                            tint = if (anyOn) DhanColor.income else DhanColor.warning, modifier = Modifier.size(18.dp),
                        )
                        Column {
                            Text(
                                if (anyOn) "Auto-capture is on" else "Auto-capture is off",
                                fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DhanColor.fg1,
                            )
                            Text(
                                "SMS capture: ${if (prefs.smsCaptureEnabled) "On" else "Off"} · Notification capture: ${if (prefs.notifCaptureEnabled) "On" else "Off"}",
                                fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg2, lineHeight = 17.sp,
                            )
                        }
                    }
                    Box(Modifier.height(16.dp))
                    Text(
                        "SUPPORTED APPS · ${trackedApps.size}", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                }

                items(trackedApps) { label ->
                    DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), padding = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(40.dp).background(DhanColor.navy, RoundedCornerShape(DhanRadius.control)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    label.take(2).uppercase(), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                    color = androidx.compose.ui.graphics.Color.White,
                                )
                            }
                            Text(label, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            StatusPill(text = "Auto-detected", tone = StatusTone.INFO)
                        }
                    }
                }

                item {
                    Box(Modifier.height(8.dp))
                    DhanButton(
                        text = "Manage capture settings", full = true, variant = DhanButtonVariant.OUTLINE,
                        icon = DhanIcon.of("gear"), onClick = { nav.toSettings() },
                    )

                    Text(
                        "PRIVACY", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
                        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
                    )
                    DhanCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Dhan reads only notifications and SMS from the apps above, on this device. We never store login " +
                                "credentials and never connect to your bank's servers — nothing leaves your phone.",
                            fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg2, lineHeight = 18.sp,
                        )
                    }
                    Box(Modifier.height(24.dp))
                }
            }
        }
    }
}
