package com.dhan.app.ui.screens.detail

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanCard
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins

private data class HelpTopic(val icon: String, val title: String, val category: String)

private val helpTopics = listOf(
    HelpTopic("chat-circle-text", "How does SMS reading work?", "Setup"),
    HelpTopic("credit-card", "Add a new bank account", "Accounts"),
    HelpTopic("receipt", "Set up bill reminders", "Bills"),
    HelpTopic("users-three", "Splitting bills with friends", "Plus"),
    HelpTopic("shield-check", "Privacy & data security", "Privacy"),
    HelpTopic("download-simple", "Export your data", "Data"),
)

/** Ported from screens-extra-detail.jsx's HelpScreen (#23). Fully static FAQ content per
 *  spec (no support backend exists) — the search box does real local filtering of the
 *  topic list, and "Email support" / "Contact support" launch the device's mail app via
 *  an ACTION_SENDTO intent (a real OS action) rather than a fabricated in-app chat. */
@Composable
fun HelpScreen(nav: DhanNavActions) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) helpTopics else helpTopics.filter { it.title.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
    }
    fun openMail() {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:help@dhan.in")))
        }
    }

    Scaffold(containerColor = DhanColor.appBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DhanScreenHeader(title = "Help & support", onBack = { nav.back() })
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color.White, RoundedCornerShape(DhanRadius.control))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(DhanIcon.of("magnifying-glass"), contentDescription = null, tint = DhanColor.fg3, modifier = Modifier.size(18.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text("Search for help…", fontFamily = Poppins, fontSize = 14.sp, color = DhanColor.fg3)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Poppins, fontSize = 14.sp, color = DhanColor.fg1),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Box(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("chats", "Chat with us", "Coming soon", DhanColor.navy, onClick = null, modifier = Modifier.weight(1f))
                    QuickAction("envelope", "Email support", "help@dhan.in", DhanColor.gold, onClick = { openMail() }, modifier = Modifier.weight(1f))
                }

                Box(Modifier.height(16.dp))
                Text(
                    "POPULAR TOPICS", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                DhanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp), padding = 4.dp) {
                    if (filtered.isEmpty()) {
                        Text(
                            "No topics match “$query”.", fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg3,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        Column {
                            filtered.forEachIndexed { i, topic ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp).background(DhanColor.bgSurface, RoundedCornerShape(DhanRadius.control)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(DhanIcon.of(topic.icon), contentDescription = null, tint = DhanColor.navy, modifier = Modifier.size(16.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(topic.title, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(topic.category, fontFamily = Poppins, fontSize = 10.5.sp, color = DhanColor.fg3, modifier = Modifier.padding(top = 1.dp))
                                    }
                                    Icon(DhanIcon.of("caret-right"), contentDescription = null, tint = DhanColor.fg4, modifier = Modifier.size(14.dp))
                                }
                                if (i != filtered.lastIndex) {
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DhanColor.borderSubtle))
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DhanColor.bgSurface, RoundedCornerShape(DhanRadius.cardSm))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Still stuck?", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text(
                        "We reply within a few hours, even on weekends.",
                        fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg2, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    DhanButton(text = "Contact support", icon = DhanIcon.of("paper-plane-tilt"), onClick = { openMail() })
                }
                Box(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun QuickAction(icon: String, title: String, body: String, accent: Color, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(DhanRadius.cardSm))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(accent.copy(alpha = 0.1f), RoundedCornerShape(DhanRadius.control)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DhanIcon.of(icon), contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Text(title, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        Text(body, fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg3, modifier = Modifier.padding(top = 2.dp))
    }
}
