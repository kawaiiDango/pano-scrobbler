package com.arn.scrobble.edits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.arn.scrobble.db.ExtractionPatterns
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditFields
import com.arn.scrobble.db.RegexEditsDao.Companion.countNamedCaptureGroups
import com.arn.scrobble.icons.AlbumArtist
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.utils.PlatformStuff
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
import pano_scrobbler.composeapp.generated.resources.close
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.edit_all
import pano_scrobbler.composeapp.generated.resources.edit_case_sensitive
import pano_scrobbler.composeapp.generated.resources.edit_continue
import pano_scrobbler.composeapp.generated.resources.edit_extract
import pano_scrobbler.composeapp.generated.resources.edit_extract_desc
import pano_scrobbler.composeapp.generated.resources.edit_extract_example
import pano_scrobbler.composeapp.generated.resources.edit_extract_extra_groups
import pano_scrobbler.composeapp.generated.resources.edit_extract_no_groups
import pano_scrobbler.composeapp.generated.resources.edit_field
import pano_scrobbler.composeapp.generated.resources.edit_first
import pano_scrobbler.composeapp.generated.resources.edit_name
import pano_scrobbler.composeapp.generated.resources.edit_regex
import pano_scrobbler.composeapp.generated.resources.edit_regex_invalid
import pano_scrobbler.composeapp.generated.resources.edit_regex_replacement
import pano_scrobbler.composeapp.generated.resources.edit_replace
import pano_scrobbler.composeapp.generated.resources.no_apps_enabled
import pano_scrobbler.composeapp.generated.resources.required_fields_empty
import pano_scrobbler.composeapp.generated.resources.save
import pano_scrobbler.composeapp.generated.resources.track
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@OptIn(ExperimentalLayoutApi::class)
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

    var replaceAll by rememberSaveable { mutableStateOf(regexEdit?.replaceAll ?: false) }
    var extractMode by rememberSaveable { mutableStateOf(regexEdit?.extractionPatterns != null) }
    var caseSensitive by rememberSaveable { mutableStateOf(regexEdit?.caseSensitive ?: false) }
    var continueMatching by rememberSaveable {
        mutableStateOf(regexEdit?.continueMatching ?: false)
    }

    // for normal mode
    var regexPattern by rememberSaveable { mutableStateOf(regexEdit?.pattern ?: "") }
    var regexReplacement by rememberSaveable { mutableStateOf(regexEdit?.replacement ?: "") }
    val fields =
        rememberSaveable(
            saver = listSaver(
                save = { it.toList() },
                restore = { it.toMutableStateList() }
            )
        ) { mutableStateListOf(*regexEdit?.fields?.toTypedArray() ?: emptyArray()) }

    // for extraction mode
    var extractionTrack by rememberSaveable {
        mutableStateOf(
            regexEdit?.extractionPatterns?.extractionTrack ?: ""
        )
    }
    var extractionAlbum by rememberSaveable {
        mutableStateOf(
            regexEdit?.extractionPatterns?.extractionAlbum ?: ""
        )
    }
    var extractionArtist by rememberSaveable {
        mutableStateOf(
            regexEdit?.extractionPatterns?.extractionArtist ?: ""
        )
    }
    var extractionAlbumArtist by rememberSaveable {
        mutableStateOf(
            regexEdit?.extractionPatterns?.extractionAlbumArtist ?: ""
        )
    }

    var errorText by remember { mutableStateOf<String?>(null) }
    val proStatus = PlatformStuff.billingRepository.isLicenseValid

    fun buildRegexEdit() = if (extractMode) {
        RegexEdit(
            _id = regexEdit?._id ?: 0,
            order = regexEdit?.order ?: -1,
            name = name,
            extractionPatterns = ExtractionPatterns(
                extractionTrack,
                extractionAlbum,
                extractionArtist,
                extractionAlbumArtist
            ),
            packages = appItems.map { it.appId }.toSet(),
            caseSensitive = caseSensitive,
            continueMatching = continueMatching
        )
    } else {
        RegexEdit(
            _id = regexEdit?._id ?: 0,
            order = regexEdit?.order ?: -1,
            name = name,
            pattern = regexPattern,
            replacement = regexReplacement,
            fields = fields.toSet(),
            packages = appItems.map { it.appId }.toSet(),
            replaceAll = replaceAll,
            caseSensitive = caseSensitive,
            continueMatching = continueMatching
        )
    }

    LaunchedEffect(Unit) {
        mainViewModel.selectedPackages.collectLatest {
            appItems = it.toSet()
        }
    }

    LaunchedEffect(Unit) {
        if (!appItemsInited) {
            appItemsInited = true
            appItems = withContext(Dispatchers.IO) {
                regexEdit?.packages?.map {
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

            AnimatedVisibility(!extractMode) {
                ReplacementFields(
                    regexPattern = regexPattern,
                    regexReplacement = regexReplacement,
                    fields = fields.toSet(),
                    onValueChange = { pattern, replacement, _fields ->
                        regexPattern = pattern
                        regexReplacement = replacement
                        fields.clear()
                        fields.addAll(_fields)
                    })
            }

            AnimatedVisibility(extractMode) {
                ExtractionFields(
                    extractionTrack = extractionTrack,
                    extractionAlbum = extractionAlbum,
                    extractionArtist = extractionArtist,
                    extractionAlbumArtist = extractionAlbumArtist,
                    onValueChange = { track, album, artist, albumArtist ->
                        extractionTrack = track
                        extractionAlbum = album
                        extractionArtist = artist
                        extractionAlbumArtist = albumArtist
                    })
            }

            ErrorText(errorText)

            Text(
                stringResource(Res.string.apps) + (
                        if (!extractMode && appItems.isEmpty())
                            ": " + stringResource(Res.string.edit_all)
                        else ""
                        ),
                color = MaterialTheme.colorScheme.secondary
            )

            AppSelector(
                appItems = appItems,
                onNavigateToAppList = {
                    onNavigate(
                        PanoRoute.AppList(
                            preSelectedPackages = it,
                            hasPreSelection = true,
                            isSingleSelect = false
                        )
                    )
                },
                onAppItemRemoved = {
                    appItems = appItems - it
                }
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !replaceAll && !extractMode,
                    onClick = {
                        replaceAll = false
                        extractMode = false
                    },
                    label = {
                        Text(
                            stringResource(Res.string.edit_replace) + ": " + stringResource(
                                Res.string.edit_first
                            )
                        )
                    }
                )
                FilterChip(
                    selected = replaceAll && !extractMode,
                    onClick = {
                        replaceAll = true
                        extractMode = false
                    },
                    label = {
                        Text(
                            stringResource(Res.string.edit_replace) + ": " + stringResource(
                                Res.string.edit_all
                            )
                        )
                    }
                )

                // extract needs java 8
                if (PlatformStuff.isJava8OrGreater) {
                    FilterChip(
                        leadingIcon = {
                            if (!proStatus)
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null
                                )
                        },
                        selected = extractMode,
                        onClick = {
                            if (proStatus)
                                extractMode = true
                            else
                                onNavigate(PanoRoute.Billing)
                        },
                        label = { Text(stringResource(Res.string.edit_extract)) }
                    )
                }
            }

            LabeledCheckbox(
                checked = caseSensitive,
                onCheckedChange = { caseSensitive = it },
                text = stringResource(Res.string.edit_case_sensitive),
                modifier = Modifier.fillMaxWidth()
            )

            LabeledCheckbox(
                checked = continueMatching,
                onCheckedChange = { continueMatching = it },
                text = stringResource(Res.string.edit_continue),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            if (regexEdit != null) {
                OutlinedIconButton(
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
                        contentDescription = stringResource(Res.string.delete)
                    )
                }
            }

            ButtonWithIcon(
                onClick = {
                    scope.launch {
                        val re = buildRegexEdit()
                        val validationResult = validate(re)

                        if (validationResult.isFailure) {
                            errorText = validationResult.exceptionOrNull()?.message
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

@Composable
private fun ExtractionFields(
    extractionTrack: String,
    extractionAlbum: String,
    extractionArtist: String,
    extractionAlbumArtist: String,
    onValueChange: (
        extractionTrack: String,
        extractionAlbum: String,
        extractionArtist: String,
        extractionAlbumArtist: String,
    ) -> Unit,
) {
    val labelPrefix = stringResource(Res.string.edit_regex) + ": "

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = extractionTrack,
            onValueChange = {
                onValueChange(it, extractionAlbum, extractionArtist, extractionAlbumArtist)
            },
            label = { Text(labelPrefix + stringResource(Res.string.track)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
        )

        OutlinedTextField(
            value = extractionAlbum,
            onValueChange = {
                onValueChange(extractionTrack, it, extractionArtist, extractionAlbumArtist)
            },
            label = { Text(labelPrefix + stringResource(Res.string.album)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
        )

        OutlinedTextField(
            value = extractionArtist,
            onValueChange = {
                onValueChange(extractionTrack, extractionAlbum, it, extractionAlbumArtist)
            },
            label = { Text(labelPrefix + stringResource(Res.string.artist)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
        )

        OutlinedTextField(
            value = extractionAlbumArtist,
            onValueChange = {
                onValueChange(extractionTrack, extractionAlbum, extractionArtist, it)
            },
            label = { Text(labelPrefix + stringResource(Res.string.album_artist)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            )
        )

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReplacementFields(
    regexPattern: String,
    regexReplacement: String,
    fields: Set<String>,
    onValueChange: (regexPattern: String, regexReplacement: String, fields: Set<String>) -> Unit,
) {

    var fieldsPopupShown by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = regexPattern,
            onValueChange = {
                onValueChange(it, regexReplacement, fields)
            },
            label = { Text(stringResource(Res.string.edit_regex)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
        )
        OutlinedTextField(
            value = regexReplacement,
            onValueChange = {
                onValueChange(regexPattern, it, fields)
            },
            label = { Text(stringResource(Res.string.edit_regex_replacement)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            )
        )

        Text(
            text = stringResource(Res.string.edit_field),
            color = MaterialTheme.colorScheme.secondary
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fields.forEach {
                InputChip(
                    selected = true,
                    onClick = {
                        onValueChange(regexPattern, regexReplacement, fields - it)
                    },
                    label = { Text(getLabelForField(it)) },
                    leadingIcon = {
                        Icon(
                            imageVector = getIconForField(it),
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(Res.string.close)
                        )
                    },
                )
            }

            if (fields.size < 4) {
                AssistChip(
                    onClick = {
                        fieldsPopupShown = true
                    },
                    label = { Text(stringResource(Res.string.add)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null
                        )
                    }
                )

                DropdownMenu(
                    expanded = fieldsPopupShown,
                    onDismissRequest = { fieldsPopupShown = false }
                ) {
                    val allFields = setOf(
                        RegexEditFields.TRACK,
                        RegexEditFields.ALBUM,
                        RegexEditFields.ARTIST,
                        RegexEditFields.ALBUM_ARTIST
                    )

                    val remainingFields = allFields - fields

                    remainingFields.forEach {
                        DropdownMenuItem(
                            onClick = {
                                onValueChange(regexPattern, regexReplacement, fields + it)
                                fieldsPopupShown = false
                            },
                            text = { Text(getLabelForField(it)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getIconForField(it),
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }

//        Text(
//            text = stringResource(Res.string.edit_regex_warning),
//            modifier = Modifier.fillMaxWidth()
//        )
    }
}

@Composable
fun getIconForField(field: String): ImageVector {
    return when (field) {
        RegexEditFields.TRACK -> Icons.Outlined.MusicNote
        RegexEditFields.ALBUM -> Icons.Outlined.Album
        RegexEditFields.ARTIST -> Icons.Outlined.Mic
        RegexEditFields.ALBUM_ARTIST -> PanoIcons.AlbumArtist
        else -> throw IllegalArgumentException("Unknown field: $field")
    }
}

@Composable
fun getLabelForField(field: String): String {
    return when (field) {
        RegexEditFields.TRACK -> stringResource(Res.string.track)
        RegexEditFields.ALBUM -> stringResource(Res.string.album)
        RegexEditFields.ARTIST -> stringResource(Res.string.artist)
        RegexEditFields.ALBUM_ARTIST -> stringResource(Res.string.album_artist)
        else -> throw IllegalArgumentException("Unknown field: $field")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppSelector(
    appItems: Iterable<AppItem>,
    onNavigateToAppList: (List<String>) -> Unit,
    onAppItemRemoved: (AppItem) -> Unit,
) {

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        appItems.forEach {
            AppItemChip(
                appListItem = it,
                onClick = { onAppItemRemoved(it) }
            )
        }
        AssistChip(
            onClick = { onNavigateToAppList(appItems.map { it.appId }) },
            label = { Text(stringResource(Res.string.add)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null
                )
            }
        )
    }
}

private suspend fun areExtractionRulesValid(regexEdit: RegexEdit): Result<Unit> {

    val extractionPatterns = regexEdit.extractionPatterns
        ?: return Result.failure(
            IllegalArgumentException(getString(Res.string.edit_extract_no_groups))
        )

    arrayOf(
        extractionPatterns.extractionTrack,
        extractionPatterns.extractionAlbum,
        extractionPatterns.extractionArtist,
        extractionPatterns.extractionAlbumArtist,
    ).all {
        try {
            Pattern.compile(it)
            true
        } catch (e: PatternSyntaxException) {
            return Result.failure(
                IllegalArgumentException(
                    getString(
                        Res.string.edit_regex_invalid,
                        e.message ?: ""
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
                RegexEditFields.TRACK
            )

            albumGroups > 1 -> getString(
                Res.string.edit_extract_extra_groups,
                RegexEditFields.ALBUM
            )

            artistGroups > 1 -> getString(
                Res.string.edit_extract_extra_groups,
                RegexEditFields.ARTIST
            )

            albumArtistGroups > 1 -> getString(
                Res.string.edit_extract_extra_groups,
                RegexEditFields.ALBUM_ARTIST
            )

            else -> getString(Res.string.edit_regex_invalid, "Unknown error")
        }

        return Result.failure(IllegalArgumentException(errMsg))
    }

    return Result.success(Unit)
}

private suspend fun validate(regexEdit: RegexEdit): Result<Unit> {
    var errorText: String? = null

    if (regexEdit.name.isNullOrBlank()) {
        errorText = getString(Res.string.required_fields_empty)
    } else if (regexEdit.extractionPatterns != null) {

        val extractRulesResult = areExtractionRulesValid(regexEdit)

        if (extractRulesResult.isFailure) {
            errorText = getString(
                Res.string.edit_regex_invalid,
                extractRulesResult.exceptionOrNull()?.message ?: ""
            )
        } else if (regexEdit.packages.isNullOrEmpty()) {
            errorText = getString(Res.string.no_apps_enabled)
        }
    } else {
        if (regexEdit.pattern.isNullOrEmpty() || regexEdit.fields.isNullOrEmpty()) {
            errorText = getString(Res.string.required_fields_empty)
        } else {
            try {
                Pattern.compile(regexEdit.pattern)
            } catch (e: Exception) {
                errorText = getString(Res.string.edit_regex_invalid, e.message ?: "")
            }
        }
    }

    if (errorText != null) {
        return Result.failure(IllegalArgumentException(errorText))
    }
    return Result.success(Unit)
}