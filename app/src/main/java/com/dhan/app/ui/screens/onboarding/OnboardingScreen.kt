package com.dhan.app.ui.screens.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.nav.DhanNavActions
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanButtonVariant
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.Poppins

private data class OnboardSlide(val title: String, val body: String, val illo: @Composable () -> Unit)

/** Ports screens-onboarding.jsx `OnboardingScreen` (02) — 3-slide intro with a
 *  progress-pill indicator, Skip, and Next/Get started. Skip and finishing slide 3
 *  both land on Sign up. */
@Composable
fun OnboardingScreen(nav: DhanNavActions) {
    var idx by remember { mutableIntStateOf(0) }
    val slides = remember {
        listOf(
            OnboardSlide(
                "Track effortlessly",
                "Dhan reads your UPI & bank SMS to log every transaction — no manual entry, no fuss.",
            ) { OnboardIllo1() },
            OnboardSlide(
                "Budget your way",
                "Pick a framework — 50/30/20, zero-based, or roll your own — and stick to it calmly.",
            ) { OnboardIllo2() },
            OnboardSlide(
                "Stay on top",
                "Bill reminders, overspend alerts, weekly summaries. We nudge; you decide.",
            ) { OnboardIllo3() },
        )
    }
    val slide = slides[idx]
    val next = { if (idx < slides.lastIndex) idx += 1 else nav.toSignup() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                "Skip",
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = DhanColor.fg3,
                modifier = Modifier
                    .clickable(onClick = { nav.toSignup() })
                    .padding(8.dp),
            )
        }

        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(320.dp).padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                slide.illo()
            }
            Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)) {
                Text(
                    slide.title,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = DhanColor.navy,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Text(
                    slide.body,
                    fontFamily = Poppins,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = DhanColor.fg2,
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                slides.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(if (i == idx) 22.dp else 6.dp)
                            .background(if (i == idx) DhanColor.navy else DhanColor.borderStrong, RoundedCornerShape(999.dp)),
                    )
                }
            }
            DhanButton(
                text = if (idx < slides.lastIndex) "Next" else "Get started",
                onClick = next,
                full = true,
                size = com.dhan.app.ui.components.DhanButtonSize.LG,
                iconRight = DhanIcon.of("arrow-right"),
            )
        }
    }
}

@Composable
private fun OnboardIllo1() {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(300f / 260f).background(DhanColor.bgSurface, RoundedCornerShape(20.dp)),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IlloRow(DhanColor.catFood, 0.62f, 0.36f)
            IlloRow(DhanColor.catTransport, 0.44f, 0.5f)
            IlloRow(DhanColor.income, 0.53f, 0.4f)
        }
        Box(
            modifier = Modifier.padding(top = 10.dp, end = 14.dp).size(40.dp)
                .background(DhanColor.gold, CircleShape)
                .align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun IlloRow(dotColor: Color, w1: Float, w2: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp)
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, DhanColor.borderDefault, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.size(20.dp).background(dotColor, CircleShape))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.fillMaxWidth(w1).height(6.dp).background(DhanColor.navy, RoundedCornerShape(3.dp)))
            Box(modifier = Modifier.fillMaxWidth(w2).height(5.dp).background(DhanColor.fg4, RoundedCornerShape(2.5.dp)))
        }
        Box(modifier = Modifier.width(26.dp).height(12.dp).background(dotColor, RoundedCornerShape(3.dp)))
    }
}

@Composable
private fun OnboardIllo2() {
    val softNavy = DhanColor.navy80
    val softGold = DhanColor.goldSoft
    val sage = DhanColor.income
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(300f / 260f)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Butt)
                    val d = size.minDimension - stroke.width
                    val topLeft = androidx.compose.ui.geometry.Offset((size.width - d) / 2, (size.height - d) / 2)
                    val arcSize = Size(d, d)
                    drawArc(DhanColor.borderSubtle, 0f, 360f, false, topLeft = topLeft, size = arcSize, style = Stroke(width = 18.dp.toPx()))
                    // Start at top (-90deg), clockwise: 50% needs, 30% wants, 20% savings.
                    drawArc(softNavy, -90f, 180f, false, topLeft = topLeft, size = arcSize, style = stroke)
                    drawArc(softGold, 90f, 108f, false, topLeft = topLeft, size = arcSize, style = stroke)
                    drawArc(sage, 198f, 72f, false, topLeft = topLeft, size = arcSize, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("50/30/20", fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = DhanColor.navy)
                    Text(
                        "BUDGET", fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                        color = DhanColor.fg3, letterSpacing = 1.5.sp,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendChip(softNavy, "Needs 50%")
                LegendChip(softGold, "Wants 30%")
                LegendChip(sage, "Save 20%")
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(label, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = DhanColor.navy)
    }
}

@Composable
private fun OnboardIllo3() {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(300f / 260f).background(DhanColor.bgSurface, RoundedCornerShape(22.dp)),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IlloRowSmall(DhanColor.warning, 0.6f, 0.42f)
            IlloRowSmall(DhanColor.expense, 0.7f, 0.5f)
            IlloRowSmall(DhanColor.income, 0.53f, 0.62f)
        }
        Box(
            modifier = Modifier.padding(top = 6.dp, end = 6.dp).size(40.dp)
                .background(DhanColor.gold, CircleShape)
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                DhanIcon.of("bell"), contentDescription = null, tint = DhanColor.navy,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun IlloRowSmall(dotColor: Color, w1: Float, w2: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp)
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, DhanColor.borderDefault, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.size(16.dp).background(dotColor, CircleShape))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.fillMaxWidth(w1).height(5.dp).background(DhanColor.navy, RoundedCornerShape(2.5.dp)))
            Box(modifier = Modifier.fillMaxWidth(w2).height(4.dp).background(DhanColor.fg4, RoundedCornerShape(2.dp)))
        }
    }
}
