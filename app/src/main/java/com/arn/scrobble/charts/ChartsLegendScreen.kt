package com.arn.scrobble.charts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arn.scrobble.R
import com.arn.scrobble.ui.BottomSheetDialogParent

@Preview(showBackground = true)
@Composable
private fun ChartsLegendContent(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.legend),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        LegendTextWithIcon(
            text = stringResource(R.string.rank_change, "> +5"),
            iconRes = R.drawable.vd_stonks_up_double,
            style = MaterialTheme.typography.bodyLarge,
        )

        LegendTextWithIcon(
            text = stringResource(R.string.rank_change, "+1 — +5"),
            iconRes = R.drawable.vd_stonks_up,
            style = MaterialTheme.typography.bodyLarge,
        )

        LegendTextWithIcon(
            text = stringResource(R.string.rank_change, "0"),
            iconRes = R.drawable.vd_stonks_no_change,
            style = MaterialTheme.typography.bodyLarge,
        )

        LegendTextWithIcon(
            text = stringResource(R.string.rank_change, "-1 — -5"),
            iconRes = R.drawable.vd_stonks_down,
            style = MaterialTheme.typography.bodyLarge,
        )

        LegendTextWithIcon(
            text = stringResource(R.string.rank_change, "< -5"),
            iconRes = R.drawable.vd_stonks_down_double,
            style = MaterialTheme.typography.bodyLarge,
        )

        LegendTextWithIcon(
            text = stringResource(R.string.rank_change_new),
            iconRes = R.drawable.vd_stonks_new,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun LegendTextWithIcon(
    text: String,
    iconRes: Int,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = text,
            style = style
        )
    }
}

@Composable
fun ChartsLegendScreen(
    onDismiss: () -> Unit,
) {
    BottomSheetDialogParent(
        onDismiss = onDismiss
    ) { ChartsLegendContent(it) }
}