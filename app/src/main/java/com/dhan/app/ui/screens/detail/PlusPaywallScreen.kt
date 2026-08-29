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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins

private data class PlusPlan(val id: String, val label: String, val price: String, val sub: String, val tag: String? = null)
private data class PlusFeature(val icon: String, val title: String, val body: String)

private val plusPlans = listOf(
    PlusPlan("annual", "Annual", "₹1,799", "₹150/mo · save ₹600", tag = "BEST VALUE"),
    PlusPlan("monthly", "Monthly", "₹199", "Per month · cancel anytime"),
)

private val plusFeatures = listOf(
    PlusFeature("users-three", "Split bills with friends", "Track who owes who, settle up via UPI."),
    PlusFeature("sparkle", "AI-powered insights", "Anomaly detection, savings tips, monthly reports."),
    PlusFeature("stack", "Custom categories & rules", "Auto-tag merchants. Build your own categories."),
    PlusFeature("download-simple", "Bank-grade exports", "PDF, Excel, ITR-ready statements."),
    PlusFeature("shield-check", "Priority support", "Skip the queue. Reply within 1 hour."),
    PlusFeature("trend-up", "Multi-account analytics", "Combined dashboard across all linked accounts."),
)

/** Ported from screens-extra-detail.jsx's PlusPaywallScreen (#24). No real payment
 *  integration exists (explicitly out of scope) — "Upgrade" flips [LocalUserPrefs.isPlus]
 *  locally, same as the source prototype's `onUpgrade`.
 *
 *  Deviation: the JSX renders the bottom CTA with a gold gradient, but the app-wide rule
 *  (gold is decorative-only; every primary CTA is navy) overrides that — [DhanButton]'s
 *  default PRIMARY variant is used here too, even on this screen's dark background, for
 *  consistency with every other CTA in the app. The gold accent stays on the plan-selector
 *  ring/tag, which is decorative, not a button. */
@Composable
fun PlusPaywallScreen(nav: DhanNavActions) {
    val prefs = LocalUserPrefs.current
    var plan by remember { mutableStateOf("annual") }

    Scaffold(containerColor = DhanColor.ink) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable { nav.back() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(DhanIcon.of("x"), contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Brush.linearGradient(listOf(DhanColor.gold, DhanColor.goldSoft)), RoundedCornerShape(DhanRadius.card)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(DhanIcon.of("star"), contentDescription = null, tint = DhanColor.ink, modifier = Modifier.size(32.dp))
                    }
                    Text(
                        "DHAN PLUS", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.gold,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                    )
                    Text(
                        "Money superpowers,\nfewer surprises.", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 26.sp,
                        color = Color.White, textAlign = TextAlign.Center, lineHeight = 31.sp,
                    )
                    Text(
                        "Unlock everything Dhan can do for you.", fontFamily = Poppins, fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Box(Modifier.height(18.dp))
                Column {
                    plusPlans.forEach { option ->
                        val active = plan == option.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (active) Color(0x1FC9A84C) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(DhanRadius.card))
                                .clickable { plan = option.id }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(if (active) DhanColor.gold else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (active) {
                                    Icon(DhanIcon.of("check"), contentDescription = null, tint = DhanColor.ink, modifier = Modifier.size(12.dp))
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(option.label, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                    if (option.tag != null) {
                                        Text(
                                            option.tag, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = DhanColor.ink,
                                            modifier = Modifier.background(DhanColor.gold, RoundedCornerShape(DhanRadius.pill)).padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                                Text(option.sub, fontFamily = Poppins, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp))
                            }
                            Text(option.price, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        }
                        Box(Modifier.height(8.dp))
                    }
                }

                Box(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(DhanRadius.card))
                        .padding(14.dp),
                ) {
                    plusFeatures.forEachIndexed { i, f ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).background(Color(0x26C9A84C), RoundedCornerShape(DhanRadius.control)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(DhanIcon.of(f.icon), contentDescription = null, tint = DhanColor.gold, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text(f.title, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Text(f.body, fontFamily = Poppins, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        if (i != plusFeatures.lastIndex) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
                        }
                    }
                }
                Box(Modifier.height(16.dp))
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                DhanButton(
                    text = "Start free 7-day trial",
                    full = true,
                    iconRight = DhanIcon.of("arrow-right"),
                    onClick = {
                        prefs.isPlus = true
                        nav.back()
                    },
                )
                val trialNote = if (plan == "annual") "Then ₹1,799/year." else "Then ₹199/month."
                Text(
                    "Cancel anytime. $trialNote\nRestore purchase · Terms · Privacy",
                    fontFamily = Poppins, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }
        }
    }
}
