package com.arn.scrobble.pref

import androidx.annotation.Keep
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arn.scrobble.R
import com.arn.scrobble.ui.ExtraBottomSpace
import com.arn.scrobble.ui.ScreenParent
import com.arn.scrobble.utils.Stuff

@Preview(showBackground = true)
@Composable
private fun DeleteAccountContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val lastfmLink = "last.fm/settings/account/delete"
    val libreLink = "libre.fm/user-delete.php"
    val listenBrainzLink = "listenbrainz.org/profile/delete"
    val textStyle = MaterialTheme.typography.titleMedium

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(id = R.string.lastfm),
            style = textStyle,
        )
        OutlinedButton(
            onClick = { Stuff.openInBrowser(context, "https://$lastfmLink") },
        ) {
            Text(text = lastfmLink)
        }

        Text(
            text = stringResource(id = R.string.librefm),
            style = textStyle
        )
        OutlinedButton(
            onClick = { Stuff.openInBrowser(context, "https://$libreLink") },
        ) {
            Text(text = libreLink)
        }

        Text(
            text = stringResource(id = R.string.listenbrainz),
            style = textStyle
        )
        OutlinedButton(
            onClick = { Stuff.openInBrowser(context, "https://$listenBrainzLink") },
        ) {
            Text(text = listenBrainzLink)
        }

        Text(
            text = stringResource(id = R.string.delete_account_custom_servers),
            style = textStyle
        )

        ExtraBottomSpace()
    }
}

@Keep
@Composable
fun DeleteAccountScreen() {
    ScreenParent { DeleteAccountContent(it) }
}