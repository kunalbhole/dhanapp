package com.dhan.app.ui.screens.auth

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins
import kotlinx.coroutines.delay

private enum class ResetStep { VERIFY, NEW_PIN, CONFIRM }

/** Ports screens-extra-auth.jsx `ForgotPinScreen` (26). This is a local-only app, so
 *  there's no real SMS delivery behind the "one-time code" step — any 6 digits pass —
 *  it's kept only for layout fidelity with the source design. verify -> new PIN ->
 *  confirm -> saves the new PIN and returns to Login. */
@Composable
fun ForgotPinScreen(nav: DhanNavActions) {
    val prefs = LocalUserPrefs.current
    var step by remember { mutableStateOf(ResetStep.VERIFY) }
    var otp by remember { mutableStateOf("") }
    val newPin = remember { mutableStateListOf<String>() }
    val confirmPin = remember { mutableStateListOf<String>() }
    var error by remember { mutableStateOf("") }

    val activeList = if (step == ResetStep.NEW_PIN) newPin else confirmPin
    val onKey: (String) -> Unit = { k ->
        error = ""
        if (k == "x") {
            if (activeList.isNotEmpty()) activeList.removeAt(activeList.lastIndex)
        } else if (activeList.size < 4) {
            activeList.add(k)
        }
    }

    LaunchedEffect(step, confirmPin.size) {
        if (step == ResetStep.CONFIRM && confirmPin.size == 4) {
            delay(250)
            if (confirmPin.joinToString("") == newPin.joinToString("")) {
                prefs.pin = newPin.joinToString("")
                nav.toLogin()
            } else {
                error = "PINs don't match"
                confirmPin.clear()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DhanScreenHeader(
            title = "Reset PIN",
            onBack = { if (step == ResetStep.VERIFY) nav.toLogin() else step = ResetStep.VERIFY },
        )
        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp)) {
            when (step) {
                ResetStep.VERIFY -> {
                    Text(
                        "Verify it's you", fontFamily = Poppins, fontWeight = FontWeight.Bold,
                        fontSize = 22.sp, color = DhanColor.fg1,
                    )
                    Text(
                        "We sent a 6-digit code to your registered number ending in •• 4421.",
                        fontFamily = Poppins, fontSize = 13.5.sp, color = DhanColor.fg2, lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        Text(
                            "ONE-TIME CODE", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            color = DhanColor.fg3, letterSpacing = 0.7.sp, modifier = Modifier.padding(bottom = 8.dp),
                        )
                        BasicTextField(
                            value = otp,
                            onValueChange = { v -> otp = v.filter { it.isDigit() }.take(6) },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = Poppins, fontSize = 22.sp, color = DhanColor.fg1,
                                textAlign = TextAlign.Center, letterSpacing = 5.sp,
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth()
                                .border(1.dp, DhanColor.borderSubtle, RoundedCornerShape(DhanRadius.cardSm))
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                        )
                        Text(
                            "Resend code", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            color = DhanColor.navy, modifier = Modifier.padding(top = 10.dp).clickable { },
                        )
                    }
                    Box(Modifier.weight(1f))
                    DhanButton(
                        text = "Verify", full = true, size = com.dhan.app.ui.components.DhanButtonSize.LG,
                        enabled = otp.length == 6, iconRight = DhanIcon.of("arrow-right"),
                        onClick = { step = ResetStep.NEW_PIN },
                    )
                    Box(Modifier.padding(bottom = 16.dp))
                }
                ResetStep.NEW_PIN, ResetStep.CONFIRM -> {
                    Text(
                        if (step == ResetStep.NEW_PIN) "Set a new PIN" else "Confirm your PIN",
                        fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = DhanColor.fg1,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Text(
                        if (step == ResetStep.NEW_PIN) "Pick 4 digits you'll remember." else "Enter it again to confirm.",
                        fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg3, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                    ) {
                        Box(Modifier.weight(1f))
                        repeat(4) { i ->
                            Box(
                                modifier = Modifier.size(16.dp)
                                    .background(if (activeList.size > i) DhanColor.navy else Color.Transparent, CircleShape)
                                    .border(2.dp, if (error.isNotEmpty()) DhanColor.expense else DhanColor.borderStrong, CircleShape),
                            )
                        }
                        Box(Modifier.weight(1f))
                    }
                    if (error.isNotEmpty()) {
                        Text(
                            error, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = DhanColor.expense, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        )
                    }
                    Box(Modifier.weight(1f))
                    if (step == ResetStep.NEW_PIN && newPin.size == 4) {
                        DhanButton(
                            text = "Continue", full = true, size = com.dhan.app.ui.components.DhanButtonSize.LG,
                            iconRight = DhanIcon.of("arrow-right"),
                            onClick = { step = ResetStep.CONFIRM },
                        )
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        userScrollEnabled = false,
                    ) {
                        items(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "x")) { k ->
                            ResetPinKey(k = k, onClick = onKey)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetPinKey(k: String, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(56.dp)
            .background(if (k == "x" || k.isEmpty()) Color.Transparent else DhanColor.bgSurface, RoundedCornerShape(DhanRadius.cardSm))
            .let { if (k.isNotEmpty()) it.clickable { onClick(k) } else it },
        contentAlignment = Alignment.Center,
    ) {
        if (k == "x") {
            Icon(DhanIcon.of("backspace"), contentDescription = "Delete", tint = DhanColor.fg2, modifier = Modifier.size(22.dp))
        } else if (k.isNotEmpty()) {
            Text(k, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = DhanColor.fg1)
        }
    }
}
