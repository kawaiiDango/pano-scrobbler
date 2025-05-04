package com.arn.scrobble.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.ui.stonksIconForDelta
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.legend
import pano_scrobbler.composeapp.generated.resources.rank_change
import pano_scrobbler.composeapp.generated.resources.rank_change_new

@Composable
fun ChartsLegendDialog(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(Res.string.legend),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        arrayOf(
            stringResource(Res.string.rank_change, "> +5") to 6,
            stringResource(Res.string.rank_change, "+1 — +5") to 4,
            stringResource(Res.string.rank_change, "0") to 0,
            stringResource(Res.string.rank_change, "-1 — -5") to -4,
            stringResource(Res.string.rank_change, "< -5") to -6,
            stringResource(Res.string.rank_change_new) to Int.MAX_VALUE,
        ).forEach { (text, value) ->
            stonksIconForDelta(value)?.let { (icon, color) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        tint = color,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

        }
    }
}