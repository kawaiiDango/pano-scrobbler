package com.arn.scrobble.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.themes.AppPreviewTheme


@Preview(showBackground = true)
@Composable
private fun ExpandableHeaderItemPreview() {
    ExpandableHeaderItem(
        icon = Icons.Outlined.Info,
        title = "Title",
        expanded = false,
        onToggle = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ExpandableHeaderMenuPreview() {
    ExpandableHeaderMenu(
        icon = Icons.Outlined.Info,
        title = "Title",
        menuItemText = "Menu",
        onMenuItemClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun GoToDetailsHeaderItemPreview() {
    GoToDetailsHeaderItem(
        icon = Icons.Outlined.Info,
        title = "Title",
        onClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun RecentsListItemPreview() {
    AppPreviewTheme {
        MusicEntryListItem(
            entry = Track(
                name = "Track Name",
                artist = Artist(name = "Artist Name"),
                album = Album(name = "Album Name"),
                userloved = true,
            ),
            imageUrlOverride = "",
            onMenuClick = {},
            onEntryClick = {},
//            showDateSeperator = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MusicEntryGridItemPreview() {
    AppPreviewTheme {
        MusicEntryGridItem(
            entry = Track(
                name = "Track Name",
                artist = Artist(name = "Artist Name"),
                album = Album(name = "Album Name"),
                userloved = true,
            ),
            showArtist = true,
            imageUrlOverride = "",
            stonksDelta = null,
            progress = 0.5f,
            index = 1,
            onClick = {},
        )
    }
}