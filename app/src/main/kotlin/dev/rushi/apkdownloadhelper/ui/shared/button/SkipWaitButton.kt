package dev.rushi.apkdownloadhelper.ui.shared.button

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner
import kotlinx.coroutines.delay

/**
 * "Skip wait" with a live countdown of the seconds left in the current
 * rate-limit pause. While the scan status shows a wait, this re-reads the
 * shared limiter's remaining gap each second and shows "Skip wait · 12s";
 * once the pause ends the button disappears. Tapping skips the wait early.
 */
@Composable
internal fun SkipWaitButton(active: Boolean, onSkipWait: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(0) }
    // Flips when the user taps: hides the button immediately so the tap gives
    // visible feedback even though the parent's "Waiting…" status (and hence
    // `active`) stays true until the scan's next network result arrives.
    var skipped by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (!active) {
            skipped = false
            return@LaunchedEffect
        }
        while (!skipped) {
            // Re-read from the limiter each tick so the countdown stays honest
            // even if the status message lags a second behind the real pause.
            secondsLeft = ((VirusTotalScanner.rateLimiter.millisUntilNextSlot() + 999) / 1000).toInt()
            if (secondsLeft <= 0) break
            delay(1000)
        }
    }
    if (active && !skipped) {
        HelperOutlinedButton(
            text = if (secondsLeft > 0) "Skip wait · ${secondsLeft}s" else "Skip wait",
            onClick = {
                skipped = true
                onSkipWait()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
