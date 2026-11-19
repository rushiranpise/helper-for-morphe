package dev.rushi.apkdownloadhelper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.rushi.apkdownloadhelper.R
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import dev.chrisbanes.haze.hazeSource

internal enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    SOURCES("Sources", "Enable, disable and reorder providers", Icons.Outlined.Dns),
    APPEARANCE("Appearance", "Theme, colors, and app icon", Icons.Outlined.Palette),
    CUSTOMIZATION("More Customization", "Find Apps layout, cards & preview", Icons.Outlined.Tune),
    DOWNLOADS("Downloads", "Save location, storage & Fast Mode", Icons.Outlined.SdStorage),
    SECURITY_NETWORK("Security & Network", "Network policy, DNS & VirusTotal", Icons.Outlined.Security),
    SYSTEM_LOGS("System & Logs", "History, logs & diagnostics", Icons.AutoMirrored.Outlined.ViewList),
    ABOUT("About", "Version, updates & GitHub", Icons.Outlined.Info)
}

@Composable
internal fun SettingsPage(
    versionName: String,
    onOpenCategory: (SettingsCategory) -> Unit
) {
    val search = SettingsSearch { "" }
    val scrollState = rememberScrollState()

    MorphePinnedHeaderPage(
        header = {
            MorpheHeaderBar(title = "Settings")
        },
        scrollState = scrollState,
        backgroundColor = Color.Transparent
    ) { insets ->
        CompositionLocalProvider(LocalSettingsSearch provides search) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollFade(scrollState)
                    .verticalScroll(scrollState)
                    .padding(
                        start = insets.horizontal,
                        top = insets.top + 16.dp,
                        end = insets.horizontal,
                        bottom = 120.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsGroup {
                    SettingsCategory.entries.forEach { categoryItem ->
                        SettingsItem(
                            onClick = { onOpenCategory(categoryItem) },
                            icon = categoryItem.icon,
                            title = categoryItem.title,
                            subtitle = categoryItem.subtitle
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsPageDetail(
    title: String,
    onBack: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler { onBack() }

    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme
    val pageHaze = remember { HazeState() }
    var headerHeightDp by remember { mutableStateOf(0.dp) }
    var footerHeightDp by remember { mutableStateOf(0.dp) }

    val isScrolled by remember(scrollState) {
        derivedStateOf { scrollState.value > 0 }
    }
    val headerShape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        // 1. Scrollable Settings Subpage Content (haze source for top header & bottom Close pill)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = pageHaze)
                .verticalScroll(scrollState)
                .padding(
                    top = headerHeightDp + 12.dp,
                    start = MorpheDefaults.ContentPadding,
                    end = MorpheDefaults.ContentPadding,
                    bottom = footerHeightDp + 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )

        // 2. Top Frosted Blur Header matching AppBrowserScreen (no top back button)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    headerHeightDp = with(density) { size.height.toDp() }
                }
                .then(
                    if (isScrolled) {
                        Modifier.glassFill(
                            glass = MorpheColors.glassContainerColor(),
                            shape = headerShape,
                            hazeState = pageHaze
                        )
                    } else {
                        Modifier
                    }
                )
                .statusBarsPadding()
                .padding(
                    horizontal = MorpheDefaults.ContentPadding,
                    vertical = 12.dp
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3. Telegram-style Bottom Dim Gradient Scrim (behind Close pill & system nav bar)
        val navBarHeight = with(density) {
            WindowInsets.navigationBars.getBottom(this).toDp()
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(navBarHeight + 76.dp)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to scheme.background.copy(alpha = 0.55f),
                        0.80f to scheme.background.copy(alpha = 0.85f),
                        1.0f to scheme.background.copy(alpha = 0.96f)
                    )
                )
        )

        // 4. Pinned Bottom Frosted "Close" Pill matching AppDetailView
        val closeInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    footerHeightDp = with(density) { size.height.toDp() }
                }
                .navigationBarsPadding()
                .padding(
                    horizontal = MorpheDefaults.ContentPadding,
                    vertical = 12.dp
                )
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .pressScale(closeInteraction)
                    .glassFill(
                        glass = Color.Transparent,
                        shape = CircleShape,
                        hazeState = pageHaze
                    ),
                interactionSource = closeInteraction,
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = scheme.onSurface.copy(alpha = 0.85f)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = stringResource(R.string.close),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}