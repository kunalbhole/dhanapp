package com.dhan.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button as M3Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins
import androidx.compose.ui.text.font.FontWeight

enum class DhanButtonVariant { PRIMARY, SECONDARY, GOLD, GHOST, OUTLINE, DESTRUCTIVE }
enum class DhanButtonSize(val height: androidx.compose.ui.unit.Dp, val fontSize: androidx.compose.ui.unit.TextUnit, val hPad: androidx.compose.ui.unit.Dp) {
    SM(36.dp, 13.sp, 14.dp),
    MD(48.dp, 15.sp, 20.dp),
    LG(56.dp, 16.sp, 24.dp),
}

private data class VariantColors(val bg: Color, val fg: Color, val border: Color? = null)

private fun colorsFor(variant: DhanButtonVariant): VariantColors = when (variant) {
    DhanButtonVariant.PRIMARY -> VariantColors(DhanColor.navy, Color.White)
    DhanButtonVariant.SECONDARY -> VariantColors(DhanColor.bgSurface, DhanColor.navy)
    DhanButtonVariant.GOLD -> VariantColors(DhanColor.gold, DhanColor.navy)
    DhanButtonVariant.GHOST -> VariantColors(Color.Transparent, DhanColor.navy)
    DhanButtonVariant.OUTLINE -> VariantColors(Color.White, DhanColor.navy, DhanColor.borderDefault)
    DhanButtonVariant.DESTRUCTIVE -> VariantColors(DhanColor.expenseBg, DhanColor.expense)
}

/** Ports components.jsx `Button`. Primary CTAs use navy per the mid-project audit
 *  (see chats/chat1.md) — gold is reserved for decorative accents, never CTAs. */
@Composable
fun DhanButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DhanButtonVariant = DhanButtonVariant.PRIMARY,
    size: DhanButtonSize = DhanButtonSize.MD,
    full: Boolean = false,
    icon: ImageVector? = null,
    iconRight: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = colorsFor(variant)
    val widthMod = if (full) modifier.fillMaxWidth() else modifier
    M3Button(
        onClick = onClick,
        enabled = enabled,
        modifier = widthMod.height(size.height),
        shape = RoundedCornerShape(DhanRadius.control),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.bg,
            contentColor = c.fg,
            disabledContainerColor = c.bg.copy(alpha = 0.5f),
            disabledContentColor = c.fg.copy(alpha = 0.5f),
        ),
        border = c.border?.let { BorderStroke(1.dp, it) },
        contentPadding = PaddingValues(horizontal = size.hPad),
        elevation = null,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Box(Modifier.width(8.dp))
        }
        Text(text, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = size.fontSize)
        if (iconRight != null) {
            Box(Modifier.width(8.dp))
            Icon(iconRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/** Ports components.jsx `FAB` — always navy per the CTA-color audit. */
@Composable
fun DhanFab(onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector = com.dhan.app.ui.theme.DhanIcon.of("plus")) {
    Box(
        modifier = modifier
            .size(56.dp)
            .background(DhanColor.navy, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Icon(icon, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(26.dp))
    }
}
