package com.arn.scrobble.pref

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.utils.PlatformStuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.delete_account_custom_servers
import pano_scrobbler.composeapp.generated.resources.lastfm
import pano_scrobbler.composeapp.generated.resources.librefm
import pano_scrobbler.composeapp.generated.resources.listenbrainz

@Composable
fun DeleteAccountScreen(modifier: Modifier = Modifier) {
    val lastfmLink = "last.fm/settings/account/delete"
    val libreLink = "libre.fm/user-delete.php"
    val listenBrainzLink = "listenbrainz.org/profile/delete"
    val textStyle = MaterialTheme.typography.titleMedium

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(Res.string.lastfm),
            style = textStyle,
        )
        OutlinedButton(
            onClick = { PlatformStuff.openInBrowser("https://$lastfmLink") },
        ) {
            Text(text = lastfmLink)
        }

        Text(
            text = stringResource(Res.string.librefm),
            style = textStyle
        )
        OutlinedButton(
            onClick = { PlatformStuff.openInBrowser("https://$libreLink") },
        ) {
            Text(text = libreLink)
        }

        Text(
            text = stringResource(Res.string.listenbrainz),
            style = textStyle
        )
        OutlinedButton(
            onClick = { PlatformStuff.openInBrowser("https://$listenBrainzLink") },
        ) {
            Text(text = listenBrainzLink)
        }

        Text(
            text = stringResource(Res.string.delete_account_custom_servers),
            style = textStyle
        )
    }
}