package com.dhan.app.ui.screens.auth

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins

private data class PermItem(
    val icon: String,
    val title: String,
    val body: String,
    val color: Color,
    val required: Boolean = false,
)

/** Ports screens-extra-auth.jsx `PermissionsScreen` (13). Unlike the JSX mock, the two
 *  real permissions (SMS, notification listener) actually reflect and act on Android
 *  permission state; Contacts stays a decorative optional toggle (no OS permission or
 *  capability hook was specified for it — it's opt-in for a future Dhan Plus feature). */
@Composable
fun PermissionsScreen(nav: DhanNavActions) {
    val prefs = LocalUserPrefs.current
    val context = LocalContext.current
    var smsGranted by remember { mutableStateOf(CapturePermissions.hasSmsPermission(context)) }
    var notifGranted by remember { mutableStateOf(CapturePermissions.isNotificationListenerEnabled(context)) }
    var contactsOn by remember { mutableStateOf(false) }

    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        smsGranted = CapturePermissions.hasSmsPermission(context)
        prefs.smsCaptureEnabled = smsGranted
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DhanScreenHeader(title = "Permissions", onBack = { nav.back() })
        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(
                "A few permissions", fontFamily = Poppins, fontWeight = FontWeight.Bold,
                fontSize = 24.sp, color = DhanColor.fg1, modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                "Dhan works best with these. You can change any of them later in Settings.",
                fontFamily = Poppins, fontSize = 14.sp, color = DhanColor.fg2, lineHeight = 21.sp,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            PermRow(
                item = PermItem("chat-centered-text", "Read SMS", "We auto-detect bank & UPI txns. Stays on-device — never uploaded.", DhanColor.navy, required = true),
                on = smsGranted,
                onToggle = {
                    if (!smsGranted) {
                        smsLauncher.launch(arrayOf(CapturePermissions.SMS_PERMISSION, CapturePermissions.READ_SMS_PERMISSION))
                    }
                },
            )
            PermRow(
                item = PermItem("bell", "Notifications", "Bill reminders, overspend nudges, weekly summaries.", DhanColor.gold),
                on = notifGranted,
                onToggle = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
            )
            PermRow(
                item = PermItem("address-book", "Contacts", "Optional — only if you split bills with friends on Dhan Plus.", DhanColor.catBills),
                on = contactsOn,
                onToggle = { contactsOn = !contactsOn },
            )

            Box(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(DhanColor.bgSurface, RoundedCornerShape(DhanRadius.control))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(DhanIcon.of("shield-check"), contentDescription = null, tint = DhanColor.income, modifier = Modifier.size(18.dp))
                Column {
                    Text(
                        "Bank-grade security.",
                        fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = DhanColor.fg1,
                    )
                    Text(
                        "Your data stays on this device. We never store SMS content on our servers.",
                        fontFamily = Poppins, fontSize = 11.5.sp, color = DhanColor.fg2, lineHeight = 16.sp,
                    )
                }
            }
            Box(Modifier.padding(top = 14.dp)) {
                DhanButton(
                    text = "Continue", full = true, size = com.dhan.app.ui.components.DhanButtonSize.LG,
                    iconRight = DhanIcon.of("arrow-right"),
                    onClick = { nav.toLinkBank() },
                )
            }
            Box(Modifier.padding(bottom = 20.dp))
        }
    }
}

@Composable
private fun PermRow(item: PermItem, on: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, DhanColor.borderSubtle, RoundedCornerShape(DhanRadius.card))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(item.color.copy(alpha = 0.08f), RoundedCornerShape(DhanRadius.control)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DhanIcon.of(item.icon), contentDescription = null, tint = item.color, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DhanColor.fg1)
                if (item.required) {
                    Text(
                        "RECOMMENDED", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                        color = DhanColor.navy, letterSpacing = 0.5.sp,
                        modifier = Modifier.background(DhanColor.goldBg, RoundedCornerShape(999.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                item.body, fontFamily = Poppins, fontSize = 11.5.sp, color = DhanColor.fg3,
                lineHeight = 15.sp, modifier = Modifier.padding(top = 3.dp),
            )
        }
        Switch(
            checked = on, onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedTrackColor = DhanColor.navy, checkedThumbColor = Color.White),
        )
    }
    Box(Modifier.padding(bottom = 10.dp))
}
