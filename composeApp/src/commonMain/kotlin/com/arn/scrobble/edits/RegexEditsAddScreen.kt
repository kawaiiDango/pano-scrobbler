package com.arn.scrobble.edits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.arn.scrobble.db.BlockPlayerAction
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditsDao.Companion.countNamedCaptureGroups
import com.arn.scrobble.db.RegexMode
import com.arn.scrobble.db.mode
import com.arn.scrobble.icons.AlbumArtist
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.pref.AppListSaveType
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.ButtonWithSpinner
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.add
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.album_artist
import pano_scrobbler.composeapp.generated.resources.apps
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.block
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.edit_all
import pano_scrobbler.composeapp.generated.resources.edit_case_sensitive
import pano_scrobbler.composeapp.generated.resources.edit_extract
import pano_scrobbler.composeapp.generated.resources.edit_extract_desc
import pano_scrobbler.composeapp.generated.resources.edit_extract_example
import pano_scrobbler.composeapp.generated.resources.edit_extract_extra_groups
import pano_scrobbler.composeapp.generated.resources.edit_extract_no_groups
import pano_scrobbler.composeapp.generated.resources.edit_first
import pano_scrobbler.composeapp.generated.resources.edit_name
import pano_scrobbler.composeapp.generated.resources.edit_regex
import pano_scrobbler.composeapp.generated.resources.edit_regex_invalid
import pano_scrobbler.composeapp.generated.resources.edit_replace
import pano_scrobbler.composeapp.generated.resources.no_apps_enabled
import pano_scrobbler.composeapp.generated.resources.required_fields_empty
import pano_scrobbler.composeapp.generated.resources.save
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.track
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RegexEditsAddScreen(
    mainViewModel: MainViewModel,
    regexEdit: RegexEdit?,
    onNavigate: (PanoRoute) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(regexEdit?.name ?: "") }
    var appItems by rememberSaveable { mutableStateOf(emptySet<AppItem>()) }
    val dao = remember { PanoDb.db.getRegexEditsDao() }

    var appItemsInited by rememberSaveable { mutableStateOf(false) }

    var regexMode by rememberSaveable {
        mutableStateOf(regexEdit?.mode() ?: RegexMode.Replace)
    }
    var replaceAll by rememberSaveable {
        mutableStateOf(regexEdit?.replacement?.replaceAll ?: false)
    }
    var caseSensitive by rememberSaveable { mutableStateOf(regexEdit?.caseSensitive ?: false) }

    var searchTrack by rememberSaveable { mutableStateOf(regexEdit?.search?.searchTrack ?: "") }
    var searchAlbum by rememberSaveable { mutableStateOf(regexEdit?.search?.searchAlbum ?: "") }
    var searchArtist by rememberSaveable { mutableStateOf(regexEdit?.search?.searchArtist ?: "") }
    var searchAlbumArtist by rememberSaveable {
        mutableStateOf(
            regexEdit?.search?.searchAlbumArtist.orEmpty()
        )
    }

    var replacementTrack by rememberSaveable {
        mutableStateOf(
            regexEdit?.replacement?.replacementTrack.orEmpty()
        )
    }
    var replacementAlbum by rememberSaveable {
        mutableStateOf(
            regexEdit?.replacement?.replacementAlbum.orEmpty()
        )
    }
    var replacementArtist by rememberSaveable {
        mutableStateOf(
            regexEdit?.replacement?.replacementArtist.orEmpty()
        )
    }
    var replacementAlbumArtist by rememberSaveable {
        mutableStateOf(
            regexEdit?.replacement?.replacementAlbumArtist.orEmpty()
        )
    }

    var blockPlayerAction by rememberSaveable {
        mutableStateOf(regexEdit?.blockPlayerAction ?: BlockPlayerAction.ignore)
    }

    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    val isLicenseValid = PlatformStuff.billingRepository.isLicenseValid

    fun buildRegexEdit(): RegexEdit {
        val search = RegexEdit.SearchPatterns(
            searchTrack,
            searchAlbum,
            searchArtist,
            searchAlbumArtist
        )

        return when (regexMode) {
            RegexMode.Extract -> {
                RegexEdit(
                    _id = regexEdit?._id ?: 0,
                    order = regexEdit?.order ?: -1,
                    name = name,
                    search = search,
                    appIds = appItems.map { it.appId }.toSet(),
                    caseSensitive = caseSensitive,
                )
            }

            RegexMode.Replace -> {
                RegexEdit(
                    _id = regexEdit?._id ?: 0,
                    order = regexEdit?.order ?: -1,
                    name = name,
                    search = search,
                    replacement = RegexEdit.ReplacementPatterns(
                        replacementTrack,
                        replacementAlbum,
                        replacementArtist,
                        replacementAlbumArtist,
                        replaceAll
                    ),
                    appIds = appItems.map { it.appId }.toSet(),
                    caseSensitive = caseSensitive,
                )
            }

            RegexMode.Block -> {
                RegexEdit(
                    _id = regexEdit?._id ?: 0,
                    order = regexEdit?.order ?: -1,
                    name = name,
                    search = search,
                    appIds = appItems.map { it.appId }.toSet(),
                    blockPlayerAction = blockPlayerAction,
                    caseSensitive = caseSensitive,
                )
            }

        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.selectedPackages.collectLatest { (checked, _) ->
            appItems = checked.toSet()
        }
    }

    LaunchedEffect(Unit) {
        if (!appItemsInited) {
            appItemsInited = true
            appItems = withContext(Dispatchers.IO) {
                regexEdit?.appIds?.map {
                    AppItem(
                        it,
                        PlatformStuff.loadApplicationLabel(it)
                    )
                }?.toSet() ?: emptySet()
            }
        }
    }
    Column(
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.edit_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            ErrorText(errorText)

            AnimatedVisibility(regexMode == RegexMode.Replace) {
                Column {
                    SearchAndReplacePair(
                        label = stringResource(Res.string.track),
                        searchRegex = searchTrack,
                        replacementRegex = replacementTrack,
                        onSearchChange = { searchTrack = it },
                        onReplacementChange = { replacementTrack = it }
                    )

                    SearchAndReplacePair(
                        label = stringResource(Res.string.artist),
                        searchRegex = searchArtist,
                        replacementRegex = replacementArtist,
                        onSearchChange = { searchArtist = it },
                        onReplacementChange = { replacementArtist = it }
                    )

                    SearchAndReplacePair(
                        label = stringResource(Res.string.album),
                        searchRegex = searchAlbum,
                        replacementRegex = replacementAlbum,
                        onSearchChange = { searchAlbum = it },
                        onReplacementChange = { replacementAlbum = it }
                    )

                    SearchAndReplacePair(
                        label = stringResource(Res.string.album_artist),
                        searchRegex = searchAlbumArtist,
                        replacementRegex = replacementAlbumArtist,
                        onSearchChange = { searchAlbumArtist = it },
                        onReplacementChange = { replacementAlbumArtist = it }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(Res.string.edit_replace),
                            color = MaterialTheme.colorScheme.secondary,
                        )

                        FilterChip(
                            selected = !replaceAll,
                            onClick = { replaceAll = false },
                            label = { Text(stringResource(Res.string.edit_first)) },
                        )

                        FilterChip(
                            selected = replaceAll,
                            onClick = { replaceAll = true },
                            label = { Text(stringResource(Res.string.edit_all)) },
                        )
                    }
                }
            }

            AnimatedVisibility(regexMode == RegexMode.Extract) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchFields(
                        headerText = stringResource(Res.string.search),
                        track = searchTrack,
                        album = searchAlbum,
                        artist = searchArtist,
                        albumArtist = searchAlbumArtist,
                        onValueChange = { track, album, artist, albumArtist ->
                            searchTrack = track
                            searchAlbum = album
                            searchArtist = artist
                            searchAlbumArtist = albumArtist
                        },
                        enabled = if (regexMode == RegexMode.Extract || regexMode == RegexMode.Block)
                            isLicenseValid
                        else
                            true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExtractOptions(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AnimatedVisibility(regexMode == RegexMode.Block) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchFields(
                        headerText = stringResource(Res.string.search),
                        track = searchTrack,
                        album = searchAlbum,
                        artist = searchArtist,
                        albumArtist = searchAlbumArtist,
                        onValueChange = { track, album, artist, albumArtist ->
                            searchTrack = track
                            searchAlbum = album
                            searchArtist = artist
                            searchAlbumArtist = albumArtist
                        },
                        enabled = if (regexMode == RegexMode.Extract || regexMode == RegexMode.Block)
                            isLicenseValid
                        else
                            true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    BlockPlayerActions(
                        blockPlayerAction = blockPlayerAction,
                        onChange = { blockPlayerAction = it },
                        enabled = isLicenseValid,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }


            AppSelector(
                label = stringResource(Res.string.apps) + (
                        if (regexMode != RegexMode.Extract && appItems.isEmpty())
                            ": " + stringResource(Res.string.edit_all)
                        else ""
                        ),
                appItems = appItems,
                onNavigateToAppList = {
                    onNavigate(
                        PanoRoute.AppList(
                            saveType = AppListSaveType.Callback,
                            preSelectedPackages = it,
                            isSingleSelect = false
                        )
                    )
                },
                onAppItemRemoved = {
                    appItems = appItems - it
                }
            )

            LabeledCheckbox(
                checked = caseSensitive,
                onCheckedChange = { caseSensitive = it },
                text = stringResource(Res.string.edit_case_sensitive),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ButtonWithSpinner(
                    prefixText = null,
                    itemToTexts = mapOf(
                        RegexMode.Replace to stringResource(Res.string.edit_replace),
                        RegexMode.Extract to (if (!isLicenseValid) "🔒 " else "") +
                                stringResource(Res.string.edit_extract),
                        RegexMode.Block to (if (!isLicenseValid) "🔒 " else "") +
                                stringResource(Res.string.block)
                    ),
                    selected = regexMode,
                    onItemSelected = { regexMode = it },
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                if (regexEdit != null) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    dao.delete(regexEdit)
                                }
                                onBack()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(Res.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                ButtonWithIcon(
                    onClick = {
                        scope.launch {
                            val re = buildRegexEdit()
                            val validationResult = validate(re)

                            if (validationResult.isFailure) {
                                errorText = validationResult.exceptionOrNull()?.redactedMessage
                            } else if (
                                !isLicenseValid && (re.replacement == null || re.blockPlayerAction != null)
                            ) {
                                onNavigate(PanoRoute.Billing)
                            } else {
                                withContext(Dispatchers.IO) {
                                    dao.insert(listOf(re))
                                }
                                onBack()
                            }
                        }
                    },
                    icon = Icons.Outlined.Check,
                    text = stringResource(Res.string.save),
                )
            }
        }
    }
}

@Composable
private fun SearchAndReplacePair(
    label: String,
    searchRegex: String,
    replacementRegex: String,
    onSearchChange: (String) -> Unit,
    onReplacementChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(top = 16.dp)
        )

        OutlinedTextField(
            value = searchRegex,
            onValueChange = onSearchChange,
            label = { Text(stringResource(Res.string.search)) },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth(),
        )

        OutlinedTextField(
            value = replacementRegex,
            onValueChange = onReplacementChange,
            label = { Text(stringResource(Res.string.edit_replace)) },
            modifier = Modifier
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
        )
    }
}

@Composable
private fun SearchFields(
    headerText: String,
    track: String,
    album: String,
    artist: String,
    albumArtist: String,
    onValueChange: (
        track: String,
        album: String,
        artist: String,
        albumArtist: String,
    ) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val labelPrefix = stringResource(Res.string.edit_regex) + ": "

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            text = headerText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
        )

        OutlinedTextField(
            value = track,
            onValueChange = { onValueChange(it, album, artist, albumArtist) },
            label = { Text(labelPrefix + stringResource(Res.string.track)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            enabled = enabled
        )

        OutlinedTextField(
            value = artist,
            onValueChange = { onValueChange(track, album, it, albumArtist) },
            label = { Text(labelPrefix + stringResource(Res.string.artist)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            enabled = enabled
        )

        OutlinedTextField(
            value = album,
            onValueChange = { onValueChange(track, it, artist, albumArtist) },
            label = { Text(labelPrefix + stringResource(Res.string.album)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            enabled = enabled
        )

        OutlinedTextField(
            value = albumArtist,
            onValueChange = { onValueChange(track, album, artist, it) },
            label = { Text(labelPrefix + stringResource(Res.string.album_artist)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            enabled = enabled
        )
    }
}

@Composable
private fun ExtractOptions(
    modifier: Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(Res.string.edit_extract_desc),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(Res.string.edit_extract_example),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun getIconForField(field: RegexEdit.Field): ImageVector {
    return when (field) {
        RegexEdit.Field.track -> Icons.Outlined.MusicNote
        RegexEdit.Field.album -> Icons.Outlined.Album
        RegexEdit.Field.artist -> Icons.Outlined.Mic
        RegexEdit.Field.albumArtist -> PanoIcons.AlbumArtist
    }
}

@Composable
fun getLabelForField(field: RegexEdit.Field): String {
    return when (field) {
        RegexEdit.Field.track -> stringResource(Res.string.track)
        RegexEdit.Field.album -> stringResource(Res.string.album)
        RegexEdit.Field.artist -> stringResource(Res.string.artist)
        RegexEdit.Field.albumArtist -> stringResource(Res.string.album_artist)
    }
}

@Composable
private fun AppSelector(
    label: String,
    appItems: Iterable<AppItem>,
    onNavigateToAppList: (List<String>) -> Unit,
    onAppItemRemoved: (AppItem) -> Unit,
) {

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        Text(
            label,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.CenterVertically)
        )

        appItems.forEach {
            AppItemChip(
                appListItem = it,
                onClick = { onAppItemRemoved(it) }
            )
        }

        IconButton(
            onClick = { onNavigateToAppList(appItems.map { it.appId }) },
        ) {
            Icon(
                Icons.Outlined.Add,
                stringResource(Res.string.add)
            )
        }
    }
}

private suspend fun areExtractionRulesValid(regexEdit: RegexEdit): Result<Unit> {

    val extractionPatterns = regexEdit.search

    arrayOf(
        extractionPatterns.searchTrack,
        extractionPatterns.searchAlbum,
        extractionPatterns.searchArtist,
        extractionPatterns.searchAlbumArtist,
    ).all {
        try {
            Pattern.compile(it)
            true
        } catch (e: PatternSyntaxException) {
            return Result.failure(
                IllegalArgumentException(
                    getString(
                        Res.string.edit_regex_invalid,
                        e.redactedMessage
                    )
                )
            )
        }
    }

    val (trackGroups, albumGroups, artistGroups, albumArtistGroups) = regexEdit.countNamedCaptureGroups().values.toList()

    val isSuccess =
        trackGroups == 1 && albumGroups <= 1 && artistGroups == 1 && albumArtistGroups <= 1

    if (!isSuccess) {
        val errMsg = when {
            trackGroups == 0 || artistGroups == 0 -> getString(Res.string.edit_extract_no_groups)
            trackGroups > 1 -> getString(
                Res.string.edit_extract_extra_groups,
                RegexEdit.Field.track.name
            )

            albumGroups > 1 -> getString(
                Res.string.edit_extract_extra_groups,
                RegexEdit.Field.album.name
            )

            artistGroups > 1 -> getString(
                Res.string.edit_extract_extra_groups,
                RegexEdit.Field.artist.name
            )

            albumArtistGroups > 1 -> getString(
                Res.string.edit_extract_extra_groups,
                RegexEdit.Field.albumArtist.name
            )

            else -> getString(Res.string.edit_regex_invalid, "Unknown error")
        }

        return Result.failure(IllegalArgumentException(errMsg))
    }

    return Result.success(Unit)
}

private suspend fun validate(regexEdit: RegexEdit): Result<Unit> {
    var errorText: String? = null

    if (regexEdit.name.isBlank()) {
        errorText = getString(Res.string.required_fields_empty)
    } else if (regexEdit.replacement == null) {

        val extractRulesResult = areExtractionRulesValid(regexEdit)

        if (extractRulesResult.isFailure) {
            errorText = getString(
                Res.string.edit_regex_invalid,
                extractRulesResult.exceptionOrNull()?.redactedMessage ?: ""
            )
        } else if (regexEdit.appIds.isEmpty()) {
            errorText = getString(Res.string.no_apps_enabled)
        }
    } else {
        val searchRegexes = arrayOf(
            regexEdit.search.searchTrack,
            regexEdit.search.searchAlbum,
            regexEdit.search.searchArtist,
            regexEdit.search.searchAlbumArtist
        )

        if (searchRegexes.all { it.isEmpty() }) {
            errorText = getString(Res.string.required_fields_empty)
        } else {
            try {
                searchRegexes.forEach {
                    Pattern.compile(it)
                }
            } catch (e: Exception) {
                errorText = getString(Res.string.edit_regex_invalid, e.redactedMessage)
            }
        }
    }

    if (errorText != null) {
        return Result.failure(IllegalArgumentException(errorText))
    }
    return Result.success(Unit)
}