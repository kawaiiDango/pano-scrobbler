package com.arn.scrobble.edits

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.error
import com.arn.scrobble.R
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.ui.PackageName

@Composable
fun AppItemChip(
    appListItem: AppItem,
    onClick: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    InputChip(
        onClick = {
            onClick(appListItem)
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.close)
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
                model = ImageRequest.Builder(LocalContext.current)
                    .data(PackageName(appListItem.appId))
                    .error(R.drawable.vd_apps)
                    .build(),
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
    modifier: Modifier = Modifier
) {
    var deleteMenuShown by remember { mutableStateOf(false) }

    IconButton(
        enabled = enabled,
        modifier = modifier,
        onClick = {
            deleteMenuShown = true
        }) {
        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more))
        DropdownMenu(
            expanded = deleteMenuShown,
            onDismissRequest = { deleteMenuShown = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
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
