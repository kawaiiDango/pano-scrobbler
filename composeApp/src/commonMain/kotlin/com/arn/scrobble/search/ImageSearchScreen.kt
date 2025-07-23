package com.arn.scrobble.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.spotify.AlbumItem
import com.arn.scrobble.api.spotify.ArtistItem
import com.arn.scrobble.api.spotify.TrackItem
import com.arn.scrobble.imageloader.clearMusicEntryImageCache
import com.arn.scrobble.ui.AlertDialogOk
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.FilePicker
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.FileType
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SearchField
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.from_gallery
import pano_scrobbler.composeapp.generated.resources.not_found
import pano_scrobbler.composeapp.generated.resources.reset
import pano_scrobbler.composeapp.generated.resources.square_photo_hint


@Composable
fun ImageSearchScreen(
    artist: Artist?,
    originalArtist: Artist?,
    album: Album?,
    originalAlbum: Album?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageSearchVM = viewModel { ImageSearchVM() },
) {
    val musicEntry = artist ?: album!!
    val originalMusicEntry = originalArtist ?: originalAlbum

    fun onDone() {
        clearMusicEntryImageCache(entry = musicEntry)
        originalMusicEntry?.let {
            clearMusicEntryImageCache(entry = it)
        }
        onBack()
    }

    val printableEntryName = if (musicEntry is Album) {
        Stuff.formatBigHyphen(musicEntry.artist!!.name, musicEntry.name)
    } else {
        musicEntry.name
    }

    val searchResults by viewModel.searchResults
        .mapLatest {
            it ?: return@mapLatest null

            (it.artists?.items ?: it.albums?.items ?: emptyList())
                .filter { item ->
                    (item is AlbumItem && !item.images.isNullOrEmpty()) ||
                            (item is ArtistItem && !item.images.isNullOrEmpty())
                }
        }
        .collectAsStateWithLifecycle(null)

    val searchError by viewModel.searchError.collectAsStateWithLifecycle()

    var searchTerm by rememberSaveable {
        mutableStateOf(
            if (musicEntry is Album)
                musicEntry.artist!!.name + " " + musicEntry.name
            else
                musicEntry.name
        )
    }

    val existingMappings by viewModel.existingMappings.collectAsStateWithLifecycle()
    val squarePhotoLearnt by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.squarePhotoLearnt }
    var showSquarePhotoDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var filePickerShown by remember { mutableStateOf(false) }

    LaunchedEffect(musicEntry, originalMusicEntry) {
        viewModel.setMusicEntries(musicEntry, originalMusicEntry)
    }

    LaunchedEffect(searchTerm) {
        viewModel.search(searchTerm)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        SearchField(
            searchTerm = searchTerm,
            label = printableEntryName,
            onSearchTermChange = { searchTerm = it },
            modifier = Modifier.padding(panoContentPadding(bottom = false))
        )

        EmptyText(
            text = stringResource(Res.string.not_found),
            visible = searchResults?.isEmpty() == true,
        )

        ErrorText(
            errorText = searchError?.redactedMessage,
            modifier = Modifier.padding(panoContentPadding(bottom = false)),
        )

        PanoLazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            item("buttons") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    if (existingMappings.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteExistingMappings()
                                onDone()
                            }
                        ) {
                            Text(stringResource(Res.string.reset))
                        }
                    }

                    if (!PlatformStuff.isTv) {
                        OutlinedButton(
                            onClick = {
                                if (!squarePhotoLearnt) {
                                    showSquarePhotoDialog = true
                                } else {
                                    filePickerShown = true
                                }
                            }
                        ) {
                            Text(stringResource(Res.string.from_gallery))
                        }
                    }
                }
            }

            if (searchResults?.isNotEmpty() == true) {
                items(
                    searchResults!!,
                    key = { it.id }
                ) {
                    MusicEntryListItem(
                        when (it) {
                            is AlbumItem ->
                                Album(
                                    name = it.name,
                                    artist = Artist(it.artists.joinToString { it.name }),
                                )

                            is ArtistItem ->
                                Artist(it.name)

                            is TrackItem ->
                                Track(
                                    name = it.name,
                                    artist = Artist(it.artists.joinToString { it.name }),
                                    album = Album(it.album.name),
                                )
                        },
                        // items have been filtered to have images
                        imageUrlOverride = when (it) {
                            is AlbumItem -> it.mediumImageUrl
                            is ArtistItem -> it.mediumImageUrl
                            else -> ""
                        },
                        forShimmer = false,
                        onEntryClick = {
                            viewModel.insertCustomMappings(it, null)
                            onDone()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (searchResults?.isEmpty() == true) {
                item {
                    EmptyText(
                        text = stringResource(Res.string.not_found),
                        visible = true,
                    )
                }
            } else if (searchError == null) {
                items(
                    10,
                    key = { it }
                ) {
                    MusicEntryListItem(
                        Track(
                            name = "",
                            artist = Artist(""),
                            album = Album(""),
                        ),
                        forShimmer = true,
                        onEntryClick = {},
                        modifier = Modifier.shimmerWindowBounds()
                    )
                }
            }
        }
    }

    if (showSquarePhotoDialog)
        AlertDialogOk(
            text = stringResource(Res.string.square_photo_hint),
            onDismissRequest = {
                showSquarePhotoDialog = false
            },
            onConfirmation = {
                scope.launch {
                    PlatformStuff.mainPrefs.updateData { it.copy(squarePhotoLearnt = true) }
                }
                filePickerShown = true
                showSquarePhotoDialog = false
            }
        )

    FilePicker(
        show = filePickerShown,
        mode = FilePickerMode.Open(),
        type = FileType.PHOTO,
        onDismiss = { filePickerShown = false },
    ) { platformFile ->
        viewModel.setImage(platformFile)
        onDone()
    }
}