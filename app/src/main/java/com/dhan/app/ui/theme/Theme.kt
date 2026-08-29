package com.dhan.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Dhan is designed as a single fixed light theme (navy/gold), matching the
// Claude Design handoff. There is no dark-mode variant in the source design.
private val DhanColorScheme = lightColorScheme(
    primary = DhanColor.navy,
    onPrimary = DhanColor.fgOnDark,
    secondary = DhanColor.gold,
    onSecondary = DhanColor.fgOnGold,
    background = DhanColor.appBg,
    onBackground = DhanColor.fg1,
    surface = DhanColor.bgElevated,
    onSurface = DhanColor.fg1,
    surfaceVariant = DhanColor.bgSurface,
    onSurfaceVariant = DhanColor.fg2,
    error = DhanColor.expense,
    onError = DhanColor.fgOnDark,
    outline = DhanColor.borderDefault,
    outlineVariant = DhanColor.borderSubtle,
)

@Composable
fun DhanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DhanColorScheme,
        typography = DhanMaterialTypography,
        content = content,
    )
}
