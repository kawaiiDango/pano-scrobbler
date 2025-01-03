package com.arn.scrobble.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.PanoLazyColumn

@Composable
fun PlaceholderScreen(
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    PanoLazyColumn(
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
                    Text("Random")
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