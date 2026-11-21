package dev.rushi.apkdownloadhelper.ui.shared.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Opaque full-screen holder for a screen that is pushed over another one, so whatever sits
 * underneath never shows through mid-slide.
 *
 * The tap handler swallows taps the pushed screen itself did not use, which keeps them from
 * reaching the screen below.
 *
 * Focus needs the same treatment, and it cannot be done here: keys walk the whole tree, so a
 * screen left composed underneath keeps taking every D-pad press (on Android TV the settings
 * page opened and then ignored the remote). Callers must therefore drop the covered screen
 * from composition while this one is visible - see the home screen's `AnimatedVisibility`.
 */
@Composable
internal fun PushedScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        content()
    }
}
