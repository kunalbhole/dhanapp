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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanScreenHeader
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins

private data class Bank(val id: String, val name: String, val color: Color, val short: String)

/** Ports screens-extra-auth.jsx `LinkBankScreen` (14). Illustrative only — there is no
 *  real bank/UPI linking integration; "linking" a bank just flips local mock state, and
 *  both Skip and Continue always proceed to Income setup. */
@Composable
fun LinkBankScreen(nav: DhanNavActions) {
    val banks = remember {
        listOf(
            Bank("hdfc", "HDFC Bank", Color(0xFF004C8F), "HD"),
            Bank("icici", "ICICI Bank", Color(0xFFF37920), "IC"),
            Bank("axis", "Axis Bank", Color(0xFF97144D), "AX"),
            Bank("sbi", "State Bank", Color(0xFF22409A), "SB"),
            Bank("kotak", "Kotak Mahindra", Color(0xFFED1A3B), "KM"),
            Bank("yes", "Yes Bank", Color(0xFF00518F), "YS"),
        )
    }
    val linked = remember { mutableStateListOf("hdfc") }

    Column(modifier = Modifier.fillMaxSize()) {
        DhanScreenHeader(
            title = "Link your accounts",
            onBack = { nav.back() },
            right = {
                Text(
                    "Skip", fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = DhanColor.fg3, modifier = Modifier.clickable { nav.toIncome() },
                )
            },
        )
        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(
                "Pick your banks", fontFamily = Poppins, fontWeight = FontWeight.Bold,
                fontSize = 24.sp, color = DhanColor.fg1, modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                "Dhan reads SMS only — no logins, no OTPs. Add the ones you use.",
                fontFamily = Poppins, fontSize = 14.sp, color = DhanColor.fg2, lineHeight = 21.sp,
                modifier = Modifier.padding(bottom = 18.dp),
            )

            banks.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { bank ->
                        BankCard(
                            bank = bank,
                            isLinked = linked.contains(bank.id),
                            onClick = {
                                if (linked.contains(bank.id)) linked.remove(bank.id) else linked.add(bank.id)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }

            Box(Modifier.weight(1f))

            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(DhanColor.goldBg, RoundedCornerShape(DhanRadius.control))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    "${linked.size} linked. Add more anytime in Settings.",
                    fontFamily = Poppins, fontSize = 13.sp, color = DhanColor.fg2, lineHeight = 18.sp,
                )
            }
            Box(Modifier.padding(top = 12.dp))
            DhanButton(
                text = "Continue", full = true, size = com.dhan.app.ui.components.DhanButtonSize.LG,
                enabled = linked.isNotEmpty(), iconRight = DhanIcon.of("arrow-right"),
                onClick = { nav.toIncome() },
            )
            Box(Modifier.padding(bottom = 20.dp))
        }
    }
}

@Composable
private fun BankCard(bank: Bank, isLinked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(DhanRadius.cardSm))
            .border(2.dp, if (isLinked) DhanColor.navy else DhanColor.borderSubtle, RoundedCornerShape(DhanRadius.cardSm))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(bank.color, RoundedCornerShape(DhanRadius.control)),
                contentAlignment = Alignment.Center,
            ) {
                Text(bank.short, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }
            if (isLinked) {
                Box(
                    modifier = Modifier.size(22.dp).background(DhanColor.income, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(DhanIcon.of("check"), contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        Text(
            bank.name, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DhanColor.fg1,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            if (isLinked) "Linked · SMS detected" else "Tap to link",
            fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp,
            color = if (isLinked) DhanColor.income else DhanColor.fg3,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
