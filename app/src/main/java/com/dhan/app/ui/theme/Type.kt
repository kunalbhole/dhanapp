package com.dhan.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dhan.app.R

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

// Dhan type scale — 1:1 with --t-* tokens in colors_and_type.css (390px reference).
object DhanType {
    val display = TextStyle(
        fontFamily = Poppins, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.02).em,
    )
    val currencyBig = display

    val h1 = TextStyle(
        fontFamily = Poppins, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.01).em,
    )
    val h2 = TextStyle(
        fontFamily = Poppins, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.005).em,
    )
    val h3 = TextStyle(
        fontFamily = Poppins, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 26.sp,
    )
    val body = TextStyle(
        fontFamily = Poppins, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp,
    )
    val bodySm = TextStyle(
        fontFamily = Poppins, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 20.sp,
    )
    val caption = TextStyle(
        fontFamily = Poppins, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.01.em,
    )
    val label = TextStyle(
        fontFamily = Poppins, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.06.em,
    )
}

// Bridge into Material3's Typography so default components pick up Poppins too.
val DhanMaterialTypography = Typography(
    displayLarge = DhanType.display,
    headlineLarge = DhanType.h1,
    headlineMedium = DhanType.h2,
    headlineSmall = DhanType.h3,
    titleLarge = DhanType.h2,
    titleMedium = DhanType.h3,
    bodyLarge = DhanType.body,
    bodyMedium = DhanType.bodySm,
    bodySmall = DhanType.caption,
    labelLarge = DhanType.label.copy(textAlign = TextAlign.Start),
    labelMedium = DhanType.label,
    labelSmall = DhanType.label,
)
