package com.dhan.app.ui.screens.auth

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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanButtonVariant
import com.dhan.app.ui.components.DhanField
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.Poppins

/** Ports screens-onboarding.jsx `SignUpScreen` (03). Step 0 collects name + phone,
 *  step 1 is a 4-digit OTP mock. "Continue with Google" and completing the OTP both
 *  just carry the user forward — there is no real OAuth/SMS wired up (out of scope). */
@Composable
fun SignUpScreen(nav: DhanNavActions) {
    val prefs = LocalUserPrefs.current
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf(listOf("", "", "", "")) }
    val otpFull = otp.all { it.isNotEmpty() }

    fun finish() {
        prefs.userName = name.ifBlank { "Dhan user" }
        nav.toPermissions()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DhanScreenHeader(
            title = if (step == 0) "Create account" else "Verify number",
            onBack = { if (step == 0) nav.back() else step = 0 },
        )
        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
            if (step == 0) {
                Text(
                    "Let's get you set up.", fontFamily = Poppins, fontWeight = FontWeight.Bold,
                    fontSize = 24.sp, color = DhanColor.fg1, modifier = Modifier.padding(bottom = 6.dp),
                )
                Text(
                    "We'll send a 4-digit code to verify your number.", fontFamily = Poppins,
                    fontSize = 14.sp, color = DhanColor.fg2, modifier = Modifier.padding(bottom = 20.dp),
                )
                DhanField(
                    value = name, onValueChange = { name = it },
                    label = "Your name", placeholder = "First + last",
                )
                DhanField(
                    value = phone, onValueChange = { phone = it },
                    label = "Mobile number", placeholder = "98xxx xxxxx", prefix = "+91",
                    keyboardType = KeyboardType.Phone,
                )
                Box(Modifier.weight(1f))
                Text(
                    "By continuing, you agree to Dhan's Terms of Service and Privacy Policy.",
                    fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg3,
                    textAlign = TextAlign.Center, lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
                DhanButton(
                    text = "Continue", full = true, size = com.dhan.app.ui.components.DhanButtonSize.LG,
                    enabled = name.isNotBlank() && phone.isNotBlank(),
                    onClick = { step = 1 },
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.weight(1f).height(1.dp).background(DhanColor.borderSubtle))
                    Text("OR", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DhanColor.fg3)
                    Box(Modifier.weight(1f).height(1.dp).background(DhanColor.borderSubtle))
                }
                DhanButton(
                    text = "Continue with Google", full = true, size = com.dhan.app.ui.components.DhanButtonSize.LG,
                    variant = DhanButtonVariant.OUTLINE, icon = DhanIcon.of("google-logo"),
                    onClick = { finish() },
                )
                Box(Modifier.height(20.dp))
            } else {
                Text(
                    "Enter the 4-digit code", fontFamily = Poppins, fontWeight = FontWeight.Bold,
                    fontSize = 24.sp, color = DhanColor.fg1, modifier = Modifier.padding(bottom = 6.dp),
                )
                Row(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text("Sent to ", fontFamily = Poppins, fontSize = 14.sp, color = DhanColor.fg2)
                    Text("+91 $phone", fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DhanColor.fg1)
                    Text(
                        "  Edit", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = DhanColor.navy, modifier = Modifier.clickable { step = 0 },
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    otp.forEachIndexed { i, d ->
                        OtpBox(
                            value = d,
                            onChange = { v ->
                                if (v.length <= 1 && v.all { it.isDigit() }) {
                                    otp = otp.toMutableList().also { it[i] = v }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text(
                    "Didn't get it? Resend in 24s", fontFamily = Poppins, fontSize = 13.sp,
                    color = DhanColor.fg3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Box(Modifier.weight(1f))
                DhanButton(
                    text = "Create account", full = true, size = com.dhan.app.ui.components.DhanButtonSize.LG,
                    enabled = otpFull, onClick = { finish() },
                )
                Box(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun OtpBox(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.height(64.dp),
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 26.sp,
            color = DhanColor.navy, textAlign = TextAlign.Center,
        ),
        shape = RoundedCornerShape(com.dhan.app.ui.theme.DhanRadius.control),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next,
        ),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DhanColor.navy,
            unfocusedBorderColor = if (value.isNotEmpty()) DhanColor.navy else DhanColor.borderDefault,
            focusedContainerColor = androidx.compose.ui.graphics.Color.White,
            unfocusedContainerColor = androidx.compose.ui.graphics.Color.White,
        ),
    )
}
