package com.arn.scrobble.edits

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arn.scrobble.icons.Apps
import com.arn.scrobble.icons.Close
import com.arn.scrobble.icons.Delete
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.MoreVert
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.ui.placeholderPainter
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.close
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.more

@Composable
fun AppItemChip(
    appListItem: AppItem,
    onClick: (AppItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    InputChip(
        onClick = {
            onClick(appListItem)
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Close,
                contentDescription = stringResource(Res.string.close)
            )

        },
        label = {
            Text(
                text = appListItem.friendlyLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 150.dp)
            )
        },
        avatar = {
            AsyncImage(
                model = PackageName(appListItem.appId),
                error = rememberVectorPainter(Icons.Apps),
                placeholder = placeholderPainter(),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        selected = true,
        modifier = modifier
    )
}

@Composable
fun EditsDeleteMenu(
    onDelete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var deleteMenuShown by remember { mutableStateOf(false) }

    IconButton(
        enabled = enabled,
        modifier = modifier,
        onClick = {
            deleteMenuShown = true
        }) {
        Icon(Icons.MoreVert, contentDescription = stringResource(Res.string.more))
        DropdownMenu(
            expanded = deleteMenuShown,
            onDismissRequest = { deleteMenuShown = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(Res.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Delete,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = null
                    )
                },
                onClick = {
                    onDelete()
                    deleteMenuShown = false
                })
        }
    }
}
