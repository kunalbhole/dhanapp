package com.dhan.app.ui.screens.main

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.capture.CapturePermissions
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanBottomSheet
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanButtonVariant
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanTab
import com.dhan.app.ui.components.DhanTabBar
import com.dhan.app.ui.components.StatusPill
import com.dhan.app.ui.components.StatusTone
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.Poppins

private data class SettingsRow(val icon: String, val label: String, val value: String? = null, val danger: Boolean = false, val onClick: () -> Unit)

@Composable
fun SettingsScreen(nav: DhanNavActions) {
    val prefs = LocalUserPrefs.current
    val context = LocalContext.current
    var isPlus by remember { mutableStateOf(prefs.isPlus) }
    var smsGranted by remember { mutableStateOf(CapturePermissions.hasSmsPermission(context)) }
    var notifGranted by remember { mutableStateOf(CapturePermissions.isNotificationListenerEnabled(context)) }
    var signOutOpen by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        smsGranted = CapturePermissions.hasSmsPermission(context)
        prefs.smsCaptureEnabled = smsGranted
    }

    Scaffold(
        bottomBar = { DhanTabBar(active = DhanTab.MORE, onChange = { nav.toTab(it) }) },
        containerColor = DhanColor.appBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text("More", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(vertical = 16.dp))

            DhanCard(modifier = Modifier.fillMaxWidth(), onClick = { nav.toProfile() }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(56.dp).background(DhanColor.gold, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            prefs.userName.firstOrNull()?.uppercase() ?: "D",
                            fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = DhanColor.navy,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(prefs.userName.ifBlank { "Dhan user" }, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Box(Modifier.height(4.dp))
                        StatusPill(text = if (isPlus) "PLUS" else "Free plan", tone = if (isPlus) StatusTone.WARNING else StatusTone.NEUTRAL)
                    }
                    Icon(DhanIcon.of("caret-right"), contentDescription = null, tint = DhanColor.fg3, modifier = Modifier.size(18.dp))
                }
            }

            Box(Modifier.height(14.dp))

            if (!isPlus) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DhanColor.navy, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(DhanIcon.of("star"), contentDescription = null, tint = DhanColor.gold, modifier = Modifier.size(16.dp))
                        Text("DHAN PLUS", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.gold)
                    }
                    Text("Split bills, track debts, more.", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
                    Text(
                        "Unlock the debt tracker, custom categories, and monthly insights.",
                        fontFamily = Poppins, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 14.dp),
                    )
                    DhanButton(text = "Upgrade ₹199/mo", onClick = { nav.toPaywall() })
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().background(DhanColor.gold, RoundedCornerShape(20.dp)).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(DhanIcon.of("star"), contentDescription = null, tint = DhanColor.navy, modifier = Modifier.size(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Dhan Plus · active", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DhanColor.navy)
                    }
                    DhanButton(text = "Manage", variant = DhanButtonVariant.OUTLINE, size = com.dhan.app.ui.components.DhanButtonSize.SM, onClick = { prefs.isPlus = false; isPlus = false })
                }
            }

            Box(Modifier.height(18.dp))

            SettingsSection(
                title = "Capture",
                rows = listOf(
                    SettingsRow(
                        icon = "device-mobile",
                        label = "SMS capture",
                        value = if (smsGranted) "On" else "Off — tap to grant",
                        onClick = {
                            if (!smsGranted) {
                                smsPermissionLauncher.launch(arrayOf(CapturePermissions.SMS_PERMISSION, CapturePermissions.READ_SMS_PERMISSION))
                            }
                        },
                    ),
                    SettingsRow(
                        icon = "bell-ringing",
                        label = "Notification capture",
                        value = if (notifGranted) "On" else "Off — tap to enable",
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                    ),
                ),
            )

            SettingsSection(
                title = "Account",
                rows = listOf(
                    SettingsRow("user", "Profile", onClick = { nav.toProfile() }),
                    SettingsRow("credit-card", "Linked accounts", onClick = { nav.toLinked() }),
                    SettingsRow("question", "Help & support", onClick = { nav.toHelp() }),
                    SettingsRow("sign-out", "Sign out", danger = true, onClick = { signOutOpen = true }),
                ),
            )

            SettingsSection(
                title = "App info",
                rows = listOf(
                    SettingsRow("info", "About Dhan", onClick = {}),
                    SettingsRow("receipt", "Terms of service", onClick = {}),
                    SettingsRow("shield-check", "Privacy policy", onClick = {}),
                    SettingsRow("info", "Version", value = "1.0", onClick = {}),
                ),
            )

            Text(
                "Made with ♥ · धन", fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg3,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            )
        }
    }

    DhanBottomSheet(open = signOutOpen, onClose = { signOutOpen = false }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Box(
                modifier = Modifier.size(56.dp).background(DhanColor.expenseBg, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DhanIcon.of("sign-out"), contentDescription = null, tint = DhanColor.expense, modifier = Modifier.size(26.dp))
            }
            Text("Sign out of Dhan?", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            Text(
                "Your data stays on this device. You'll need your PIN to get back in.",
                fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
        DhanButton(
            text = "Sign out", full = true, variant = DhanButtonVariant.DESTRUCTIVE,
            onClick = {
                prefs.hasOnboarded = false
                signOutOpen = false
                nav.signOutToSplash()
            },
        )
        Box(Modifier.height(8.dp))
        DhanButton(text = "Cancel", full = true, variant = DhanButtonVariant.GHOST, onClick = { signOutOpen = false })
    }
}

@Composable
private fun SettingsSection(title: String, rows: List<SettingsRow>) {
    Text(
        title.uppercase(), fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
    )
    DhanCard(modifier = Modifier.fillMaxWidth(), padding = 4.dp) {
        Column {
            rows.forEachIndexed { i, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = row.onClick)
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        DhanIcon.of(row.icon), contentDescription = null,
                        tint = if (row.danger) DhanColor.expense else DhanColor.navy,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        row.label, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        color = if (row.danger) DhanColor.expense else DhanColor.fg1,
                        modifier = Modifier.weight(1f),
                    )
                    if (row.value != null) {
                        Text(row.value, fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3)
                    }
                    Icon(DhanIcon.of("caret-right"), contentDescription = null, tint = DhanColor.fg4, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
    Box(Modifier.height(18.dp))
}
