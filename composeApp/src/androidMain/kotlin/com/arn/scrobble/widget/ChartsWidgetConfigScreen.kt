package com.arn.scrobble.widget

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.HeaderViewListAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.icons.Check
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.ui.ButtonWithDropdown
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.ui.accountTypeLabel
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album_art
import pano_scrobbler.composeapp.generated.resources.appwidget_alpha
import pano_scrobbler.composeapp.generated.resources.appwidget_period
import pano_scrobbler.composeapp.generated.resources.appwidget_refresh_every
import pano_scrobbler.composeapp.generated.resources.appwidget_shadow
import pano_scrobbler.composeapp.generated.resources.done
import pano_scrobbler.composeapp.generated.resources.num_hours
import pano_scrobbler.composeapp.generated.resources.scrobble_services

@Composable
fun ChartsWidgetConfigScreen(
    prefs: WidgetPrefs.SpecificWidgetPrefs,
    onSave: (prefs: WidgetPrefs.SpecificWidgetPrefs, reFetch: Boolean) -> Unit,
    onCancel: () -> Unit,
) {

    var period by rememberSaveable { mutableStateOf(prefs.period) }
    var bgAlpha by rememberSaveable { mutableFloatStateOf(prefs.bgAlpha) }
    var shadow by rememberSaveable { mutableStateOf(prefs.shadow) }
    var images by rememberSaveable { mutableStateOf(prefs.images) }
    val accountTypesWithUsernames by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { prefs ->
        prefs.scrobbleAccounts
            .distinctBy { it.type }
            .filterNot {
                it.type in arrayOf(
                    AccountType.PLEROMA,
                    AccountType.FILE,
                )
            }
            .associate {
                it.type to it.user.name
            }
    }
    val accountTypesWithLabels = accountTypesWithUsernames.mapValues { (k, v) ->
        accountTypeLabel(k) + ": $v"
    }

    var accountType by rememberSaveable {
        mutableStateOf(
            if (prefs.accountType in accountTypesWithLabels)
                prefs.accountType
            else
                accountTypesWithLabels.keys.firstOrNull() ?: AccountType.LASTFM
        )
    }
    val firstDayOfWeek by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.firstDayOfWeek }
    val scrollState = rememberScrollState()
    val widgetPeriod = remember {
        WidgetPeriod.entries.associateWith {
            it.toTimePeriod(firstDayOfWeek)
        }
    }

    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass

    @Composable
    fun RowOrColumn(
        modifier: Modifier = Modifier,
        widgetPreview: @Composable (Modifier) -> Unit,
        content: @Composable (Modifier) -> Unit,
    ) {
        if (sizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            Row(modifier = modifier) {
                widgetPreview(Modifier.align(Alignment.CenterVertically))
                content(Modifier.weight(1f))
            }
        } else {
            Column(modifier = modifier) {
                widgetPreview(Modifier.align(Alignment.CenterHorizontally))
                content(Modifier.weight(1f))
            }
        }
    }

    RowOrColumn(
        widgetPreview = { modifier ->
            WidgetPreview(
                shadow = shadow,
                bgAlpha = bgAlpha,
                images = images,
                modifier = modifier
                    .size(300.dp, 270.dp)
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
    ) { modifier ->
        Box(
            modifier = modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large
                )
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(top = 16.dp, bottom = 72.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.scrobble_services),
                            style = MaterialTheme.typography.titleMedium,
                        )

                        ButtonWithDropdown(
                            prefixText = null,
                            itemToTexts = accountTypesWithLabels,
                            selected = accountType,
                            onItemSelected = { accountType = it },
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.appwidget_period),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        widgetPeriod.forEach { (thisPeriod, thisTimePeriod) ->
                            FilterChip(
                                label = { Text(thisTimePeriod.name) },
                                selected = period == thisPeriod,
                                shapes = FilterChipDefaults.shapes(),
                                onClick = { period = thisPeriod }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.appwidget_alpha) +
                                    ": ${"%.0f".format(bgAlpha * 100)}%",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Slider(
                            value = bgAlpha,
                            onValueChange = { bgAlpha = it },
                            valueRange = 0f..1f,
                            steps = 100,
                        )
                    }

                    LabeledCheckbox(
                        text = stringResource(Res.string.album_art),
                        checked = images,
                        onCheckedChange = { images = it },
                        isSwitch = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    LabeledCheckbox(
                        text = stringResource(Res.string.appwidget_shadow),
                        checked = shadow,
                        onCheckedChange = { shadow = it },
                        isSwitch = true,
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
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                }

                FloatingActionButton(
                    onClick = {
                        onSave(
                            prefs.copy(
                                accountType = accountType,
                                period = period,
                                images = images,
                                bgAlpha = bgAlpha,
                                shadow = shadow
                            ),
                            prefs.period != period || accountType != prefs.accountType
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Check,
                        contentDescription = stringResource(Res.string.done)
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPreview(
    shadow: Boolean,
    bgAlpha: Float,
    images: Boolean,
    modifier: Modifier = Modifier,
) {
    key(shadow) { // re-inflate the layout when shadow changes
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

                val listView = layout.findViewById<ListView>(R.id.appwidget_list)
                ((listView.adapter as? HeaderViewListAdapter)
                    ?.wrappedAdapter as? FakeChartsAdapter)
                    ?.setShowImages(images)
            },
            onReset = {

            },
            modifier = modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        )
    }
}