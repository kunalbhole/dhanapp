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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.components.DhanField
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins

private data class ProfileRow(val icon: String, val label: String, val value: String = "", val danger: Boolean = false)

/** Ported from screens-extra-detail.jsx's ProfileEditScreen (#21). Only [name] is backed
 *  by anything real ([LocalUserPrefs.current.userName]) — email/DOB/city/mobile have no
 *  server or local store anywhere in this app, so they stay plain unpersisted text fields
 *  (cleared on next launch) rather than pretending to save. The JSX's hardcoded sample
 *  values ("priya.s@gmail.com" etc.) are dropped in favor of blank fields with placeholders.
 *
 *  Deviation: header "Save" is a checkmark icon (see EditBudgetScreen for why — the shared
 *  [DhanScreenHeader] `right` slot is a fixed 40dp square). The Account rows (change PIN,
 *  two-factor, delete account) are shown for visual completeness but are inert — none of
 *  those flows exist yet, and wiring "delete account" to actually wipe the on-device
 *  database felt like a bigger, more destructive decision than this screen's spec asked for. */
@Composable
fun ProfileEditScreen(nav: DhanNavActions) {
    val prefs = LocalUserPrefs.current
    var name by remember { mutableStateOf(prefs.userName) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    Scaffold(containerColor = DhanColor.appBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DhanScreenHeader(
                title = "Profile",
                onBack = { nav.back() },
                right = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(DhanColor.navy, RoundedCornerShape(DhanRadius.control))
                            .clickable {
                                prefs.userName = name
                                nav.back()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(DhanIcon.of("check"), contentDescription = "Save", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                },
            )

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .background(Brush.linearGradient(listOf(DhanColor.gold, DhanColor.goldSoft)), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            name.firstOrNull()?.uppercase() ?: "D",
                            fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = DhanColor.navy,
                        )
                    }
                    Text(
                        "Photo isn't wired up yet", fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }

                DhanField(value = name, onValueChange = { name = it }, label = "Full name", placeholder = "Your name")
                DhanField(value = email, onValueChange = { email = it }, label = "Email", placeholder = "you@example.com", keyboardType = KeyboardType.Email)
                DhanField(value = phone, onValueChange = { phone = it }, label = "Mobile", placeholder = "+91 00000 00000", keyboardType = KeyboardType.Phone)
                DhanField(value = dob, onValueChange = { dob = it }, label = "Date of birth", placeholder = "DD MMM YYYY")
                DhanField(value = city, onValueChange = { city = it }, label = "City", placeholder = "Your city")

                Text(
                    "ACCOUNT", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp),
                )
                DhanCard(modifier = Modifier.fillMaxWidth(), padding = 4.dp) {
                    Column {
                        val rows = listOf(
                            ProfileRow("key", "Change PIN"),
                            ProfileRow("shield-check", "Two-factor auth", value = "Off"),
                            ProfileRow("trash", "Delete account", danger = true),
                        )
                        rows.forEachIndexed { i, row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp),
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
                                if (row.value.isNotEmpty()) {
                                    Text(row.value, fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3)
                                }
                                Icon(DhanIcon.of("caret-right"), contentDescription = null, tint = DhanColor.fg4, modifier = Modifier.size(14.dp))
                            }
                            if (i != rows.lastIndex) {
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DhanColor.borderSubtle))
                            }
                        }
                    }
                }
                Box(Modifier.height(24.dp))
            }
        }
    }
}
