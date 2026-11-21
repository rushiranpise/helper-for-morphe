package dev.rushi.apkdownloadhelper.ui.shared.source

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.ui.shared.selection.RadioDot
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsItemCard
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.ResolveState
import dev.rushi.apkdownloadhelper.ui.model.SourceCandidateGroup

@Composable
internal fun SourceGrid(
    groups: List<SourceCandidateGroup>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.chunked(2).forEach { rowGroups ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowGroups.forEach { group ->
                    val index = groups.indexOfFirst { it.source == group.source }
                    SourceCard(
                        group = group,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowGroups.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Reusable dropdown item: source brand icon + label + optional check. */
@Composable
internal fun SourceMenuItem(
    source: DownloadSource,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(source.label) },
        leadingIcon = { SourceAvatar(source = source, size = 24.dp) },
        trailingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        onClick = onClick
    )
}

/**
 * Fill used by the list/selection cards (sources, dropdown rows, stat cards).
 * Matches the manager's card surface so every row sits on the same neutral tone
 * in both themes instead of a hand-tuned per-theme shade.
 */
@Composable
internal fun sourceCardFill(): Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

@Composable
internal fun sourceCardBorder(): Color = MaterialTheme.colorScheme.outlineVariant

@Composable
internal fun SourceCard(
    group: SourceCandidateGroup,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val confirmed = group.source != DownloadSource.PLAY &&
        ((group.latest as? ResolveState.Done)?.candidates?.isNotEmpty() == true ||
            (group.recommended as? ResolveState.Done)?.candidates?.isNotEmpty() == true)

    SettingsItemCard(
        onClick = onClick,
        borderWidth = if (selected) 1.5.dp else 1.dp,
        borderColor = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceAvatar(source = group.source)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = group.source.label,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (confirmed) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.available),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            RadioDot(selected = selected)
        }
    }
}

internal data class SourceBrand(
    val resId: Int
)

internal fun DownloadSource.brand(): SourceBrand = when (this) {
    DownloadSource.PLAY -> SourceBrand(resId = R.drawable.ic_src_play)
    DownloadSource.APK_MIRROR -> SourceBrand(resId = R.drawable.ic_src_apkmirror)
    DownloadSource.APK_PURE -> SourceBrand(resId = R.drawable.ic_src_apkpure)
    DownloadSource.APK_CUBE -> SourceBrand(resId = R.drawable.ic_src_apkcube)
    DownloadSource.UPTODOWN -> SourceBrand(resId = R.drawable.ic_src_uptodown)
    DownloadSource.AURORA -> SourceBrand(resId = R.drawable.ic_src_aurora)
    DownloadSource.APTOIDE -> SourceBrand(resId = R.drawable.ic_src_aptoide)
    DownloadSource.MORPHE_LINK -> SourceBrand(resId = R.drawable.ic_src_morphe)
}

@Composable
internal fun SourceAvatar(source: DownloadSource, size: Dp = 40.dp) {
    // Official brand logo: square artwork, shown as-is with rounded corners sized to the
    // artwork so a small inline icon stays a rounded square rather than turning into a circle.
    Image(
        painter = painterResource(source.brand().resId),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
    )
}
