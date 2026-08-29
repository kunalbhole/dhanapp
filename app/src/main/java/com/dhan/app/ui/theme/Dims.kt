package com.dhan.app.ui.theme

import androidx.compose.ui.unit.dp

// Spacing — 8px grid, 4px half-step allowed (--s-*)
object DhanSpace {
    val s0 = 0.dp
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s7 = 32.dp
    val s8 = 40.dp
    val s9 = 48.dp
    val s10 = 64.dp
}

// Radii — "Soft" shape table (the shipped default; --r-* tokens)
object DhanRadius {
    val input = 8.dp
    val control = 12.dp
    val cardSm = 14.dp
    val card = 16.dp
    val cardLg = 20.dp
    val sheet = 24.dp
    val pill = 999.dp
    val icon = 0.30f // fractional corner (30%) for CategoryIcon
}

object DhanLayout {
    val tabBarHeight = 76.dp
    val statusBarHeight = 44.dp
}
