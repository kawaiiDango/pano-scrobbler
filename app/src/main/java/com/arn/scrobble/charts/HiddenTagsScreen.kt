package com.arn.scrobble.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.ui.DialogParent
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HiddenTagsContent(modifier: Modifier = Modifier) {
    val hiddenTags by PlatformStuff.mainPrefs.data.mapLatest { it.hiddenTags }
        .collectAsStateWithLifecycle(emptySet())
    var tagInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun addTag(tag: String) {
        if (tagInput.isNotBlank()) {
            scope.launch {
                PlatformStuff.mainPrefs.updateData { it.copy(hiddenTags = it.hiddenTags + tag) }
            }
            tagInput = ""
        }
    }

    fun removeTag(tag: String) {
        scope.launch {
            PlatformStuff.mainPrefs.updateData { it.copy(hiddenTags = it.hiddenTags - tag) }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (hiddenTags.isEmpty()) {
            Text(
                stringResource(R.string.user_tags_empty),
                modifier = Modifier.padding(8.dp)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                hiddenTags.forEach { tag ->
                    InputChip(
                        onClick = {
                            removeTag(tag)
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.delete)
                            )

                        },
                        label = {
                            Text(
                                text = tag,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selected = false,
                    )
                }
            }
        }

        OutlinedTextField(
            modifier = Modifier.widthIn(max = 200.dp),
            value = tagInput,
            singleLine = true,
            onValueChange = { tagInput = it },
            label = { Text(stringResource(R.string.tag)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    addTag(tagInput)
                }
            )
        )

        TextButton(
            onClick = {
                addTag(tagInput)
            },
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.End)
        ) {
            Text(stringResource(R.string.add))
        }
    }
}

@Composable
fun HiddenTagsScreen(
    onDismiss: () -> Unit,
) {
    DialogParent(
        onDismiss = onDismiss
    ) {
        HiddenTagsContent(
            modifier = it
        )
    }
}
