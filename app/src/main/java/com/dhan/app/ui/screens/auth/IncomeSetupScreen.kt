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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.components.formatINR
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins
import java.text.NumberFormat
import java.util.Locale

private val inrFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }

/** Ports screens-extra-auth.jsx `IncomeSetupScreen` (15) — a mock monthly-income
 *  entry with quick presets and a live 50/30/20 preview. Continue -> Framework picker. */
@Composable
fun IncomeSetupScreen(nav: DhanNavActions) {
    var income by remember { mutableIntStateOf(82500) }
    val presets = remember { listOf(50000, 75000, 100000, 150000) }

    Column(modifier = Modifier.fillMaxSize()) {
        DhanScreenHeader(title = "Monthly income", onBack = { nav.back() })
        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(
                "What's coming in?", fontFamily = Poppins, fontWeight = FontWeight.Bold,
                fontSize = 24.sp, color = DhanColor.fg1, modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                "Roughly your take-home each month. We'll use this to suggest budgets.",
                fontFamily = Poppins, fontSize = 14.sp, color = DhanColor.fg2, lineHeight = 21.sp,
                modifier = Modifier.padding(bottom = 28.dp),
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    "PER MONTH", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    color = DhanColor.fg3, letterSpacing = 1.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Text("₹", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = DhanColor.navy)
                    Box(Modifier.width(4.dp))
                    BasicTextField(
                        value = inrFormat.format(income),
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }
                            income = digits.toIntOrNull() ?: 0
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 46.sp,
                            color = DhanColor.navy, textAlign = TextAlign.Center,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(240.dp),
                    )
                }
                Text(
                    "That's ${formatINR(income.toLong() * 12)} a year",
                    fontFamily = Poppins, fontSize = 12.sp, color = DhanColor.fg3, modifier = Modifier.padding(top = 6.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { p ->
                    val active = income == p
                    Text(
                        text = if (p >= 100000) "₹${p / 100000}L" else "₹${p / 1000}k",
                        fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        color = if (active) Color.White else DhanColor.fg2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                            .background(if (active) DhanColor.navy else Color.White, RoundedCornerShape(999.dp))
                            .border(1.dp, if (active) DhanColor.navy else DhanColor.borderSubtle, RoundedCornerShape(999.dp))
                            .clickable { income = p }
                            .padding(vertical = 8.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(DhanColor.bgSurface, RoundedCornerShape(DhanRadius.card))
                    .padding(16.dp),
            ) {
                Text(
                    "SUGGESTED 50/30/20 SPLIT", fontFamily = Poppins, fontWeight = FontWeight.Bold,
                    fontSize = 11.sp, color = DhanColor.fg3, letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                listOf(
                    Triple("Needs", 0.5, DhanColor.navy),
                    Triple("Wants", 0.3, DhanColor.gold),
                    Triple("Save", 0.2, DhanColor.income),
                ).forEach { (label, frac, color) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(999.dp)))
                            Text(
                                "$label · ${(frac * 100).toInt()}%", fontFamily = Poppins, fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp, color = DhanColor.fg1,
                            )
                        }
                        Text(
                            formatINR((income * frac).toInt()), fontFamily = Poppins, fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp, color = DhanColor.fg1,
                        )
                    }
                }
            }

            Box(Modifier.weight(1f))

            Text(
                "You can adjust this any time. Variable income? Set an average.",
                fontFamily = Poppins, fontSize = 11.sp, color = DhanColor.fg3, textAlign = TextAlign.Center,
                lineHeight = 15.sp, modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp),
            )
            DhanButton(
                text = "Continue", full = true, size = com.dhan.app.ui.components.DhanButtonSize.LG,
                enabled = income > 0, iconRight = DhanIcon.of("arrow-right"),
                onClick = { nav.toFramework() },
            )
            Box(Modifier.padding(bottom = 20.dp))
        }
    }
}
