package com.arn.scrobble.widget

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arn.scrobble.R
import com.arn.scrobble.navigation.enumSaver
import com.arn.scrobble.pref.SpecificWidgetPrefs
import com.arn.scrobble.ui.LabeledSwitch
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.appwidget_alpha
import pano_scrobbler.composeapp.generated.resources.appwidget_period
import pano_scrobbler.composeapp.generated.resources.appwidget_refresh_every
import pano_scrobbler.composeapp.generated.resources.appwidget_shadow
import pano_scrobbler.composeapp.generated.resources.cancel
import pano_scrobbler.composeapp.generated.resources.num_hours
import pano_scrobbler.composeapp.generated.resources.ok

@Composable
fun ChartsWidgetConfigScreen(
    isPinned: Boolean,
    prefs: SpecificWidgetPrefs,
    onSave: (prefs: SpecificWidgetPrefs, reFetch: Boolean) -> Unit,
    onCancel: () -> Unit,
) {

    var period by rememberSaveable(saver = enumSaver()) { mutableStateOf(prefs.period) }
    var bgAlpha by rememberSaveable { mutableFloatStateOf(prefs.bgAlpha) }
    var shadow by rememberSaveable { mutableStateOf(prefs.shadow) }
    val firstDayOfWeek by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.firstDayOfWeek }
    val scrollState = rememberScrollState()
    val widgetPeriods = remember {
        WidgetPeriods.entries.associateWith {
            it.toTimePeriod(firstDayOfWeek)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
    ) {

        if (shadow) {
            WidgetPreviewWithShadow(
                bgAlpha = bgAlpha,
                modifier = Modifier
                    .size(300.dp, 270.dp)
                    .align(Alignment.CenterHorizontally)
            )
        } else {
            WidgetPreviewWithoutShadow(
                bgAlpha = bgAlpha,
                modifier = Modifier
                    .size(300.dp, 270.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .weight(1f)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.appwidget_period),
                    style = MaterialTheme.typography.titleMedium,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    widgetPeriods.forEach { (thisPeriod, thisTimePeriod) ->
                        FilterChip(
                            label = { Text(thisTimePeriod.name) },
                            selected = period == thisPeriod,
                            onClick = { period = thisPeriod }
                        )
                    }
                }

                Text(
                    text = stringResource(Res.string.appwidget_alpha),
                    style = MaterialTheme.typography.titleMedium
                )

                Slider(
                    value = bgAlpha,
                    onValueChange = { bgAlpha = it },
                    valueRange = 0f..1f,
                    steps = 100,
                )

                LabeledSwitch(
                    text = stringResource(Res.string.appwidget_shadow),
                    checked = shadow,
                    onCheckedChange = { shadow = it },
                    textStyle = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(
                        Res.string.appwidget_refresh_every,
                        pluralStringResource(
                            Res.plurals.num_hours,
                            Stuff.CHARTS_WIDGET_REFRESH_INTERVAL_HOURS,
                            Stuff.CHARTS_WIDGET_REFRESH_INTERVAL_HOURS,
                        )
                    ),
                )
            }
        }

        Surface(
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isPinned) {
                    TextButton(onClick = onCancel) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    onSave(
                        prefs.copy(
                            period = period,
                            bgAlpha = bgAlpha,
                            shadow = shadow
                        ),
                        prefs.period != period
                    )
                }) {
                    Text(text = stringResource(Res.string.ok))
                }
            }

        }
    }
}

@Composable
private fun WidgetPreview(
    shadow: Boolean,
    bgAlpha: Float,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            val layoutRes = if (shadow) {
                R.layout.appwidget_charts_dynamic_shadow
            } else {
                R.layout.appwidget_charts_dynamic
            }
            val layout = LayoutInflater.from(context).inflate(layoutRes, null, false)

            val listView = layout.findViewById<ListView>(R.id.appwidget_list)
            listView.emptyView = layout.findViewById(R.id.appwidget_status)
            listView.adapter = FakeChartsAdapter(layout.context)

            val headerLayout = LayoutInflater.from(context)
                .inflate(R.layout.appwidget_list_header, layout as ViewGroup, false)
            headerLayout.findViewById<TextView>(R.id.appwidget_period).text =
                context.resources.getQuantityString(
                    R.plurals.num_months,
                    1,
                    1
                )

            listView.addHeaderView(headerLayout)

            layout
        },

        update = { layout ->
            val bg = layout.findViewById<ImageView>(R.id.appwidget_bg)

            bg.alpha = bgAlpha
        },
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    )
}

@Composable
fun WidgetPreviewWithShadow(
    bgAlpha: Float,
    modifier: Modifier = Modifier,
) {
    WidgetPreview(
        shadow = true,
        bgAlpha = bgAlpha,
        modifier = modifier
    )
}

@Composable
fun WidgetPreviewWithoutShadow(
    bgAlpha: Float,
    modifier: Modifier = Modifier,
) {
    WidgetPreview(
        shadow = false,
        bgAlpha = bgAlpha,
        modifier = modifier
    )
}