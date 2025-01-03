package com.arn.scrobble.edits

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(Res.string.close)
            )

        },
        label = {
            Text(
                text = appListItem.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        avatar = {
            AsyncImage(
                model = PackageName(appListItem.appId),
                error = rememberVectorPainter(Icons.Outlined.Apps),
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
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var deleteMenuShown by remember { mutableStateOf(false) }

    IconButton(
        enabled = enabled,
        modifier = modifier,
        onClick = {
            deleteMenuShown = true
        }) {
        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(Res.string.more))
        DropdownMenu(
            expanded = deleteMenuShown,
            onDismissRequest = { deleteMenuShown = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.delete)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.DeleteOutline,
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
