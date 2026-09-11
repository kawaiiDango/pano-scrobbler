package com.arn.scrobble.pref

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.OpenInBrowser
import com.arn.scrobble.ui.myTransparentCheckableItemColors
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        ListItem(
            onClick = {
                PlatformStuff.openInBrowser("https://$lastfmLink")
            },
            colors = ListItemDefaults.myTransparentCheckableItemColors(),
            trailingContent = {
                Icon(
                    imageVector = Icons.OpenInBrowser,
                    contentDescription = null
                )
            },
            supportingContent = {
                Text(
                    text = lastfmLink,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        ) {
            Text(
                text = stringResource(Res.string.lastfm),
                style = textStyle,
            )
        }

        ListItem(
            onClick = {
                PlatformStuff.openInBrowser("https://$libreLink")
            },
            colors = ListItemDefaults.myTransparentCheckableItemColors(),
            trailingContent = {
                Icon(
                    imageVector = Icons.OpenInBrowser,
                    contentDescription = null
                )
            },
            supportingContent = {
                Text(
                    text = libreLink,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        ) {
            Text(
                text = stringResource(Res.string.librefm),
                style = textStyle,
            )
        }

        ListItem(
            onClick = {
                PlatformStuff.openInBrowser("https://$listenBrainzLink")
            },
            colors = ListItemDefaults.myTransparentCheckableItemColors(),
            trailingContent = {
                Icon(
                    imageVector = Icons.OpenInBrowser,
                    contentDescription = null
                )
            },
            supportingContent = {
                Text(
                    text = listenBrainzLink,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        ) {
            Text(
                text = stringResource(Res.string.listenbrainz),
                style = textStyle,
            )
        }

        ListItem(
            colors = ListItemDefaults.myTransparentCheckableItemColors(),
        ) {
            Text(
                text = stringResource(Res.string.delete_account_custom_servers),
            )
        }
    }
}