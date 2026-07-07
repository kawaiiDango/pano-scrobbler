package com.arn.scrobble.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger

@Composable
fun FilePickerSample(modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        OutlinedButton(
            onClick = { show = !show }
        ) {
            Text("Show File Picker")
        }
    }

    FilePicker(
        show = show,
        mode = FilePickerMode.Open(),
        type = FileType.LOG,
        onDismiss = { show = false },
        onFilePicked = { file ->
            Logger.w { "File picked: ${file.uri}" }
//            GlobalScope.launch {
//                file.writeAppend { stream ->
//                    stream.write("Hello, World!".toByteArray())
//                }
//            }
        }
    )
}