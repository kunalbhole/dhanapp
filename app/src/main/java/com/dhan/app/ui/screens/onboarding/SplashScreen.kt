package com.dhan.app.ui.screens.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dhan.app.R
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.nav.DhanNavActions
import kotlinx.coroutines.delay

/** Ports screens-onboarding.jsx `SplashScreen` (01) — the Dhan logo mark for ~1.8s then
 *  `onDone()`. A returning user (hasOnboarded) skips straight to Login instead of
 *  Onboarding, matching the source design's `hasAccount()` branch in app.jsx. */
@Composable
fun SplashScreen(nav: DhanNavActions) {
    val prefs = LocalUserPrefs.current
    val scale = remember { Animatable(0.9f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(600))
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(500))
    }
    LaunchedEffect(Unit) {
        delay(1200)
        if (prefs.hasOnboarded) nav.toLogin() else nav.toOnboarding()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.dhan_logo),
            contentDescription = "Dhan",
            modifier = Modifier
                .width(140.dp)
                .height(152.dp)
                .scale(scale.value)
                .alpha(alpha.value),
        )
    }
}
