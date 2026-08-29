package com.dhan.app.data.repo

import androidx.compose.runtime.staticCompositionLocalOf

/** Provided once at the app root (MainActivity) from DhanApplication.repository.
 *  Screens read `LocalRepository.current` instead of threading the repository through
 *  every composable's parameter list. */
val LocalRepository = staticCompositionLocalOf<DhanRepository> {
    error("LocalRepository not provided — wrap the app in CompositionLocalProvider(LocalRepository provides ...)")
}
