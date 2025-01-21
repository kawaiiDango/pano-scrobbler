package com.arn.scrobble.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.themes.LocalThemeAttributes
import com.arn.scrobble.utils.PlatformStuff
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.close
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.login_submit
import pano_scrobbler.composeapp.generated.resources.network_error
import pano_scrobbler.composeapp.generated.resources.ok
import pano_scrobbler.composeapp.generated.resources.profile_pic
import pano_scrobbler.composeapp.generated.resources.retry
import pano_scrobbler.composeapp.generated.resources.search
import kotlin.math.abs

@Composable
fun AlertDialogOk(
    text: String,
    icon: ImageVector = Icons.Outlined.Info,
    onConfirmation: () -> Unit = {},
    onDismissRequest: () -> Unit = onConfirmation,
    title: String? = null,
    confirmText: String = stringResource(Res.string.ok),
) {
    AlertDialog(
        title = if (title != null) {
            {
                Text(text = title)
            }
        } else
            null,
        text = {
            Text(text = text)
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text(text = confirmText)
            }
        }
    )
}

@Composable
fun OutlinedToggleButtons(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        items.forEachIndexed { index, item ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                selected = selectedIndex == index,
                onClick = {
                    if (selectedIndex != index) {
                        onSelected(index)
                    }
                },
            ) {
                Text(text = item)
            }
        }
    }
}


@Composable
fun <E : Enum<*>> RadioButtonGroup(
    enumToTexts: Map<E, String>,
    selected: E?,
    onSelected: (E) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        enumToTexts.forEach { (enu, text) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        if (enu != selected)
                            onSelected(enu)
                    }
                    .padding(horizontal = 8.dp, vertical = 16.dp)
            ) {
                RadioButton(
                    selected = selected == enu,
                    onClick = null,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text(text)
            }
        }
    }
}

@Composable
fun ErrorText(
    errorText: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(errorText != null) {
        Row(
            modifier = modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = errorText ?: "",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}


@Composable
fun InfoText(
    text: String,
    icon: ImageVector = Icons.Outlined.Info,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = style
        )
    }
}


@Composable
fun TextWithIcon(
    text: String,
    icon: ImageVector,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
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
fun SearchBox(
    searchTerm: String,
    label: String = stringResource(Res.string.search),
    onSearchTermChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = searchTerm,
        onValueChange = {
            onSearchTermChange(it)
        },
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (searchTerm.isNotEmpty()) {
                IconButton(onClick = {
                    onSearchTermChange("")
                }) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(Res.string.delete)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        singleLine = true,
    )
}

@Composable
fun <T> ButtonWithSpinner(
    prefixText: String?,
    itemToTexts: Map<T, String>,
    selected: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dropDownShown by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { dropDownShown = true },
        modifier = modifier.padding(end = 8.dp)
    ) {
        Text(
            if (prefixText == null) {
                itemToTexts[selected] ?: ""
            } else {
                "$prefixText: ${itemToTexts[selected] ?: ""}"
            }
        )
        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)

        DropdownMenu(
            expanded = dropDownShown,
            onDismissRequest = { dropDownShown = false }
        ) {
            itemToTexts.forEach { (item, text) ->
                DropdownMenuItem(
                    onClick = {
                        onItemSelected(item)
                        dropDownShown = false
                    },
                    enabled = item != selected,
                    text = {
                        Text(text)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconButtonWithTooltip(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.secondary,
    modifier: Modifier = Modifier,
) {
    // todo find out why this freezes
//    TooltipBox(
//        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
//        tooltip = { PlainTooltip { Text(contentDescription) } },
//        state = rememberTooltipState(),
//        modifier = modifier,
//    ) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
//    }
}

@Composable
fun ButtonWithIcon(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        modifier = modifier
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDialogParent(
    onDismiss: () -> Unit,
    skipPartiallyExpanded: Boolean = PlatformStuff.isDesktop,
    padding: Boolean = true,
    content: @Composable (modifier: Modifier) -> Unit,
) {
    val isTabletUi = LocalNavigationType.current != PanoNavigationType.BOTTOM_NAVIGATION

    val sheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = isTabletUi || skipPartiallyExpanded)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = if (PlatformStuff.isTv || PlatformStuff.isDesktop) null
        else {
            { BottomSheetDefaults.DragHandle() }
        },
        sheetGesturesEnabled = !PlatformStuff.isTv && !PlatformStuff.isDesktop,
        sheetState = sheetState,
    ) {
        if (PlatformStuff.isDesktop) {
            IconButtonWithTooltip(
                icon = Icons.Outlined.Close,
                onClick = onDismiss,
                contentDescription = stringResource(Res.string.close),
                modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally),
            )
        }

        content(
            Modifier
                .fillMaxWidth()
                .then(
                    if (padding)
                        if (PlatformStuff.isTv || PlatformStuff.isDesktop)
                            Modifier.padding(24.dp)
                        else
                            Modifier.padding(horizontal = 24.dp)
                    else
                        Modifier
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogParent(
    onDismiss: () -> Unit,
    content: @Composable (modifier: Modifier) -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        content = {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                content(Modifier.padding(24.dp))
            }
        }
    )
}

@Composable
fun EmptyText(
    visible: Boolean,
    text: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }
    }
}


@Composable
fun SimpleHeaderItem(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium
            )
            .padding(vertical = 8.dp, horizontal = horizontalOverscanPadding()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(end = 32.dp)
                .size(24.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun VerifyButton(
    doStuff: () -> Unit,
    result: Result<*>?,
    onDone: () -> Unit,
    buttonText: String = stringResource(Res.string.login_submit),
    modifier: Modifier = Modifier,
    extraContent: @Composable RowScope.() -> Unit = {},
) {
    var verifying by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val networkErrorStr = stringResource(Res.string.network_error)

    LaunchedEffect(result) {
        result?.onFailure {
            verifying = false
            errorText = it.message ?: networkErrorStr
        }?.onSuccess {
            verifying = false
            errorText = null
            onDone()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.align(Alignment.End)
        ) {
            extraContent()
            Box(
                modifier = Modifier
                    .height(60.dp)
            ) {
                if (verifying) {
                    CircularProgressIndicator()
                } else {
                    OutlinedButton(
                        onClick = {
                            verifying = true
                            errorText = null
                            doStuff()
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(buttonText, maxLines = 1)
                    }
                }
            }
        }

        ErrorText(errorText)
    }
}

@Composable
fun AvatarOrInitials(
    avatarUrl: String?,
    avatarInitialLetter: Char?,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    modifier: Modifier = Modifier
        .padding(8.dp)
        .size(24.dp)
        .clip(CircleShape),
) {
    if (!avatarUrl.isNullOrEmpty()) {
        AsyncImage(
            model = avatarUrl,
            error = rememberVectorPainter(Icons.Outlined.Person),
            placeholder = placeholderPainter(),
            contentDescription = stringResource(Res.string.profile_pic),
            modifier = modifier,
        )
    } else if (avatarInitialLetter != null) {
        val themeAttributes = LocalThemeAttributes.current
        val index = abs(avatarInitialLetter.hashCode()) % themeAttributes.allOnSecondaryColors.size

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .background(themeAttributes.allSecondaryContainerColors[index])
        ) {
            Text(
                text = avatarInitialLetter.uppercase(),
                style = textStyle,
                softWrap = false,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = themeAttributes.allOnSecondaryColors[index],
            )
        }
    }
}

@Composable
fun ListLoadError(
    throwable: Throwable,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val genericErrorText = stringResource(Res.string.network_error)
    val errorText = remember(throwable) {
        if (throwable is ApiException)
            genericErrorText + ": " + throwable.code
        else
            throwable.localizedMessage!!
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = errorText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )
        IconButton(
            onClick = onRetry,
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = stringResource(Res.string.retry)
            )
        }
    }
}


@Composable
fun Modifier.backgroundForShimmer(
    isShimmer: Boolean,
    shape: Shape = MaterialTheme.shapes.medium,
): Modifier {
    if (!isShimmer) return this

    return clip(shape)
        .background(Color.Gray.copy(alpha = 0.3f))
}

fun Modifier.shake(enabled: Boolean) = composed(

    factory = {
        val scale by animateFloatAsState(
            targetValue = if (enabled) .9f else 1f,
            animationSpec = repeatable(
                iterations = 5,
                animation = tween(durationMillis = 50, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        Modifier.graphicsLayer {
            scaleX = if (enabled) scale else 1f
            scaleY = if (enabled) scale else 1f
        }
    },
    inspectorInfo = debugInspectorInfo {
        name = "shake"
        properties["enabled"] = enabled
    }
)

@Composable
fun horizontalOverscanPadding(): Dp {
    val navigationType = LocalNavigationType.current
    return when (navigationType) {
        PanoNavigationType.BOTTOM_NAVIGATION -> 16.dp
        PanoNavigationType.NAVIGATION_RAIL -> 24.dp
        PanoNavigationType.PERMANENT_NAVIGATION_DRAWER -> 48.dp
    }
}

@Composable
fun verticalOverscanPadding(): Dp {
    val navigationType = LocalNavigationType.current
    return when (navigationType) {
        PanoNavigationType.BOTTOM_NAVIGATION,
        PanoNavigationType.NAVIGATION_RAIL,
            -> if (PlatformStuff.isDesktop) 27.dp
        else 0.dp

        PanoNavigationType.PERMANENT_NAVIGATION_DRAWER -> 27.dp
    }
}

@Composable
fun placeholderPainter(): ColorPainter {
    val color = MaterialTheme.colorScheme.surfaceContainerHigh
    return remember { ColorPainter(color) }
}

@Composable
fun Modifier.shimmerWindowBounds() = Modifier.shimmer(rememberShimmer(ShimmerBounds.Window))

@Composable
expect fun getActivityOrNull(): Any?

@Composable
expect fun AppIcon(
    appItem: AppItem?,
    modifier: Modifier = Modifier,
)