package com.arn.scrobble.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.OutlinedToggleButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.ResultEventBus
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toDuration
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.icons.ArrowDropDown
import com.arn.scrobble.icons.CalendarToday
import com.arn.scrobble.icons.CalendarViewMonth
import com.arn.scrobble.icons.CalendarViewWeek
import com.arn.scrobble.icons.Circle
import com.arn.scrobble.icons.DateRange
import com.arn.scrobble.icons.ExpandCircleDownFilled
import com.arn.scrobble.icons.ExpandCircleRightFilledAutoMirrored
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Refresh
import com.arn.scrobble.navigation.DatePickerResult
import com.arn.scrobble.navigation.DateRangePickerResult
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.TimePeriodClickedResult
import com.arn.scrobble.navigation.TimePeriodDataResult
import com.arn.scrobble.navigation.TimePeriodTypeClickedResult
import com.arn.scrobble.navigation.jsonSerializableSaver
import com.arn.scrobble.ui.PanoDropdownMenu
import com.arn.scrobble.ui.myColors
import com.arn.scrobble.ui.rememberClippedPainter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.Stuff.timeToLocal
import com.arn.scrobble.utils.Stuff.timeToUTC
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.charts_continuous
import pano_scrobbler.composeapp.generated.resources.charts_custom
import pano_scrobbler.composeapp.generated.resources.item_options
import pano_scrobbler.composeapp.generated.resources.listenbrainz
import pano_scrobbler.composeapp.generated.resources.months
import pano_scrobbler.composeapp.generated.resources.num_months
import pano_scrobbler.composeapp.generated.resources.num_weeks
import pano_scrobbler.composeapp.generated.resources.num_years
import pano_scrobbler.composeapp.generated.resources.reload
import pano_scrobbler.composeapp.generated.resources.weeks
import pano_scrobbler.composeapp.generated.resources.years
import java.util.Calendar

private val TimePeriodType.stringRes
    get() = when (this) {
        TimePeriodType.CONTINUOUS -> Res.string.charts_continuous
        TimePeriodType.CUSTOM -> Res.string.charts_custom
        TimePeriodType.WEEK -> Res.string.weeks
        TimePeriodType.MONTH -> Res.string.months
        TimePeriodType.YEAR -> Res.string.years
        TimePeriodType.LISTENBRAINZ -> Res.string.listenbrainz
        else -> throw IllegalArgumentException("Unknown period type: $this")
    }

private val periodTypeMenuItems by lazy {
    mapOf(
        TimePeriodType.WEEK to Icons.CalendarViewWeek,
        TimePeriodType.MONTH to Icons.CalendarViewMonth,
        TimePeriodType.YEAR to Icons.CalendarToday,
        TimePeriodType.CONTINUOUS to Icons.Circle,
        TimePeriodType.CUSTOM to Icons.DateRange,
    )
}

fun getPeriodTypeIcon(periodType: TimePeriodType): ImageVector {
    return periodTypeMenuItems[periodType] ?: Icons.Circle
}

private suspend fun LazyListState.scrollToCenterItem(index: Int, animate: Boolean) {
    val info = layoutInfo
    val viewportSize = info.viewportEndOffset - info.viewportStartOffset
    val estimatedItemSize = info.visibleItemsInfo.let { visible ->
        when {
            visible.isEmpty() -> 0
            index < visible.first().index -> visible.first().size
            index > visible.last().index -> visible.last().size
            index in visible.first().index..visible.last().index -> visible[index - visible.first().index].size
            else -> 0
        }
    }
    val scrollOffset = (estimatedItemSize - viewportSize) / 2

    if (animate) {
        animateScrollToItem(index, scrollOffset)
    } else {
        scrollToItem(index, scrollOffset)
    }
}

fun getPeriodTypePluralRes(periodType: TimePeriodType): PluralStringResource {
    return when (periodType) {
        TimePeriodType.WEEK -> Res.plurals.num_weeks
        TimePeriodType.MONTH -> Res.plurals.num_months
        TimePeriodType.YEAR -> Res.plurals.num_years
        else -> throw IllegalArgumentException("Unknown period type: $periodType")
    }
}

@Composable
fun TimePeriodSelector(
    registeredTime: Long,
    viewModel: ChartsPeriodVM,
    onNavigate: (PanoRoute) -> Unit,
    onSelected: (timePeriod: TimePeriod, prevTimePeriod: TimePeriod?, Int) -> Unit,
    showRefreshButton: Boolean,
    enabled: Boolean = true,
    digestTimePeriod: LastfmPeriod? = null,
) {
    val timePeriods by viewModel.timePeriods.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val refreshCount by viewModel.refreshCount.collectAsStateWithLifecycle()
    val periodTypeToRegisteredTime by viewModel.periodTypeToRegisteredTime.collectAsStateWithLifecycle()
    var dropdownTypeShown by rememberSaveable(saver = jsonSerializableSaver<TimePeriodType?>()) {
        mutableStateOf(null)
    }
    val accountType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }
    var typeSelectorShown by remember { mutableStateOf<Boolean?>(false) }
    var selectedPeriodOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current
    val resultEventBus = LocalResultEventBus.current

    LaunchedEffect(accountType, digestTimePeriod) {
        when (accountType) {
            AccountType.LISTENBRAINZ -> {
                viewModel.setPeriodTypeAndRegisteredTime(
                    TimePeriodType.LISTENBRAINZ,
                    registeredTime
                )
                if (digestTimePeriod == null) {
                    val selected = PlatformStuff.mainPrefs.data.map {
                        it.lastChartsListenBrainzPeriod
                    }.first()
                    viewModel.setSelectedPeriod(selected)
                }

                typeSelectorShown = null
            }

            AccountType.LASTFM if digestTimePeriod == null -> {
                val (type, selected, custom) = PlatformStuff.mainPrefs.data.map {
                    Triple(
                        it.lastChartsPeriodType,
                        it.lastChartsLastfmPeriod,
                        it.lastChartsCustomPeriod
                    )
                }.first()
                viewModel.setPeriodTypeAndRegisteredTime(type, registeredTime)
                viewModel.setSelectedPeriod(selected)
                viewModel.setCustomPeriodInput(custom)
                typeSelectorShown = false
            }

            else -> {
                viewModel.setPeriodTypeAndRegisteredTime(
                    TimePeriodType.CONTINUOUS,
                    registeredTime
                )
                typeSelectorShown = false
            }
        }

        if (digestTimePeriod != null) {
            viewModel.setDigestPeriod(digestTimePeriod)
        }
    }

    LaunchedEffect(selectedPeriod, refreshCount) {
        selectedPeriod?.let { selectedPeriod ->
            var prevPeriod: TimePeriod? = null
            val periodType = periodTypeToRegisteredTime?.first
            if (periodType != TimePeriodType.CONTINUOUS) {
                timePeriods.indexOf(selectedPeriod)
                    .takeIf { it in 0..<timePeriods.lastIndex }
                    ?.let { idx ->
                        prevPeriod = timePeriods.elementAtOrNull(idx + 1)
                    }
            } else {
                if (selectedPeriod.lastfmPeriod != null && selectedPeriod.lastfmPeriod != LastfmPeriod.OVERALL) {
                    val cal = Calendar.getInstance()
                    cal.setMidnight()
                    val duration =
                        selectedPeriod.lastfmPeriod.toDuration(endTime = cal.timeInMillis)
                    prevPeriod =
                        selectedPeriod.lastfmPeriod.toTimePeriod(endTime = cal.timeInMillis - duration)
                }
            }

            onSelected(selectedPeriod, prevPeriod, refreshCount)
        }

        periodTypeToRegisteredTime?.let { (periodType, _) ->
            val res = TimePeriodDataResult(
                typeSelectorShown = typeSelectorShown,
                periodType = periodType,
                timePeriodsList = timePeriods,
                selectedPeriod = selectedPeriod,
                enabled = enabled
            )
            resultEventBus.sendResult(res)
        }
    }

    ResultEffect<DateRangePickerResult> { res ->
        val timePeriod =
            TimePeriod(res.startUtc.timeToLocal(), res.endUtc.timeToLocal())

        viewModel.setCustomPeriodInput(timePeriod)
    }

    ResultEffect<DatePickerResult> { res ->
        val idx = timePeriods.binarySearch { period ->
            res.timeUtc.timeToLocal().compareTo(period.start)
        }

        if (idx >= 0) {
            viewModel.setSelectedPeriod(timePeriods[idx])
        }
    }

    ResultEffect<TimePeriodClickedResult> { res ->
        if (res.timePeriod == selectedPeriod) {
            selectedPeriodOffset = res.selectedPeriodOffset.let {
                with(density) {
                    DpOffset(it.x.toDp(), 0.dp)
                }
            }
            dropdownTypeShown = periodTypeToRegisteredTime?.first
        } else
            viewModel.setSelectedPeriod(res.timePeriod)
    }

    ResultEffect<TimePeriodTypeClickedResult> {
        typeSelectorShown = true
    }

    LaunchedEffect(typeSelectorShown, enabled) {
        periodTypeToRegisteredTime?.let { (periodType, _) ->
            val res = TimePeriodDataResult(
                typeSelectorShown = typeSelectorShown,
                periodType = periodType,
                timePeriodsList = timePeriods,
                selectedPeriod = selectedPeriod,
                enabled = enabled
            )
            resultEventBus.sendResult(res)
        }
    }

    Box {
        if (typeSelectorShown == true) {
            PeriodTypeSelector(
                onDismissRequest = { typeSelectorShown = false },
                selectedPeriodType = periodTypeToRegisteredTime?.first,
                onMenuItemClick = {
                    viewModel.setPeriodTypeAndRegisteredTime(
                        it,
                        registeredTime
                    )
                },
                onRefresh = if (showRefreshButton) {
                    {
                        viewModel.refresh()
                    }
                } else null
            )
        }

        when (dropdownTypeShown) {
            TimePeriodType.CUSTOM -> {
                val route = PanoRoute.Modal.DateRangePicker(
                    selectedDateRange = selectedPeriod?.let { it.start.timeToUTC() to it.end.timeToUTC() },
                    allowedRange = registeredTime to System.currentTimeMillis(),
                )

                onNavigate(route)
                dropdownTypeShown = null
            }

            TimePeriodType.WEEK -> {
                val route = PanoRoute.Modal.DatePicker(
                    selectedDate = selectedPeriod?.start?.timeToUTC(),
                    allowedRange = registeredTime to System.currentTimeMillis(),
                    weeksOnly = true,
                )
                onNavigate(route)
                dropdownTypeShown = null
            }

            TimePeriodType.MONTH -> {
                MonthPickerPopup(
                    offset = selectedPeriodOffset,
                    selectedMillis = selectedPeriod?.start ?: System.currentTimeMillis(),
                    onDismissRequest = { dropdownTypeShown = null },
                    allowedRange = registeredTime to System.currentTimeMillis(),
                    onMonthMillisSelected = { monthMillis ->
                        val idx = timePeriods.binarySearch { period ->
                            monthMillis.compareTo(period.start)
                        }

                        if (idx >= 0) {
                            viewModel.setSelectedPeriod(timePeriods[idx])
                        }
                    }
                )
            }

            TimePeriodType.CONTINUOUS, TimePeriodType.YEAR, TimePeriodType.LISTENBRAINZ -> {
                PanoDropdownMenu(
                    offset = selectedPeriodOffset,
                    expanded = dropdownTypeShown != null,
                    onDismissRequest = { dropdownTypeShown = null }
                ) {
                    timePeriods.forEach { timePeriod ->
                        item(
                            onClick = {
                                viewModel.setSelectedPeriod(timePeriod)
                                dropdownTypeShown = null
                            },
                            enabled = timePeriod != selectedPeriod,
                            text = {
                                Text(timePeriod.name)
                            },
                        )
                    }
                }
            }

            else -> {}
        }
    }
}

@Composable
fun TimePeriodSelectorRow(
    typeSelectorShown: Boolean?,
    periodType: TimePeriodType,
    timePeriodsList: List<TimePeriod>,
    selectedPeriod: TimePeriod?,
    enabled: Boolean,
    resultEventBus: ResultEventBus,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var selectedPeriodOffset by remember { mutableStateOf(IntOffset.Zero) }
    var firstScrollDone by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPeriod) {
        timePeriodsList.indexOf(selectedPeriod)
            .takeIf { it in timePeriodsList.indices }
            ?.let { idx ->
                val animate = firstScrollDone
                firstScrollDone = true
                listState.scrollToCenterItem(idx, animate = animate)
            }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (typeSelectorShown != null) {
            OutlinedToggleButton(
                enabled = enabled,
                checked = typeSelectorShown,
                contentPadding = ButtonDefaults.ExtraSmallContentPadding,
                onCheckedChange = {
                    if (it) {
                        resultEventBus.sendResult(TimePeriodTypeClickedResult)
                    }
                },
                colors = OutlinedToggleButtonDefaults.myColors(),
                modifier = Modifier
                    .padding(end = 8.dp)
            ) {
                Icon(
                    getPeriodTypeIcon(periodType),
                    contentDescription = stringResource(periodType.stringRes),
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterVertically)
                )

                Icon(
                    rememberClippedPainter(Icons.ArrowDropDown, 16.dp),
                    contentDescription = stringResource(Res.string.item_options),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )
            }
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            contentPadding = PaddingValues(start = 16.dp, end = 24.dp),
            userScrollEnabled = !PlatformStuff.isDesktop && !PlatformStuff.isTv,
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(
                timePeriodsList,
                key = { _, timePeriod ->
                    timePeriod.lastfmPeriod?.name
                        ?: timePeriod.listenBrainzRange?.name
                        ?: timePeriod.start
                }
            ) { idx, timePeriod ->
                val enabled = !PlatformStuff.isTv ||
                        timePeriod == selectedPeriod ||
                        timePeriodsList.getOrNull(idx + 1) == selectedPeriod ||
                        timePeriodsList.getOrNull(idx - 1) == selectedPeriod

                OutlinedToggleButton(
                    enabled = enabled,
                    checked = timePeriod == selectedPeriod,
                    onCheckedChange = {
                        resultEventBus.sendResult(
                            TimePeriodClickedResult(
                                timePeriod,
                                selectedPeriodOffset
                            )
                        )
                    },
                    colors = OutlinedToggleButtonDefaults.myColors(),
                    contentPadding = ButtonDefaults.ExtraSmallContentPadding,
                    modifier = if (timePeriod == selectedPeriod) {
                        Modifier
                            .onGloballyPositioned { coordinates ->
                                selectedPeriodOffset =
                                    coordinates.positionInParent().round() +
                                            IntOffset(
                                                coordinates.size.width / 2,
                                                coordinates.size.height
                                            )
                            }
                    } else
                        Modifier
                ) {

                    if (timePeriodsList.getOrNull(idx - 1) == selectedPeriod) {
                        Icon(
                            imageVector = Icons.ExpandCircleRightFilledAutoMirrored,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    Text(text = timePeriod.name)

                    if (timePeriod == selectedPeriod) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.ExpandCircleDownFilled,
                            contentDescription = null,
                        )
                    } else if (timePeriodsList.getOrNull(idx + 1) == selectedPeriod) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.ExpandCircleRightFilledAutoMirrored,
                            contentDescription = null,
                            modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)
                        )
                    }
                }

                /*

               FilterChip(
                   onClick = {
                       resultEventBus.sendResult(
                           TimePeriodClickedResult(
                               timePeriod,
                               selectedPeriodOffset
                           )
                       )
                   },
                   enabled = enabled,
                   selected = timePeriod == selectedPeriod,
                   trailingIcon = {
                       if (timePeriod == selectedPeriod) {
                           Icon(
                               imageVector = Icons.Filled.ExpandCircleDown,
                               contentDescription = null,
                           )
                       } else if (timePeriodsList.getOrNull(idx + 1) == selectedPeriod) {
                           Icon(
                               imageVector = Icons.Filled.ExpandCircleRight,
                               contentDescription = null,
                               modifier = Modifier.graphicsLayer {
                                   scaleX = -1f
                               }
                           )
                       }
                   },
                   leadingIcon = {
                       if (timePeriodsList.getOrNull(idx - 1) == selectedPeriod) {
                           Icon(
                               imageVector = Icons.Filled.ExpandCircleRight,
                               contentDescription = null
                           )
                       }
                   },
                   shapes = FilterChipDefaults.shapes(),
                   label = { Text(text = timePeriod.name) },
                   modifier = if (timePeriod == selectedPeriod) {
                       Modifier
                           .onGloballyPositioned { coordinates ->
                               selectedPeriodOffset =
                                   coordinates.positionInParent().round() +
                                           IntOffset(
                                               coordinates.size.center.x,
                                               coordinates.size.height
                                           )
                           }
                   } else
                       Modifier
               )

                */
            }
        }
    }
}

@Composable
private fun PeriodTypeSelector(
    onDismissRequest: () -> Unit,
    selectedPeriodType: TimePeriodType?,
    onMenuItemClick: (TimePeriodType) -> Unit,
    onRefresh: (() -> Unit)?
) {
    PanoDropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest
    ) {
        periodTypeMenuItems.forEach { (periodType, icon) ->
            item(
                onClick = {
                    onMenuItemClick(periodType)
                    onDismissRequest()
                },
                enabled = periodType != selectedPeriodType,
                text = { Text(text = stringResource(periodType.stringRes)) },
                leadingIcon = {
                    Icon(imageVector = icon, contentDescription = null)
                }
            )
        }

        if (onRefresh != null) {
            item(
                onClick = {
                    onRefresh()
                    onDismissRequest()
                },
                text = { Text(text = stringResource(Res.string.reload)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Refresh, contentDescription = null)
                }
            )
        }
    }
}

@Composable
fun ChartsCount(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
            .padding(horizontal = 16.dp)
    )
}

@Composable
fun getMusicEntryQString(
    zeroStrRes: StringResource,
    pluralRes: PluralStringResource,
    count: Int,
    isTimePeriodContinuous: Boolean,
): String {
    val plus = if (count == 1000 && !isTimePeriodContinuous) "+" else ""

    return if (count <= 0)
        stringResource(zeroStrRes)
    else
        pluralStringResource(
            pluralRes,
            count,
            count.format() + plus
        )
}

