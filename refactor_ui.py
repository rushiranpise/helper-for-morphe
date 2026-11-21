import os, re, sys, shutil, time, zipfile
from collections import defaultdict

def _skip_char(t, i):
    n = len(t)
    if t[i+1:i+2] == '\\':
        j = t.find("'", i+2)
        return j+1 if j != -1 and j-i < 12 else i+1
    if t[i+2:i+3] == "'": return i+3
    return i+1

def _skip_string(t, i, regions=None):
    n = len(t); s = i
    def code(i):
        d = 1
        while i < n and d > 0:
            c = t[i]
            if c == '"': i = _skip_string(t, i)
            elif c == "'": i = _skip_char(t, i)
            elif c == '{': d += 1; i += 1
            elif c == '}': d -= 1; i += 1
            else: i += 1
        return i
    if t.startswith('"""', i):
        i += 3
        while i < n:
            if t.startswith('"""', i):
                j = i+3
                while j < n and t[j] == '"': j += 1
                i = j; break
            if t.startswith('${', i): i = code(i+2); continue
            i += 1
    else:
        i += 1
        while i < n:
            c = t[i]
            if c == '\\': i += 2; continue
            if c == '"': i += 1; break
            if c == '\n': break
            if t.startswith('${', i): i = code(i+2); continue
            i += 1
    if regions is not None: regions.append((s, i))
    return i

def scan_lines(t):
    n = len(t); i = 0; depth = 0; regions = []
    line_starts = [0] + [k+1 for k, c in enumerate(t) if c == '\n']
    ls = set(line_starts); dat = {}
    while i < n:
        if i in ls: dat[i] = depth
        c = t[i]
        if t.startswith('//', i):
            j = t.find('\n', i); j = n if j == -1 else j
            regions.append((i, j)); i = j; continue
        if t.startswith('/*', i):
            s = i; d = 1; i += 2
            while i < n and d > 0:
                if t.startswith('/*', i): d += 1; i += 2
                elif t.startswith('*/', i): d -= 1; i += 2
                else: i += 1
            regions.append((s, i)); continue
        if c == '"': i = _skip_string(t, i, regions); continue
        if c == "'": i = _skip_char(t, i); continue
        if c in '({[': depth += 1
        elif c in ')}]': depth -= 1
        i += 1
    inside = set()
    for s, e in regions:
        for k in line_starts:
            if s < k <= e and k < n: inside.add(k)
    return [(dat.get(k), k in inside) for k in line_starts]

def match_close(t, i):
    """t[i] is an opening bracket; return index of its matching close (string/comment aware)."""
    n = len(t); d = 0
    while i < n:
        c = t[i]
        if t.startswith('//', i):
            j = t.find('\n', i); i = n if j == -1 else j; continue
        if t.startswith('/*', i):
            j = t.find('*/', i+2); i = n if j == -1 else j+2; continue
        if c == '"': i = _skip_string(t, i); continue
        if c == "'": i = _skip_char(t, i); continue
        if c in '({[': d += 1
        elif c in ')}]':
            d -= 1
            if d == 0: return i
        i += 1
    return -1

def split_args(s):
    out = []; cur = 0; i = 0; n = len(s)
    while i < n:
        c = s[i]
        if c == '"': i = _skip_string(s, i); continue
        if c == "'": i = _skip_char(s, i); continue
        if c in '({[':
            j = match_close(s, i)
            if j == -1: return None
            i = j+1; continue
        if c == ',':
            out.append(s[cur:i]); cur = i+1
        i += 1
    last = s[cur:]
    if last.strip(): out.append(last)
    return [a.strip() for a in out]

MOD = r'(?:private|internal|public|protected|data|sealed|enum|abstract|open|inline|value|annotation|const|operator|tailrec|suspend|infix|lateinit|fun\s+interface)'
DECL = re.compile(r'^((?:' + MOD + r'\s+)*)(class|interface|object|fun|val|var|typealias)\s+(.*)$', re.S)

def is_prefix(chunk):
    s = re.sub(r'/\*.*?\*/', '', chunk, flags=re.S)
    s = re.sub(r'//[^\n]*', '', s)
    s = re.sub(r'@\w+(?::\w+)?(?:\((?:[^()]|\([^()]*\))*\))?', '', s)
    return s.strip() == ''

def decl_info(chunk):
    s = re.sub(r'^(?:\s*/\*.*?\*/|\s*//[^\n]*|\s*@\w+(?::\w+)?(?:\((?:[^()]|\([^()]*\))*\))?)+', '', chunk, flags=re.S).lstrip()
    m = DECL.match(s)
    if not m: return None
    mods, kind, rest = m.group(1).split(), m.group(2), m.group(3).lstrip()
    ext = False
    if kind in ('fun', 'val', 'var'):
        if rest.startswith('<'):
            d = 0
            for k, c in enumerate(rest):
                if c == '<': d += 1
                elif c == '>':
                    d -= 1
                    if d == 0: rest = rest[k+1:].lstrip(); break
        head = re.split(r'[(:=\n{]| by ', rest, 1)[0].strip()
        head = re.sub(r'<[^<>]*(?:<[^<>]*>[^<>]*)*>', '', head)
        ext = '.' in head
        name = head.split('.')[-1].strip().strip('`')
    else:
        name = re.match(r'\w+', rest).group(0)
    return dict(mods=mods, kind=kind, name=name, ext=ext)

def parse_kt(raw):
    raw = raw.replace('\r\n', '\n')
    info = scan_lines(raw)
    lines = raw.split('\n')
    cands = [i for i, l in enumerate(lines)
             if l.strip() and not info[i][1] and info[i][0] == 0 and l[0] not in ' \t})]']
    chunks = ['\n'.join(lines[a:b]).rstrip('\n') for a, b in zip(cands, cands[1:] + [len(lines)])]
    pkg = None; imports = []; fileann = []; decls = []; pending = ''
    for ch in chunks:
        first = ch.split('\n')[0]
        if first.startswith('package '): pkg = first.split()[1]; continue
        if first.startswith('import '):
            imports += [l.strip() for l in ch.split('\n') if l.startswith('import ')]; continue
        if first.startswith('@file:'): fileann.append(ch); continue
        if is_prefix(ch):
            pending = (pending + '\n' + ch).strip('\n') if pending else ch; continue
        full = (pending + '\n' + ch) if pending else ch; pending = ''
        di = decl_info(full)
        if di is None:
            if decls: decls[-1]['text'] += '\n' + full
            continue
        decls.append(dict(text=full, **di))
    if pending and decls: decls[-1]['text'] += '\n' + pending
    return pkg, imports, fileann, decls

BASE_PKG = 'dev.rushi.apkdownloadhelper'
UI_PKG = BASE_PKG + '.ui'

RENAMES = {
 'MorpheFavourites':'FavouriteApps','MorpheTracking':'TrackingStore','MorpheSprings':'AppSprings',
 'MorpheColors':'AppColors','MorpheBackground':'AppBackground','MorpheReleaseNotes':'ReleaseNotes',
 'MorpheAccentPresets':'AccentPresets','MorpheAccentPickerDialog':'AccentPickerDialog',
 'MorpheDefaults':'AppDefaults','MorpheAnimations':'AppAnimations','morpheDarkColorScheme':'appDarkColorScheme',
 'morpheLightColorScheme':'appLightColorScheme','MorpheTypography':'AppTypography','MorpheDivider':'AppDivider',
 'MorpheSectionTitle':'SectionTitle','MorpheCardHeader':'CardHeader','MorpheExpandableSurface':'ExpandableSurface',
 'MorpheToggleSwitch':'ToggleSwitch','MorpheSelectorOption':'SelectorOption','MorpheSelectorRow':'SelectorRow',
 'MorphePillButton':'PillButton','MorpheEmptyState':'EmptyState','MorpheFilterChip':'AppFilterChip',
 'MorpheStatusBadge':'StatusBadge','MorpheStatusBadgeRow':'StatusBadgeRow','MorpheDialogButton':'DialogButton',
 'MorpheDialogContentPadding':'DialogContentPadding','MorpheDialogProperties':'AppDialogProperties',
 'MorpheDialog':'AppDialog','MorpheFab':'AnimatedFab','MorphePushedScreen':'PushedScreen',
 'MorpheDialogSurface':'DialogSurface','MorpheWavyProgressRing':'WavyProgressRing','MorpheWordmark':'AppWordmark',
 'MorpheNavigationBar':'AppNavigationBar','MorphePinnedHeaderPage':'PinnedHeaderPage',
 'MorpheSubpageScaffold':'SubpageScaffold','MorpheHeaderBar':'HeaderBar','MorpheScrollbar':'ScrollbarCore',
 'AppBrowserRow':'AppListCard','AppDetailView':'AppDetailScreen','HelperBoltButton':'BoltButton',
 'HelperIconButton':'IconActionButton','HelperHeaderIconButton':'HeaderIconButton','HelperThemeButton':'ThemeToggleButton',
}

DEFAULT = {
 'AppBrowserScreen.kt':'screen/browser/AppBrowserScreen.kt',
 'AppTracking.kt':'model/TrackedApps.kt',
 'CaptchaBrowserScreen.kt':'screen/captcha/CaptchaBrowserScreen.kt',
 'DownloadStatusCards.kt':'component/download/DownloadCards.kt',
 'GlassUtils.kt':'theme/GlassStyle.kt',
 'HelperComponents.kt':'component/source/SourceComponents.kt',
 'HelperHomeScreen.kt':'screen/helper/HelperScreen.kt',
 'HelperModels.kt':'model/HelperModels.kt',
 'HomePagerScreen.kt':'screen/home/HomePagerScreen.kt',
 'MainPage.kt':'screen/home/MainPage.kt',
 'MainPagerScaffold.kt':'screen/home/MainPagerScaffold.kt',
 'MorpheBackground.kt':'component/layout/AppBackground.kt',
 'MorpheChangelog.kt':'component/release/ReleaseNotes.kt',
 'MorpheColorPicker.kt':'component/color/AccentSwatches.kt',
 'MorpheConnectedShapes.kt':'component/shape/ConnectedShapes.kt',
 'MorpheEdgeFade.kt':'component/scroll/EdgeFade.kt',
 'MorpheListScrollbar.kt':'component/scroll/ListScrollbar.kt',
 'MorpheNavigationBar.kt':'component/navigation/AppNavigationBar.kt',
 'MorpheOptionTiles.kt':'component/selection/OptionTiles.kt',
 'MorphePageScaffold.kt':'component/layout/PageScaffold.kt',
 'MorpheSettingsComponents.kt':'component/settings/SettingsRows.kt',
 'MorpheShimmer.kt':'component/loading/Shimmer.kt',
 'MorpheUi.kt':'component/card/SurfaceCards.kt',
 'MorpheWavyProgress.kt':'component/progress/WavyProgress.kt',
 'MorpheWordmark.kt':'component/layout/AppWordmark.kt',
 'PagerStateExt.kt':'util/PagerStateExt.kt',
 'ScrollToTopButton.kt':'component/button/ScrollToTopButton.kt',
 'SettingsPage.kt':'screen/settings/SettingsPage.kt',
 'SettingsScreen.kt':'screen/settings/SettingsScreen.kt',
 'UpdateScreen.kt':'screen/update/UpdateScreen.kt',
}

def _o(path, *names):
    return {n: path for n in names}
OVR = {}
for p, ns in [
 ('screen/browser/AppBrowserFilters.kt', 'AppListTab AppSort'),
 ('model/FavouriteApps.kt', 'MorpheFavourites'),
 ('component/appcard/FindAppsStatsStrip.kt', 'FindAppsStatsStrip'),
 ('component/banner/AppDisclaimerBanner.kt', 'AppDisclaimerBanner'),
 ('component/tracking/TrackedPackageCards.kt', 'PendingTrackedPackagesCard TrackPackageField'),
 ('component/chip/CompactFilterChip.kt', 'CompactFilterChip'),
 ('component/button/GlassHeaderButton.kt', 'GlassHeaderButton'),
 ('component/appcard/AppListCard.kt', 'AppBrowserRow TrackingChangeBadge'),
 ('component/appcard/AppCardStyle.kt', 'FindAppPlaceholderGradients FindAppCardColors FindAppFallbackColors findAppCardColors'),
 ('component/appcard/AppCardLoading.kt', 'FindAppCardLoading'),
 ('component/appcard/AppIconLoader.kt', 'installedAppIconCache rememberInstalledAppIcon ARCHIVE_ICON_CACHE_BYTES ARCHIVE_ICON_MAX_PX archiveIconCache decodeArchiveIcon PUBLIC_METADATA_ICON_BASE publicMetadataIconUrl failedPublicMetadataLookups IconFetchResult fetchIcon loadArchiveIcon'),
 ('component/appcard/AppIcon.kt', 'FindAppIconPlaceholder FindAppIcon AsyncAvatar AppAvatar'),
 ('util/ExternalActions.kt', 'openAddSource openVirusTotalPage handleShizukuAction openExternal'),
 ('component/version/VersionStrip.kt', 'ExpandableVersionRow VersionPillChip VersionExpandToggleButton AppVersionStrip'),
 ('screen/browser/AppDetailScreen.kt', 'DetailHeaderCollapse foldsWith AppDetailView'),
 ('component/appcard/AppSourceCard.kt', 'AppSourceCard SourceMetaItem'),
 ('screen/browser/SourcePatchesScreen.kt', 'SourcePatchesScreen'),
 ('model/TrackingStore.kt', 'MorpheTracking'),
 ('screen/captcha/CaptchaScripts.kt', 'CaptchaCaptureBridge CAPTCHA_BRIDGE_NAME CAPTCHA_CAPTURE_JS BROWSER_CHALLENGE_TIMEOUT_MS BROWSER_CHALLENGE_PROBE_JS reportsBrowserChallenge CAPTCHA_AUTOPRESS_JS'),
 ('screen/captcha/StoreBrowserScripts.kt', 'apkMirrorBrowserScript apkPureBrowserScript'),
 ('screen/captcha/BrowserUrlRules.kt', 'urlDecoded looksLikeApkDownload isUptodownFileUrl browserCookieHeader isApkMirrorFileUrl isApkPureFileUrl isCapturableDownload isBrowserBuildMismatchUrl browserBuildMismatchMessage isApkCubeBrowserFlow isApkCubeHostedUrl isApkCubeAllowedNavigation'),
 ('component/download/CandidateCard.kt', 'CandidateCard CandidateInfoChips CandidateMatchBox'),
 ('component/download/CheckingPickedFileState.kt', 'CheckingPickedFileState'),
 ('component/download/FastModeCard.kt', 'FastModeCard'),
 ('component/download/DownloadStatusRules.kt', 'isRateLimitWait isScanStatus scanPhaseSplit'),
 ('component/button/SkipWaitButton.kt', 'SkipWaitButton'),
 ('component/download/DownloadingState.kt', 'DownloadingState DownloadProgressGauge TransferStats transferStatLines'),
 ('util/Formatters.kt', 'formatTransferSpeed formatTransferEta formatScanDate formatBytes'),
 ('component/download/ScanCards.kt', 'ScanAskCard ScanResultCard ScanMetaRows scanResultDetail'),
 ('component/state/ErrorState.kt', 'FailureActions ErrorState'),
 ('theme/AppMotion.kt', 'MorpheSprings PressedScale pressScale'),
 ('theme/AppColors.kt', 'MorpheColors'),
 ('component/button/ActionButtons.kt', 'HelperBoltButton HelperIconButton HelperHeaderIconButton HelperThemeButton'),
 ('component/dialog/ReuseOfferDialog.kt', 'ReuseOption ReuseOfferDialog'),
 ('component/appinfo/AppInfoCards.kt', 'AppInfoCard AppInfoHeader AppInfoStatCard AppInfoFormatCard AppInfoArchCard'),
 ('component/layout/AnimatedExpand.kt', 'AnimatedExpand'),
 ('component/dialog/SwitchCandidateDialog.kt', 'SwitchCandidateDialog'),
 ('component/state/LoadingState.kt', 'LoadingState'),
 ('component/source/SourcePickerFlow.kt', 'SourcePickerFlow'),
 ('component/source/SourcePickerLogic.kt', 'PrimaryAction buildPrimaryAction subTabsFor defaultSubTab SourceSubTab supportsRecommended supportsHistory'),
 ('component/source/SourceCards.kt', 'SourceGrid SourceMenuItem sourceCardFill sourceCardBorder SourceCard SourceBrand brand SourceAvatar'),
 ('component/selection/RadioDot.kt', 'RadioDot'),
 ('component/source/SourceBars.kt', 'SelectedSourceBar SourceBottomBar'),
 ('component/button/SourceSquareButton.kt', 'SourceSquareButton'),
 ('component/source/SourcePageContent.kt', 'SourcePageContent VersionTypeRow'),
 ('component/source/SourceSettingsTabs.kt', 'SettingsTab SettingsTabRow'),
 ('component/source/AboutModeCard.kt', 'AboutModeCard'),
 ('component/version/VersionHistory.kt', 'VersionHistorySection VersionHistoryRow'),
 ('component/source/CandidateResolveSection.kt', 'CandidateResolveSection'),
 ('component/card/InfoCard.kt', 'InfoCard'),
 ('component/history/DownloadHistorySection.kt', 'HistoryFilter DownloadHistorySection'),
 ('component/history/RequestLogsCard.kt', 'RequestLogsCard'),
 ('component/history/HistoryEntryCard.kt', 'HistoryEntryCard'),
 ('component/state/EmptyState.kt', 'EmptyLaunchState MorpheEmptyState'),
 ('component/card/HelperSetupCard.kt', 'HelperSetupCard'),
 ('model/HelperRequest.kt', 'HelperRequest'),
 ('model/ResolveModels.kt', 'FailedAttempt CandidateResult SourceCandidateGroup ResolveOutcome definitiveNotFoundReason ResolveState VersionHistoryState'),
 ('model/StoreResponses.kt', 'ApkMirrorLatestInfo ApkMirrorVariant UptodownVersionResponse UptodownVersionEntry UptodownVersionUrl UptodownVariantResponse UptodownVariantFile'),
 ('model/DownloadCandidate.kt', 'DownloadCandidate matchingSourceSwitchCandidates sourceSwitchVersionMatches sourceSwitchVariantMatches normalizedVariantLabel CandidateMatchSummary matchSummary identityKey'),
 ('model/DownloadFiles.kt', 'CandidateDownloadFile BrowserDownloadCapture GENERIC_BROWSER_DOWNLOAD_NAMES capturedDownloadFileName DownloadedApkMetadata'),
 ('model/DownloadSource.kt', 'DownloadSource searchDomain'),
 ('model/CandidateOptions.kt', 'CandidateOption labelForLogs VersionStatus'),
 ('theme/LogLevelColors.kt', 'color'),
 ('model/UiState.kt', 'UiState FastModeChoice FastModeFindResult SwitchCandidatePrompt FastModeProgress'),
 ('model/DownloadHelperContract.kt', 'DownloadHelperContract'),
 ('component/release/ReleaseNotesParser.kt', 'NoteGroup HeadingPattern BulletPattern ImagePattern HtmlTagPattern changelogIcon isFullChangelogFooter parseReleaseNotes cleanReleaseNotes'),
 ('component/color/HsvPicker.kt', 'HsvColor legibleOn SaturationValuePanel HueSlider drawMarker toHsv HueStops'),
 ('component/dialog/AccentPickerDialog.kt', 'MorpheAccentPickerDialog'),
 ('component/shape/PressShapes.kt', 'PillGroupShapes rememberPressProgress rememberPressShape pressed ScaledCornerSize animateCornersAsState AnimatedCorners LerpCornerSize'),
 ('component/button/ButtonGroup.kt', 'ButtonGroupRow buttonGroupItem ButtonGroupItemElement ButtonGroupItemNode widenPressed'),
 ('component/settings/SettingsSearch.kt', 'SettingsSearch LocalSettingsSearch'),
 ('component/text/Footnote.kt', 'Footnote'),
 ('theme/AppDefaults.kt', 'MorpheDefaults'),
 ('theme/AppAnimations.kt', 'MorpheAnimations animatedListItem'),
 ('theme/ColorSchemes.kt', 'morpheDarkColorScheme morpheLightColorScheme monochromeDarkColorScheme monochromeLightColorScheme LocalMonochromeTheme MonochromeThemeDefaults toAccentColorOrNull toHexString withCustomAccent withPureBlack adjustLightness contrastingForeground'),
 ('theme/AppTypography.kt', 'MorpheTypography'),
 ('theme/SemanticTone.kt', 'SemanticTone'),
 ('component/icon/ThemedIcons.kt', 'ThemedIcon GradientCircleIcon ForwardChevronIcon'),
 ('component/card/InfoCards.kt', 'InfoStatBox HeroInfoCard InfoBox'),
 ('component/settings/SettingsItems.kt', 'SettingsGroup SettingsItemCard SettingsItem'),
 ('component/layout/AppDivider.kt', 'MorpheDivider'),
 ('component/layout/IconTextRow.kt', 'IconTextRow'),
 ('component/text/SectionHeaders.kt', 'MorpheSectionTitle MorpheCardHeader'),
 ('component/card/ExpandableSurface.kt', 'MorpheExpandableSurface'),
 ('component/selection/ToggleSwitch.kt', 'MorpheToggleSwitch'),
 ('component/text/FittedText.kt', 'fittedTextAutoSize'),
 ('component/selection/SelectorRow.kt', 'MorpheSelectorOption MorpheSelectorRow'),
 ('component/button/PillButton.kt', 'MorphePillButton'),
 ('component/chip/StatusBadges.kt', 'BadgeDefaults MorpheStatusBadge MorpheStatusBadgeRow'),
 ('component/chip/AppFilterChip.kt', 'MorpheFilterChip'),
 ('component/button/DialogButton.kt', 'MorpheDialogButton isDarkColor'),
 ('component/dialog/AppDialog.kt', 'LocalDialogTextColor MorpheDialogContentPadding MorpheDialogProperties MorpheDialog MorpheDialogSurface'),
 ('component/button/AnimatedFab.kt', 'MorpheFab'),
 ('component/layout/PushedScreen.kt', 'MorphePushedScreen'),
 ('screen/settings/SettingsCategory.kt', 'SettingsCategory'),
 ('screen/settings/AppearanceSettings.kt', 'AppearanceTabContent'),
 ('screen/settings/CustomizationSettings.kt', 'CustomizationTabContent'),
 ('screen/settings/SystemSettings.kt', 'SystemTabContent'),
 ('screen/settings/AdvancedSettings.kt', 'AdvancedTabContent'),
 ('screen/settings/LogsSettings.kt', 'LogsSubTab LogsTabContent'),
 ('screen/settings/AboutSettings.kt', 'AboutTabContent'),
 ('screen/settings/SourcesSettings.kt', 'SourcesSettingsScreen'),
 ('component/dialog/SettingsChoiceDialog.kt', 'SettingsChoice SettingsChoiceDialog'),
 ('screen/settings/SettingsIcons.kt', 'icon'),
 ('component/settings/SettingsOptionCard.kt', 'SettingsOptionCard'),
 ('component/settings/VirusTotalSettings.kt', 'ApiKeyEditor VirusTotalQuotaCard QuotaBar'),
 ('component/dialog/UpdateDialogs.kt', 'ShizukuPackageDialog UpdatePromptDialog'),
 ('theme/ComponentTokens.kt', 'ScrollbarWidth ScrollbarTrackWidth ScrollbarVerticalPadding ScrollbarMinThumbHeight ScrollbarMinThumbFraction ScrollbarIdleTimeout EdgeFadeWidth SegmentGap OuterCorner InnerCorner PillOuter PillInner PressedCornerScale PressedWidthRatio ConnectedSpaceBetween SelectedTileBorder WordmarkAspectRatio MarkerRadius MarkerStroke HueStripHeight'),
]:
    OVR.update(_o(p, *ns.split()))
OVR.update(_o('component/layout/PageScaffold.kt', 'PageInsets', 'MorphePinnedHeaderPage', 'MorpheSubpageScaffold', 'MorpheHeaderBar', 'HeaderBand'))
OVR.update(_o('component/layout/AppWordmark.kt', 'MorpheWordmark'))
OVR.update(_o('component/progress/WavyProgress.kt', 'MorpheWavyProgressRing', 'drawWavyArc'))
OVR.update(_o('component/release/ReleaseNotes.kt', 'MorpheReleaseNotes', 'ReleaseNoteGroup', 'inlineNote'))
OVR.update(_o('component/layout/AppBackground.kt', 'MorpheBackground'))
OVR.update(_o('component/navigation/AppNavigationBar.kt', 'MorpheNavigationBar'))
OVR.update(_o('screen/update/UpdateScreen.kt', 'UpdateScreen'))
OVR.update(_o('component/card/SurfaceCards.kt', 'SurfaceCard', 'SectionCard'))
OVR.update(_o('component/selection/OptionTiles.kt', 'ThemeSelectorCard', 'ThemeOption', 'ThemeOptionRow', 'OptionGroup', 'IconOptionCard', 'SelectionTile'))
OVR.update(_o('component/scroll/ListScrollbar.kt', 'LazyListScrollbar', 'ScrollStateScrollbar', 'MorpheScrollbar', 'ScrollbarMetrics', 'ScrollbarSizeCache', 'seekTarget', 'scrollbarMetrics'))
OVR.update(_o('component/color/AccentSwatches.kt', 'AccentSwatchGrid', 'AccentSwatch', 'AccentGridSection', 'MorpheAccentPresets'))

NEW_FILES = {}   # path -> (imports, body)

NEW_FILES['theme/ComponentStyles.kt'] = ([
 'androidx.compose.foundation.shape.CircleShape','androidx.compose.material3.MaterialTheme',
 'androidx.compose.runtime.Composable','androidx.compose.ui.graphics.Color','androidx.compose.ui.graphics.Shape',
 'androidx.compose.ui.graphics.luminance','androidx.compose.ui.unit.dp'], '''/**
 * Central style tokens for the shared components. Change a colour, shape or size here and every
 * screen that uses the component (home, settings, updates, dialogs...) follows.
 */
internal object SearchFieldStyle {
    val Height = 40.dp
    val HorizontalPadding = 14.dp
    val IconSize = 20.dp
    val Shape: Shape = CircleShape

    @Composable
    fun containerColor(): Color {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.05f
        return MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.6f)
    }

    @Composable
    fun placeholderColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
}

/** Colours for the trailing value shown on the right side of a settings row. */
internal object SettingsValueStyle {
    @Composable
    fun color(): Color = MaterialTheme.colorScheme.primary

    @Composable
    fun destructiveColor(): Color = MaterialTheme.colorScheme.error
}

/** Look shared by every alert dialog (update, storage, picker, settings choice...). */
internal object AlertDialogStyle {
    @Composable
    fun containerColor(): Color = MaterialTheme.colorScheme.background
}
''')

NEW_FILES['component/input/SearchField.kt'] = ([
 'androidx.compose.animation.animateColorAsState','androidx.compose.animation.core.tween',
 'androidx.compose.foundation.clickable','androidx.compose.foundation.layout.Arrangement',
 'androidx.compose.foundation.layout.Box','androidx.compose.foundation.layout.Row',
 'androidx.compose.foundation.layout.fillMaxWidth','androidx.compose.foundation.layout.height',
 'androidx.compose.foundation.layout.padding','androidx.compose.foundation.layout.size',
 'androidx.compose.foundation.shape.CircleShape','androidx.compose.foundation.text.BasicTextField',
 'androidx.compose.material.icons.Icons','androidx.compose.material.icons.outlined.Close',
 'androidx.compose.material.icons.outlined.Search','androidx.compose.material3.Icon',
 'androidx.compose.material3.MaterialTheme','androidx.compose.material3.Surface','androidx.compose.material3.Text',
 'androidx.compose.runtime.Composable','androidx.compose.runtime.getValue','androidx.compose.ui.Alignment',
 'androidx.compose.ui.Modifier','androidx.compose.ui.draw.clip','androidx.compose.ui.focus.FocusRequester',
 'androidx.compose.ui.focus.focusRequester','androidx.compose.ui.graphics.SolidColor',
 'androidx.compose.ui.text.style.TextOverflow'], '''/** Pill shaped search input used by every list screen. Style tokens live in [SearchFieldStyle]. */
@Composable
internal fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val containerColor by animateColorAsState(
        targetValue = SearchFieldStyle.containerColor(),
        animationSpec = tween(200),
        label = "search_field_container"
    )
    Surface(
        modifier = modifier.height(SearchFieldStyle.Height),
        shape = SearchFieldStyle.Shape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SearchFieldStyle.HorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(SearchFieldStyle.IconSize)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SearchFieldStyle.placeholderColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(SearchFieldStyle.IconSize)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}
''')

NEW_FILES['component/settings/SettingsValueText.kt'] = ([
 'androidx.compose.material3.MaterialTheme','androidx.compose.material3.Text','androidx.compose.runtime.Composable',
 'androidx.compose.ui.Modifier','androidx.compose.ui.graphics.Color'], '''/** Trailing value of a settings row ("Stable", "Default", "Clear"...). Colour: [SettingsValueStyle]. */
@Composable
internal fun SettingsValueText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SettingsValueStyle.color()
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.bodyMedium
    )
}
''')

NEW_FILES['component/dialog/AppAlertDialog.kt'] = ([
 'androidx.compose.material3.AlertDialog','androidx.compose.runtime.Composable','androidx.compose.ui.Modifier',
 'androidx.compose.ui.graphics.Color','androidx.compose.ui.graphics.Shape','androidx.compose.material3.MaterialTheme',
 'androidx.compose.ui.window.DialogProperties'], '''/**
 * The one alert dialog used app-wide. Same parameters as Material's AlertDialog, but the colour
 * and shape come from [AlertDialogStyle], so update, settings and helper dialogs always match.
 */
@Composable
internal fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = AlertDialogStyle.containerColor(),
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = containerColor,
        properties = properties
    )
}
''')

def replace_code_only(text, rx, fn):
    """apply regex substitution outside string literals (but inside ${...} templates)."""
    out = []; i = 0; n = len(text); last = 0
    segs = []  # (start,end,is_string)
    pos = 0
    while pos < n:
        c = text[pos]
        if text.startswith('//', pos):
            j = text.find('\n', pos); j = n if j == -1 else j
            pos = j; continue
        if c == '"':
            e = _skip_string(text, pos)
            segs.append((pos, e)); pos = e; continue
        if c == "'":
            pos = _skip_char(text, pos); continue
        pos += 1
    res = []; cur = 0
    for s, e in segs:
        res.append(rx.sub(fn, text[cur:s]))
        lit = text[s:e]
        lit2 = re.sub(r'\$\{[^}]*\}|\$[A-Za-z_]\w*', lambda m: rx.sub(fn, m.group(0)), lit)
        res.append(lit2); cur = e
    res.append(rx.sub(fn, text[cur:]))
    return ''.join(res)

def apply_renames(text, rx):
    return replace_code_only(text, rx, lambda m: RENAMES[m.group(1)])

def fix_search_blocks(text):
    pat = re.compile(r'^([ \t]*)val isDark = (?:MaterialTheme\.colorScheme|colors)\.background\.luminance\(\) < 0\.05f\n', re.M)
    count = 0
    while True:
        m = pat.search(text)
        if not m: break
        indent = m.group(1)
        s = text.find('Surface(', m.end())
        po = s + len('Surface')
        pc = match_close(text, po)
        k = pc + 1
        while text[k] in ' \n\t': k += 1
        if text[k] != '{': raise RuntimeError('search block shape not recognised')
        bc = match_close(text, k)
        block = text[m.start():bc+1]
        q = re.search(r'value = (\w+),', block).group(1)
        ph = re.search(r'Text\(\s*text = (.+?),\n\s+style', block, re.S).group(1).strip()
        fr = re.search(r'\.focusRequester\((\w+)\)', block)
        call = (f'{indent}SearchField(\n{indent}    query = {q},\n{indent}    onQueryChange = {{ {q} = it }},\n'
                f'{indent}    placeholder = {ph},\n')
        if fr: call += f'{indent}    focusRequester = {fr.group(1)},\n'
        call += f'{indent}    modifier = Modifier.weight(1f)\n{indent})'
        text = text[:m.start()] + call + text[bc+1:]
        count += 1
    return text, count

def fix_value_text(text):
    count = 0; pos = 0; out = []
    key = 'trailingContent = {'
    while True:
        i = text.find(key, pos)
        if i == -1: out.append(text[pos:]); break
        ob = i + len(key) - 1
        cb = match_close(text, ob)
        body = text[ob+1:cb].strip()
        new = None
        if body.startswith('Text(') and match_close(body, 4) == len(body) - 1:
            args = split_args(body[5:-1])
            if args:
                txt = None; color = None; style = None; ok = True
                for k, a in enumerate(args):
                    nm = re.match(r'(\w+)\s*=\s*(.*)$', a, re.S)
                    if nm and nm.group(1) in ('text', 'color', 'style'):
                        v = nm.group(2).strip()
                        if nm.group(1) == 'text': txt = v
                        elif nm.group(1) == 'color': color = v
                        else: style = v
                    elif k == 0 and not nm: txt = a
                    else: ok = False
                if ok and txt and style in (None, 'MaterialTheme.typography.bodyMedium') \
                   and color in ('MaterialTheme.colorScheme.primary', 'MaterialTheme.colorScheme.error', None):
                    if color is None and style is None: ok = False
                    if ok:
                        txt = re.sub(r'\s*\n\s*', ' ', txt)
                        extra = ', color = SettingsValueStyle.destructiveColor()' if color and color.endswith('error') else ''
                        new = f'trailingContent = {{ SettingsValueText({txt}{extra}) }}'
        if new:
            out.append(text[pos:i]); out.append(new); pos = cb + 1; count += 1
        else:
            out.append(text[pos:ob+1]); pos = ob + 1
    return ''.join(out), count

def fix_alert_dialogs(text):
    if not re.search(r'\bAlertDialog\(', text): return text, 0
    count = 0; pos = 0; out = []
    for m in re.finditer(r'(?<![\w.])AlertDialog\(', text):
        pass
    res = []; cur = 0
    for m in list(re.finditer(r'(?<![\w.])AlertDialog\(', text)):
        po = m.end() - 1; pc = match_close(text, po)
        call = text[m.start():pc+1]
        call = call.replace('AlertDialog(', 'AppAlertDialog(', 1)
        call = re.sub(r'\n[ \t]*containerColor = MaterialTheme\.colorScheme\.background,', '', call)
        res.append(text[cur:m.start()]); res.append(call); cur = pc + 1; count += 1
    res.append(text[cur:])
    return ''.join(res), count

SKIP_DIRS = {'build', '.git', '.gradle', '.idea', 'node_modules', '.refactor_backup'}
OPERATOR_IMPORTS = {'getValue','setValue','provideDelegate','plus','minus','times','div','rem','unaryMinus','unaryPlus',
                    'not','inc','dec','rangeTo','rangeUntil','contains','get','set','invoke','compareTo','iterator',
                    'hasNext','next','component1','component2','component3','component4','component5','equals'}

def pkg_of(path):
    d = os.path.dirname(path)
    return UI_PKG + ('.' + d.replace('/', '.') if d else '')

def strip_comments(t):
    t = re.sub(r'/\*.*?\*/', '', t, flags=re.S)
    return re.sub(r'(?m)^[ \t]*//.*$', '', t)

def use_regex(d):
    n = re.escape(d['name'])
    if d['name'][0].islower() or d['name'][0] == '_':
        if d['ext']: return re.compile(r'(?<=\.)' + n + r'\b')
        return re.compile(r'(?<![\w.])' + n + r'\b(?!\s*=(?!=))')
    return re.compile(r'(?<![\w])' + n + r'\b')

def make_internal(text):
    pre = re.match(r'^(?:\s*/\*.*?\*/|\s*//[^\n]*|\s*@\w+(?::\w+)?(?:\((?:[^()]|\([^()]*\))*\))?)*', text, flags=re.S).group(0)
    rest = text[len(pre):]
    return pre + re.sub(r'\bprivate\b', 'internal', rest, count=1)

def main():
    root = os.getcwd()
    ui_dir = None
    for dp, dn, fn in os.walk(root):
        dn[:] = [d for d in dn if d not in SKIP_DIRS]
        if dp.replace('\\', '/').endswith('dev/rushi/apkdownloadhelper/ui'): ui_dir = dp; break
    if not ui_dir: sys.exit('ERROR: run this from the project root (could not find .../dev/rushi/apkdownloadhelper/ui)')
    if any(os.path.isdir(os.path.join(ui_dir, d)) for d in os.listdir(ui_dir)):
        sys.exit('ERROR: ui/ already has sub-folders - looks like the refactor was already applied.')
    src_files = sorted(f for f in os.listdir(ui_dir) if f.endswith('.kt'))
    print(f'ui folder : {ui_dir}\nsource files: {len(src_files)}')

    stamp = time.strftime('%Y%m%d-%H%M%S')
    all_kt = []
    for dp, dn, fn in os.walk(root):
        dn[:] = [d for d in dn if d not in SKIP_DIRS]
        all_kt += [os.path.join(dp, f) for f in fn if f.endswith('.kt')]
    bk = os.path.join(root, f'ui_refactor_backup_{stamp}.zip')
    with zipfile.ZipFile(bk, 'w', zipfile.ZIP_DEFLATED) as z:
        for f in all_kt: z.write(f, os.path.relpath(f, root))
    print('backup    :', os.path.basename(bk))

    rename_rx = re.compile(r'\b(' + '|'.join(sorted(map(re.escape, RENAMES), key=len, reverse=True)) + r')\b')

    records = []; imports_by_src = {}; fileann_by_src = {}
    for sf in src_files:
        raw = open(os.path.join(ui_dir, sf), encoding='utf-8-sig').read()
        pkg, imps, fann, decls = parse_kt(raw)
        imports_by_src[sf] = imps; fileann_by_src[sf] = fann
        for d in decls:
            d['src'] = sf; d['orig'] = d['name']
            records.append(d)
    print('declarations:', len(records))

    unmapped = []
    for d in records:
        t = OVR.get(d['orig']) or DEFAULT.get(d['src'])
        if d['src'] != 'GlassUtils.kt' and d['orig'] in ('PressedScale',) : pass
        d['target'] = t
    stats = defaultdict(int)
    for d in records:
        txt = d['text']
        if d['src'] == 'AppBrowserScreen.kt' and d['orig'] in ('AppBrowserScreen', 'SourcePatchesScreen'):
            txt, c = fix_search_blocks(txt); stats['search fields replaced'] += c
        if d['src'] in ('SettingsScreen.kt', 'UpdateScreen.kt'):
            txt, c = fix_value_text(txt); stats['settings value rows unified'] += c
        txt, c = fix_alert_dialogs(txt); stats['alert dialogs unified'] += c
        txt = apply_renames(txt, rename_rx)
        d['text'] = txt
        d['name'] = RENAMES.get(d['orig'], d['orig'])
    new_decl_names = []
    by_target = defaultdict(list)
    for d in records: by_target[d['target']].append(d)
    for d in records: d['use_rx'] = use_regex(d)
    def signature(txt):
        s = re.sub(r'^(?:\s*/\*.*?\*/|\s*//[^\n]*|\s*@\w+(?::\w+)?(?:\((?:[^()]|\([^()]*\))*\))?)+', '', txt, flags=re.S)
        pd = 0; i = 0
        while i < len(s):
            c = s[i]
            if c == '"': i = _skip_string(s, i); continue
            if c in '([': pd += 1
            elif c in ')]': pd -= 1
            elif pd == 0 and c in '{=': return s[:i]
            i += 1
        return s
    stripped = {id(d): strip_comments(d['text']) for d in records}
    sigs = {id(d): signature(d['text']) for d in records}
    made_internal = 0
    for p in records:
        if 'private' not in p['mods']: continue
        need = False
        for q in records:
            if q is p: continue
            if q['target'] != p['target'] and p['use_rx'].search(stripped[id(q)]): need = True; break
            if q['target'] == p['target'] and p['kind'] in ('class','interface','object','typealias') \
               and 'private' not in q['mods'] and p['use_rx'].search(sigs[id(q)]): need = True; break
        if need:
            p['text'] = make_internal(p['text'])
            p['mods'] = ['internal' if m == 'private' else m for m in p['mods']]
            made_internal += 1
    stats['private -> internal (cross-file)'] = made_internal

    new_records = []
    for path, (imps, body) in NEW_FILES.items():
        for chunk in re.split(r'\n(?=(?:/\*\*|@Composable|internal ))', body.rstrip() + '\n'):
            pass
        pk, ims, fa, ds = parse_kt('package x\n\n' + body)
        for d in ds:
            d['src'] = '<new>'; d['orig'] = d['name']; d['target'] = path; d['new_imports'] = imps
            d['use_rx'] = use_regex(d); new_records.append(d)
    records += new_records
    for d in new_records:
        stripped[id(d)] = strip_comments(d['text'])

    index = defaultdict(list)
    for d in records:
        if 'private' in d['mods']: continue
        if d['target'] not in index[d['name']]: index[d['name']].append(d['target'])
    seen = defaultdict(set)
    for d in records:
        key = (d['target'], d['name'], d['kind'], d['ext'])
        if d['kind'] in ('class','interface','object','typealias') or d['kind'] != 'fun':
            if key in seen and d['name'] not in ('icon','color'):
                print('WARNING duplicate name in', d['target'], d['name'])
            seen[key].add(1)

    groups = defaultdict(list)
    for d in records: groups[d['target']].append(d)
    pkg_names = sorted({pkg_of(t) for t in groups})
    rewritten_imports = {}
    def map_ui_import(line):
        m = re.match(r'^import ' + re.escape(UI_PKG) + r'\.(\w+)(.*)$', line)
        if not m: return line
        nm = RENAMES.get(m.group(1), m.group(1)); tg = index.get(nm)
        if not tg: return line
        return f'import {pkg_of(tg[0])}.{nm}{m.group(2)}'
    out_files = {}
    warn = []
    tokens_re = re.compile(r'[A-Za-z_]\w*')
    for target, ds in groups.items():
        pkg = pkg_of(target)
        body_text = '\n\n'.join(d['text'].rstrip() for d in ds)
        body_clean = strip_comments(body_text)
        imps = []
        for d in ds:
            if d['src'] == '<new>':
                imps += [f'import {i}' for i in d['new_imports']]
            else:
                imps += imports_by_src[d['src']]
        anns = []
        for d in ds:
            if d['src'] != '<new>':
                for a in fileann_by_src[d['src']]:
                    if a not in anns: anns.append(a)
        final = []
        for line in dict.fromkeys(imps):
            line = map_ui_import(apply_renames(line, rename_rx))
            if re.match(r'^import ' + re.escape(UI_PKG) + r'\.\*$', line): continue
            m = re.match(r'^import ([\w.]+)(?:\.\*|(?: as (\w+))?)$', line)
            if line.endswith('.*'): final.append(line); continue
            simple = (m.group(2) or m.group(1).split('.')[-1]) if m else None
            if simple in ('getValue','setValue','provideDelegate'):
                if re.search(r'\bby\b', body_clean): final.append(line)
                continue
            if simple and (simple in OPERATOR_IMPORTS or re.search(r'(?<![\w])' + re.escape(simple) + r'\b', body_clean)):
                final.append(line)
        have = {}
        for l in final:
            m = re.match(r'^import ([\w.]+)(?: as (\w+))?$', l)
            if m: have[m.group(2) or m.group(1).split('.')[-1]] = m.group(1)
        local_names = {d['name'] for d in ds}
        for nm, tgs in sorted(index.items()):
            if nm in local_names: continue
            tg = [t for t in tgs if t != target]
            if not tg: continue
            if pkg_of(tg[0]) == pkg: continue
            dd = next(x for x in records if x['name'] == nm and 'private' not in x['mods'])
            if not dd['use_rx'].search(body_clean): continue
            fq = f'{pkg_of(tg[0])}.{nm}'
            if nm in have and have[nm] != fq:
                warn.append(f'{target}: "{nm}" clashes with import {have[nm]} - kept the existing import (check manually)')
                continue
            if nm not in have:
                final.append(f'import {fq}'); have[nm] = fq
        final = sorted(set(final))
        head = '\n'.join(anns) + ('\n\n' if anns else '')
        out_files[target] = f'{head}package {pkg}\n\n' + '\n'.join(final) + '\n\n' + body_text.rstrip() + '\n'

    for sf in src_files: os.remove(os.path.join(ui_dir, sf))
    for target, content in out_files.items():
        p = os.path.join(ui_dir, *target.split('/'))
        os.makedirs(os.path.dirname(p), exist_ok=True)
        open(p, 'w', encoding='utf-8', newline='\n').write(content)

    wildcard_new = ''.join(f'import {p}.*\n' for p in pkg_names)
    changed = 0
    for f in all_kt:
        if os.path.dirname(os.path.abspath(f)) == os.path.abspath(ui_dir) or not os.path.exists(f): continue
        raw = open(f, encoding='utf-8-sig').read(); crlf = '\r\n' in raw
        t = raw.replace('\r\n', '\n'); o = t
        t = apply_renames(t, rename_rx)
        t = re.sub(r'(?m)^import ' + re.escape(UI_PKG) + r'\.\*\n', lambda m: wildcard_new, t)
        t = '\n'.join(map_ui_import(l) if l.startswith('import ') else l for l in t.split('\n'))
        def fq(m):
            nm = m.group(1); tg = index.get(nm)
            return f'{pkg_of(tg[0])}.{nm}' if tg else m.group(0)
        t = re.sub(re.escape(UI_PKG) + r'\.(\w+)', fq, t)
        if t != o:
            open(f, 'w', encoding='utf-8', newline='\r\n' if crlf else '\n').write(t); changed += 1
    print(f'other project files updated: {changed}')

    problems = 0
    for target, content in out_files.items():
        body = strip_comments(re.sub(r'"""[\s\S]*?"""', '""', content))
        body = re.sub(r'"(?:\\.|[^"\\\n])*"', '""', body)
        if body.count('{') != body.count('}') or body.count('(') != body.count(')'):
            print('UNBALANCED:', target); problems += 1
    print('\n== result ==')
    for k, v in stats.items(): print(f'{k}: {v}')
    print('new files:', len(out_files))
    for w in warn: print('WARNING', w)
    print('structural problems:', problems)
    print('\nNew layout:')
    last = None
    for t in sorted(out_files):
        d = os.path.dirname(t)
        if d != last: print('  ui/' + d + '/'); last = d
        print('      ' + os.path.basename(t))
    print('\nNow: Android Studio -> File > Sync Project, then Build > Rebuild Project.')
    print('Undo any time by unzipping', os.path.basename(bk), 'in the project root (after deleting ui/ sub-folders).')

main()