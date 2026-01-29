package com.arn.scrobble.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Add
import com.arn.scrobble.icons.Close
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.ui.PanoOutlinedTextField
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.add
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.tag
import pano_scrobbler.composeapp.generated.resources.user_tags_empty

@Composable
fun HiddenTagsDialog(modifier: Modifier = Modifier) {
    val hiddenTags by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.hiddenTags }
    var tagInput by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun addTag(tag: String) {
        if (tagInput.isNotBlank()) {
            scope.launch {
                PlatformStuff.mainPrefs.updateData { it.copy(hiddenTags = it.hiddenTags + tag) }
                tagInput = ""
            }
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
                stringResource(Res.string.user_tags_empty),
                modifier = Modifier.padding(8.dp)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(8.dp)
                    .heightIn(max = (48 * 4).dp)
                    .verticalScroll(scrollState)
            ) {
                hiddenTags.forEach { tag ->
                    InputChip(
                        onClick = {
                            removeTag(tag)
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Close,
                                contentDescription = stringResource(Res.string.delete)
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

        PanoOutlinedTextField(
            modifier = Modifier.widthIn(max = 200.dp),
            value = tagInput,
            singleLine = true,
            onValueChange = { tagInput = it },
            label = { Text(stringResource(Res.string.tag)) },
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

        OutlinedIconButton(
            onClick = {
                addTag(tagInput)
            },
            modifier = Modifier
                .padding(top = 8.dp)
                .width(100.dp)
                .align(Alignment.End)
        ) {
            Icon(
                Icons.Add,
                stringResource(Res.string.add)
            )
        }
    }
}