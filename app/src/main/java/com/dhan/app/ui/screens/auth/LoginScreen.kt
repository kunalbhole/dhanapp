package com.dhan.app.ui.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins
import kotlinx.coroutines.delay

private const val MAX_ATTEMPTS = 5

/** Ports screens-extra-auth.jsx `LoginScreen` (12) — returning-user PIN pad.
 *
 *  Deviations from the JSX mock, both deliberate:
 *  1. There is no separate "create PIN" screen in this design. If `prefs.pin` is null
 *     this doubles as PIN *creation*: whatever 4 digits the user enters is accepted and
 *     saved. Once a PIN exists, entries are compared against it.
 *  2. The JSX locks after 3 wrong attempts but the source project's own history flagged
 *     that as needing a real lockout UI — implemented here as a full locked state after
 *     5 wrong attempts in a row (the keypad becomes inert until the user resets via
 *     Forgot PIN). The JSX's "face" key (`k === "face"`) auto-succeeds as a simulated
 *     biometric bypass with no biometric hardware behind it; since no biometric
 *     capability was specified for this build, that key is kept only as an inert
 *     decorative glyph rather than ported as a fake instant-login shortcut.
 */
@Composable
fun LoginScreen(nav: DhanNavActions) {
    val prefs = LocalUserPrefs.current
    val isNewPin = prefs.pin == null
    val pin = remember { mutableStateListOf<String>() }
    var error by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    val locked = attempts >= MAX_ATTEMPTS
    val shake = remember { Animatable(0f) }

    val onKey: (String) -> Unit = onKey@{ k ->
        if (locked) return@onKey
        error = false
        when {
            k == "x" -> if (pin.isNotEmpty()) pin.removeAt(pin.lastIndex)
            pin.size < 4 -> pin.add(k)
        }
    }

    LaunchedEffect(pin.size) {
        if (pin.size == 4) {
            delay(280)
            val entered = pin.joinToString("")
            if (isNewPin) {
                prefs.pin = entered
                nav.toHomeFresh()
            } else if (entered == prefs.pin) {
                nav.toHomeFresh()
            } else {
                error = true
                attempts += 1
                pin.clear()
                shake.snapTo(0f)
                shake.animateTo(-6f, animationSpec = androidx.compose.animation.core.tween(90))
                shake.animateTo(6f, animationSpec = androidx.compose.animation.core.tween(180))
                shake.animateTo(0f, animationSpec = androidx.compose.animation.core.tween(90))
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Dhan", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 30.sp,
            color = DhanColor.navy, modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
        )

        Box(
            modifier = Modifier.size(72.dp).background(DhanColor.gold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (prefs.userName.firstOrNull() ?: 'D').uppercase(), fontFamily = Poppins,
                fontWeight = FontWeight.Bold, fontSize = 26.sp, color = DhanColor.navy,
            )
        }
        Text(
            if (isNewPin) "Welcome, ${prefs.userName.ifBlank { "there" }}" else "Welcome back, ${prefs.userName.ifBlank { "there" }}",
            fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DhanColor.fg1,
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
        )
        Text(
            if (isNewPin) "Create a 4-digit PIN" else "Enter your 4-digit PIN",
            fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg3, modifier = Modifier.padding(bottom = 24.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 18.dp).offset(x = shake.value.dp),
        ) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier.size(16.dp)
                        .background(if (pin.size > i) DhanColor.navy else Color.Transparent, CircleShape)
                        .border(2.dp, if (error) DhanColor.expense else DhanColor.borderStrong, CircleShape),
                )
            }
        }
        if (error && !locked) {
            Text(
                "Wrong PIN. ${MAX_ATTEMPTS - attempts} ${if (MAX_ATTEMPTS - attempts == 1) "try" else "tries"} left.",
                fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DhanColor.expense,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        if (locked) {
            Text(
                "Too many attempts. Reset your PIN to continue.",
                fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DhanColor.expense,
                textAlign = TextAlign.Center, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Text(
            "Forgot PIN?", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            color = DhanColor.navy, modifier = Modifier.clickable { nav.toForgotPin() }.padding(bottom = 8.dp),
        )

        Box(Modifier.weight(1f))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(),
            userScrollEnabled = false,
        ) {
            items(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "scan", "0", "x")) { k ->
                PinKey(k = k, enabled = !locked, onClick = onKey)
            }
        }
    }
}

@Composable
private fun PinKey(k: String, enabled: Boolean, onClick: (String) -> Unit) {
    val isAction = k == "scan" || k == "x"
    Box(
        modifier = Modifier.fillMaxWidth().height(56.dp)
            .background(if (isAction) Color.Transparent else DhanColor.bgSurface, RoundedCornerShape(DhanRadius.cardSm))
            .let { if (k != "scan" && enabled) it.clickable { onClick(k) } else it },
        contentAlignment = Alignment.Center,
    ) {
        when (k) {
            "scan" -> Icon(
                DhanIcon.of("scan"), contentDescription = null,
                tint = DhanColor.navy.copy(alpha = 0.35f), modifier = Modifier.size(24.dp),
            )
            "x" -> Icon(
                DhanIcon.of("backspace"), contentDescription = "Delete",
                tint = if (enabled) DhanColor.fg2 else DhanColor.fg4, modifier = Modifier.size(22.dp),
            )
            else -> Text(
                k, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
                color = if (enabled) DhanColor.fg1 else DhanColor.fg4,
            )
        }
    }
}
