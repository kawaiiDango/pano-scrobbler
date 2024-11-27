package com.arn.scrobble

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.serializableType
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.Stuff

@Composable
fun PlaceholderScreen(
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = panoContentPadding(),
        modifier = modifier
    ) {
        item {
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = {
                        val user = Scrobblables.currentScrobblableUser!!
                        onNavigate(PanoRoute.Random(user))
                    }
                ) {
                    Text("Open Placeholder Artist")
                }
            }
        }

        items(30, key = { it.toString() }) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${it + 1}. Placeholder Placeholder Placeholder Placeholder Placeholder Placeholder Placeholder Placeholder Placeholder",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}