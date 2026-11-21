package dev.rushi.apkdownloadhelper.ui.shared.appinfo

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.isPackageInstalled
import dev.rushi.apkdownloadhelper.download.orderedFileKinds
import dev.rushi.apkdownloadhelper.ui.shared.appcard.AppAvatar
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadgeRow
import dev.rushi.apkdownloadhelper.ui.shared.layout.AnimatedExpand
import dev.rushi.apkdownloadhelper.ui.shared.source.sourceCardBorder
import dev.rushi.apkdownloadhelper.ui.shared.source.sourceCardFill
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AppInfoCard(
    request: HelperRequest,
    onFormatToggle: (String) -> Unit
) {
    SurfaceCard(cornerRadius = AppDefaults.SectionCornerRadius) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppInfoHeader(request, onFormatToggle)
        }
    }
}

@Composable
internal fun AppInfoHeader(
    request: HelperRequest,
    onFormatToggle: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    var copied by remember(request.packageName) { mutableStateOf(false) }
    var installed by remember(request.packageName) { mutableStateOf(false) }
    LaunchedEffect(request.packageName) {
        installed = withContext(Dispatchers.IO) {
            context.isPackageInstalled(request.packageName)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppAvatar(
                packageName = request.packageName,
                initial = request.appName.firstOrNull()?.uppercaseChar() ?: '?'
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = request.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (installed) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = stringResource(R.string.installed),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = request.packageName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.copy_package),
                        tint = if (copied) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .size(16.dp)
                            .clip(AppDefaults.PillShape)
                            .clickable {
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("package", request.packageName)
                                )
                                copied = true
                            }
                            .padding(2.dp)
                    )
                }
            }
        }

        // Version + Format cards side by side, same height (the build subtext
        // would otherwise make the Version card taller).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoStatCard(
                label = stringResource(R.string.version),
                value = request.requestedVersionName ?: "Any",
                subtext = request.versionCodeSummary?.let { "build $it" },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            AppInfoFormatCard(
                kinds = request.availableFileKinds.orderedFileKinds(),
                selectedKinds = request.requestedFileKinds,
                onToggle = onFormatToggle,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        AppInfoArchCard(
            abis = request.availableAbis
        )
    }
}

@Composable
internal fun AppInfoStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtext: String? = null
) {
    // The manager's BundleInfoCard: a tinted container holding a small label
    // above a medium-weight value, on the shared warning tone.
    SurfaceCard(
        modifier = modifier,
        cornerRadius = AppDefaults.CompactCornerRadius,
        color = SemanticTone.Warning.container
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = SemanticTone.Warning.content.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = SemanticTone.Warning.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtext?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
internal fun AppInfoFormatCard(
    kinds: List<String>,
    selectedKinds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppDefaults.CompactCornerRadius)
    val selected = kinds.filter { it in selectedKinds }
    val summary = when {
        selected.isEmpty() || selected.size == kinds.size -> "Any"
        selected.size == 1 -> selected.first().uppercase(Locale.US)
        else -> selected.joinToString(" + ") { it.uppercase(Locale.US) }
    }
    var expanded by rememberSaveable(kinds) { mutableStateOf(false) }

    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = sourceCardFill(),
        border = BorderStroke(1.dp, sourceCardBorder())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.format),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = summary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (kinds.size > 1) {
                    // Same count pill + chevron affordance as the Architecture card.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(AppDefaults.PillShape)
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        if (!expanded) {
                            Text(
                                text = "${selected.size}/${kinds.size}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .clip(AppDefaults.PillShape)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Icon(
                            imageVector = if (expanded) {
                                Icons.Outlined.ExpandLess
                            } else {
                                Icons.Outlined.ExpandMore
                            },
                            contentDescription = if (expanded) "Collapse formats" else "Choose formats",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            // Plain conditional (no AnimatedVisibility): this card sits in an
            // equal-height Row(IntrinsicSize.Min), where the expand animation's
            // intrinsic height is measured as collapsed and clips the list.
            if (expanded) {
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                kinds.forEach { kind ->
                    val isSelected = kind in selectedKinds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDefaults.CompactCornerRadius))
                            .clickable { onToggle(kind) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = kind.uppercase(Locale.US),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppInfoArchCard(abis: List<String>) {
    val shape = RoundedCornerShape(AppDefaults.CompactCornerRadius)
    var expanded by rememberSaveable { mutableStateOf(false) }
    val displayAbis = abis.takeIf { it.isNotEmpty() } ?: listOf("Default")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = displayAbis.size > 1) { expanded = !expanded },
        shape = shape,
        color = sourceCardFill(),
        border = BorderStroke(1.dp, sourceCardBorder())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.architecture),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                if (displayAbis.size > 1 && !expanded) {
                    Text(
                        text = "+${displayAbis.size - 1}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .clip(AppDefaults.PillShape)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.ExpandLess
                    } else {
                        Icons.Outlined.ExpandMore
                    },
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            AnimatedExpand(visible = expanded) {
                StatusBadgeRow(modifier = Modifier.fillMaxWidth()) {
                    displayAbis.forEach { abi ->
                        StatusBadge(text = abi)
                    }
                }
            }
            if (!expanded) {
                Text(
                    text = displayAbis.first(),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
