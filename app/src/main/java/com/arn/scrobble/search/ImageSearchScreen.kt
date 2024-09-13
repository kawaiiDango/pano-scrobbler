package com.arn.scrobble.search

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.compose.LocalFragment
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.spotify.AlbumItem
import com.arn.scrobble.api.spotify.ArtistItem
import com.arn.scrobble.api.spotify.TrackItem
import com.arn.scrobble.main.App
import com.arn.scrobble.ui.AlertDialogOk
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.ExtraBottomSpace
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.ScreenParent
import com.arn.scrobble.ui.SearchBox
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.getData
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch


@Composable
private fun ImageSearchContent(
    viewModel: ImageSearchVM = viewModel(),
    musicEntry: MusicEntry,
    originalMusicEntry: MusicEntry? = null,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {

    val printableEntryName = if (musicEntry is Album) {
        stringResource(R.string.artist_title, musicEntry.artist!!.name, musicEntry.name)
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

    var searchTerm by remember {
        mutableStateOf(
            if (musicEntry is Album)
                musicEntry.artist!!.name + " " + musicEntry.name
            else
                musicEntry.name
        )
    }

    val existingMappings by viewModel.existingMappings.collectAsStateWithLifecycle()
    val squarePhotoLearnt by PlatformStuff.mainPrefs.data.map { it.squarePhotoLearnt }
        .collectAsStateWithLifecycle(false)
    var showSquarePhotoDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.setImage(uri)
        onDone()
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.setImage(uri)
        onDone()
    }

    fun launchImagePickerCompat() {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable())
            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        else {
            // the gms image picker fails on takePersistableUriPermission()
            // so do this until google fixes it
            documentPickerLauncher.launch(arrayOf("image/*"))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setMusicEntries(musicEntry, originalMusicEntry)
    }

    LaunchedEffect(searchTerm) {
        viewModel.search(searchTerm)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        SearchBox(
            searchTerm = searchTerm,
            label = printableEntryName,
            onSearchTermChange = { searchTerm = it },
        )

        EmptyText(
            text = stringResource(R.string.not_found),
            visible = searchResults?.isEmpty() == true,
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {

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
                            Text(stringResource(R.string.reset))
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (!squarePhotoLearnt) {
                                showSquarePhotoDialog = true
                            } else {
                                launchImagePickerCompat()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.from_gallery))
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
                        onTrackClick = {
                            viewModel.insertCustomMappings(it, null)
                            onDone()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (searchResults?.isEmpty() == true) {
                item {
                    EmptyText(
                        text = stringResource(R.string.not_found),
                        visible = true,
                    )
                }
            } else {
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
                        onTrackClick = {},
                        modifier = Modifier.shimmer()
                    )
                }
            }

            item("extraBottomSpace") {
                ExtraBottomSpace()
            }
        }

        if (searchResults == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .shimmer()
            ) {
                items(10) {
                    MusicEntryListItem(
                        Track(
                            name = "",
                            artist = Artist(""),
                            album = Album(""),
                        ),
                        forShimmer = true,
                        onTrackClick = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showSquarePhotoDialog)
        AlertDialogOk(
            text = stringResource(R.string.square_photo_hint),
            onDismissRequest = {
                showSquarePhotoDialog = false
            },
            onConfirmation = {
                scope.launch {
                    PlatformStuff.mainPrefs.updateData { it.copy(squarePhotoLearnt = true) }
                }
                launchImagePickerCompat()
                showSquarePhotoDialog = false
            }
        )
}

@Keep
@Composable
fun ImageSearchScreen() {
    val fragment = LocalFragment.current

    val musicEntry = fragment.requireArguments().getData<MusicEntry>()!!
    val originalMusicEntry = fragment.requireArguments().getData<MusicEntry>(Stuff.ARG_ORIGINAL)

    ScreenParent {
        ImageSearchContent(
            onDone = {
                (PlatformStuff.application as? App)?.clearMusicEntryImageCache(musicEntry)
                fragment.findNavController().popBackStack()
            },
            musicEntry = musicEntry,
            originalMusicEntry = originalMusicEntry,
            modifier = it
        )
    }
}