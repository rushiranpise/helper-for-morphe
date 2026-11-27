package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.rushi.apkdownloadhelper.ui.theme.AppAnimations

internal class DialogEntry(
    content: @Composable () -> Unit,
    onDismiss: () -> Unit,
    dismissOnBack: Boolean,
    topBar: (@Composable BoxScope.() -> Unit)?,
    bottomBar: (@Composable BoxScope.() -> Unit)?,
    bottomReserve: Dp
) {
    // Starts hidden, target visible: that is what plays the enter animation
    val state = MutableTransitionState(false).apply { targetState = true }
    var content by mutableStateOf(content)
    var onDismiss by mutableStateOf(onDismiss)
    var dismissOnBack by mutableStateOf(dismissOnBack)
    var topBar by mutableStateOf(topBar)
    var bottomBar by mutableStateOf(bottomBar)
    var bottomReserve by mutableStateOf(bottomReserve)
}

internal class DialogLayerState {
    val entries = mutableStateListOf<DialogEntry>()
}

internal val LocalDialogLayer = staticCompositionLocalOf<DialogLayerState?> { null }

/**
 * Put once around the app's root. Every dialog opened below it is drawn here, inside the
 * composition (no system window), so it animates exactly like the settings sub page.
 */
@Composable
internal fun DialogLayerHost(content: @Composable () -> Unit) {
    val layer = remember { DialogLayerState() }
    CompositionLocalProvider(LocalDialogLayer provides layer) {
        Box(Modifier.fillMaxSize()) {
            content()
            layer.entries.toList().forEach { entry ->
                key(entry) { DialogEntryHost(entry, layer) }
            }
        }
    }
}

@Composable
private fun DialogEntryHost(entry: DialogEntry, layer: DialogLayerState) {
    val state = entry.state
    BackHandler(enabled = state.targetState) { if (entry.dismissOnBack) entry.onDismiss() }
    // Drop the entry once its exit animation has finished
    LaunchedEffect(state.currentState, state.targetState) {
        if (!state.currentState && !state.targetState) layer.entries.remove(entry)
    }
    AnimatedVisibility(
        visibleState = state,
        enter = AppAnimations.pushEnter,
        exit = AppAnimations.pushExit,
        modifier = Modifier.fillMaxSize()
    ) {
        DialogPage(
            topBar = entry.topBar,
            bottomBar = entry.bottomBar,
            bottomReserve = entry.bottomReserve
        ) { entry.content() }
    }
}

/**
 * Full-screen page behind every dialog: theme background and insets. [topBar] and [bottomBar]
 * are drawn in the page's own BoxScope, so they are placed like on the full screens
 * (`Modifier.align(Alignment.TopCenter / BottomCenter)`). [bottomReserve] keeps the centred
 * content above a bottom bar when needed.
 */
@Composable
internal fun DialogPage(
    topBar: (@Composable BoxScope.() -> Unit)? = null,
    bottomBar: (@Composable BoxScope.() -> Unit)? = null,
    bottomReserve: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    // With a pinned bottom bar, it (not the page) covers the navigation bar area
    val insets = if (bottomBar != null) {
        WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else {
        WindowInsets.systemBars
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(insets)
                .imePadding()
                .padding(bottom = bottomReserve),
            contentAlignment = Alignment.Center
        ) { content() }
        topBar?.invoke(this)
        bottomBar?.invoke(this)
    }
}

/** What every dialog composable calls. Registers [content] with the host above. */
@Composable
internal fun DialogWindow(
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean = true,
    topBar: (@Composable BoxScope.() -> Unit)? = null,
    bottomBar: (@Composable BoxScope.() -> Unit)? = null,
    bottomReserve: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val layer = LocalDialogLayer.current
    if (layer == null) {
        // Safety net if DialogLayerHost is missing: old system dialog, no custom animation
        Dialog(onDismissRequest = onDismissRequest, properties = AppDialogProperties) {
            DialogPage(topBar = topBar, bottomBar = bottomBar, bottomReserve = bottomReserve) {
                content()
            }
        }
        return
    }
    val entry = remember {
        DialogEntry(content, onDismissRequest, dismissOnBackPress, topBar, bottomBar, bottomReserve)
    }
    SideEffect {
        entry.content = content
        entry.onDismiss = onDismissRequest
        entry.dismissOnBack = dismissOnBackPress
        entry.topBar = topBar
        entry.bottomBar = bottomBar
        entry.bottomReserve = bottomReserve
    }
    DisposableEffect(layer) {
        layer.entries.add(entry)
        onDispose { entry.state.targetState = false }
    }
}