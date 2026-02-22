package com.arn.scrobble.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.icons.ArrowDropDown
import com.arn.scrobble.icons.Close
import com.arn.scrobble.icons.Error
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Info
import com.arn.scrobble.icons.Person
import com.arn.scrobble.icons.Refresh
import com.arn.scrobble.icons.Search
import com.arn.scrobble.icons.ToggleOff
import com.arn.scrobble.icons.ToggleOn
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.themes.LocalThemeAttributes
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.redactedMessage
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.disable
import pano_scrobbler.composeapp.generated.resources.enable
import pano_scrobbler.composeapp.generated.resources.lastfm
import pano_scrobbler.composeapp.generated.resources.librefm
import pano_scrobbler.composeapp.generated.resources.like_instance
import pano_scrobbler.composeapp.generated.resources.listenbrainz
import pano_scrobbler.composeapp.generated.resources.login_submit
import pano_scrobbler.composeapp.generated.resources.no
import pano_scrobbler.composeapp.generated.resources.ok
import pano_scrobbler.composeapp.generated.resources.pleroma
import pano_scrobbler.composeapp.generated.resources.pref_import
import pano_scrobbler.composeapp.generated.resources.profile_pic
import pano_scrobbler.composeapp.generated.resources.retry
import pano_scrobbler.composeapp.generated.resources.scrobble_to_file
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.yes
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@Composable
fun AlertDialogOk(
    text: String,
    icon: ImageVector = Icons.Info,
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
    AnimatedVisibility(
        errorText != null,
        modifier = modifier.padding(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Error,
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
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Info,
    style: TextStyle = LocalTextStyle.current,
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
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
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
fun SearchField(
    searchTerm: String,
    onSearchTermChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = stringResource(Res.string.search),
) {
    PanoOutlinedTextField(
        value = searchTerm,
        onValueChange = {
            onSearchTermChange(it)
        },
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        leadingIcon = { Icon(Icons.Search, contentDescription = null) },
        trailingIcon = if (!PlatformStuff.isTv) {
            {
                if (searchTerm.isNotEmpty()) {
                    IconButton(onClick = {
                        onSearchTermChange("")
                    }) {
                        Icon(
                            Icons.Close,
                            contentDescription = stringResource(Res.string.delete)
                        )
                    }
                }
            }
        } else
            null,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        singleLine = true,
        enabled = enabled
    )
}

@Composable
fun PanoOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    enabledOnTv: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(
                it.filterNot { it == '\n' || it == '\r' }
            )
        },
        label = label,
        modifier = modifier then
                if (!singleLine) {
                    val focusManager = LocalFocusManager.current

                    Modifier.onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Tab && !it.isShiftPressed) {
                            focusManager.moveFocus(FocusDirection.Next)
                            true
                        } else
                            false
                    }
                } else
                    Modifier,
        enabled = enabled && (!PlatformStuff.isTv || enabledOnTv),
//        readOnly = PlatformStuff.isTv,
        placeholder = placeholder,
        singleLine = singleLine,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ButtonWithSpinner(
    prefixText: String?,
    itemToTexts: Map<T, String>,
    selected: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dropDownShown by remember { mutableStateOf(false) }

    OutlinedToggleButton(
        checked = dropDownShown,
        onCheckedChange = { dropDownShown = it },
        modifier = modifier
    ) {
        Text(
            if (prefixText == null) {
                itemToTexts[selected] ?: ""
            } else {
                "$prefixText: ${itemToTexts[selected] ?: ""}"
            }
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Icon(Icons.ArrowDropDown, contentDescription = null)

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
    modifier: Modifier = Modifier,
    filledStyle: Boolean = false,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.secondary,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(contentDescription) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        if (filledStyle) {
            FilledTonalIconButton(
                onClick = onClick,
                enabled = enabled,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint
                )
            }
        } else {
            IconButton(
                onClick = onClick,
                enabled = enabled,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint
                )
            }
        }
    }
}

@Composable
fun ButtonWithIcon(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
fun EmptyTextWithImportButtonOnTv(
    visible: Boolean,
    text: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = stringResource(Res.string.pref_import),
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(16.dp)
            )

            if (PlatformStuff.isTv) {
                OutlinedButton(
                    onClick = onButtonClick,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(text = buttonText)
                }
            }
        }
    }
}


@Composable
fun SimpleHeaderItem(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = horizontalOverscanPadding()),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(end = 32.dp)
            )

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VerifyButton(
    doStuff: () -> Unit,
    result: Result<*>?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = stringResource(Res.string.login_submit),
    extraContent: @Composable RowScope.() -> Unit = {},
) {
    var verifying by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(result) {
        result?.onFailure {
            verifying = false
            errorText = it.redactedMessage
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
                    CircularWavyProgressIndicator()
                } else {
                    OutlinedButton(
                        onClick = {
                            verifying = true
                            errorText = null
                            doStuff()
                        },
                        modifier = Modifier.align(Alignment.CenterEnd).testTag("button_verify")
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
    avatarName: String?,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    if (!avatarUrl.isNullOrEmpty()) {
        AsyncImage(
            model = avatarUrl,
            error = placeholderImageVectorPainter(null, Icons.Person),
            placeholder = placeholderPainter(),
            contentDescription = stringResource(Res.string.profile_pic),
            modifier = modifier,
        )
    } else if (avatarName != null) {
        val themeAttributes = LocalThemeAttributes.current
        val index =
            abs(avatarName.hashCode()) % themeAttributes.allOnSecondaryContainerColors.size

        val initials = avatarName.split(" ", limit = 2)
            .joinToString("") { it.take(1) }
            .takeIf { it.isNotEmpty() } ?: " "

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .background(themeAttributes.allSecondaryContainerColors[index])
        ) {
            Text(
                text = initials.uppercase(),
                style = textStyle,
                softWrap = false,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = themeAttributes.allOnSecondaryContainerColors[index],
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
    val errorText = remember(throwable) {
        throwable.redactedMessage
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Error,
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
                imageVector = Icons.Refresh,
                contentDescription = stringResource(Res.string.retry)
            )
        }
    }
}


@Composable
fun customFpsInfiniteAnimation(
    initialValue: Float,
    targetValue: Float,
    durationMillis: Int,
    enabled: Boolean = true,
    frameDelay: Long = 1000L / 24
): State<Float> {
    val animatable = remember { Animatable(initialValue) }
    var direction by remember { mutableFloatStateOf(1f) } // 1f: forward, -1f: reverse
    var playhead by remember { mutableFloatStateOf(0f) } // 0f=start, 1f=end

    LaunchedEffect(enabled) {
        while (enabled) {
            val totalFrames = (durationMillis / frameDelay).toInt()
            val nextFrame = playhead + direction / totalFrames
            playhead = nextFrame.coerceIn(0f, 1f)
            val value = initialValue + (targetValue - initialValue) * playhead
            animatable.snapTo(value)
            if (playhead >= 1f) direction = -1f
            if (playhead <= 0f) direction = 1f
            delay(frameDelay)
        }
    }

    return animatable.asState()
}

@Composable
fun YesNoDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onYes: () -> Unit,
    onNo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.yes)) },
            onClick = {
                onYes()
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.no)) },
            onClick = {
                onNo()
                onDismissRequest()
            },
        )
    }
}

@Composable
fun rememberLocaleWithCustomWeekday(): Locale {
    // is empty on desktop
    val currentLocale = LocalLocaleList.current.localeList.firstOrNull()?.platformLocale
    val firstWeekDay by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.firstDayOfWeek }
    return remember {
        val currentLocaleSafe = currentLocale ?: Locale.getDefault()

        val firstWeekDayStr = when (firstWeekDay) {
            Calendar.SUNDAY -> "sun"
            Calendar.MONDAY -> "mon"
            Calendar.TUESDAY -> "tue"
            Calendar.WEDNESDAY -> "wed"
            Calendar.THURSDAY -> "thu"
            Calendar.FRIDAY -> "fri"
            Calendar.SATURDAY -> "sat"
            else -> return@remember currentLocaleSafe
        }

        Locale.Builder()
            .setLocale(currentLocaleSafe)
            .setUnicodeLocaleKeyword("fw", firstWeekDayStr)
            .build()
    }
}

@Composable
fun InlineCheckButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    IconButton(
        onClick = { onCheckedChange(!checked) },
    ) {
        Icon(
            imageVector = if (checked)
                Icons.ToggleOn
            else
                Icons.ToggleOff,
            contentDescription = if (checked)
                stringResource(Res.string.disable)
            else
                stringResource(Res.string.enable),
        )
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
            -> if (PlatformStuff.isDesktop || PlatformStuff.isTv) 27.dp
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
fun combineImageVectors(
    main: ImageVector,
    secondary: ImageVector,
): VectorPainter {
    val mainScaleFactor = 0.7f
    val secondaryScaleFactor = 0.5f

    return rememberVectorPainter(
        defaultWidth = main.defaultWidth,
        defaultHeight = main.defaultHeight,
        viewportWidth = main.viewportWidth,
        viewportHeight = main.viewportHeight,
        name = main.name,
        tintColor = main.tintColor,
        tintBlendMode = main.tintBlendMode,
        autoMirror = main.autoMirror
    ) { viewportWidth, viewportHeight ->
        Group(
            name = main.root.name + "_main",
            scaleX = mainScaleFactor,
            scaleY = mainScaleFactor,
            translationY = (viewportWidth - main.viewportWidth * mainScaleFactor) / 2,
        ) {
            RenderVectorGroup(group = main.root)
        }

        Group(
            name = secondary.root.name + "_secondary",
            scaleX = secondaryScaleFactor,
            scaleY = secondaryScaleFactor,
            translationX = (viewportWidth - secondary.viewportWidth * secondaryScaleFactor),
            translationY = (viewportHeight - secondary.viewportHeight * secondaryScaleFactor),
        ) {
            RenderVectorGroup(group = secondary.root)
        }
    }
}

@Composable
fun minGridSize(): Dp {
    val navigationType = LocalNavigationType.current
    return when (navigationType) {
        PanoNavigationType.BOTTOM_NAVIGATION -> 170.dp
        PanoNavigationType.NAVIGATION_RAIL,
            -> 200.dp

        PanoNavigationType.PERMANENT_NAVIGATION_DRAWER -> 250.dp
    }
}

@Composable
fun accountTypeLabel(accountType: AccountType) = when (accountType) {
    AccountType.LASTFM -> stringResource(Res.string.lastfm)
    AccountType.LIBREFM -> stringResource(Res.string.librefm)
    AccountType.GNUFM -> stringResource(Res.string.like_instance, stringResource(Res.string.lastfm))
    AccountType.LISTENBRAINZ -> stringResource(Res.string.listenbrainz)
    AccountType.CUSTOM_LISTENBRAINZ -> stringResource(
        Res.string.like_instance,
        stringResource(Res.string.listenbrainz)
    )

//    AccountType.MALOJA -> stringResource(Res.string.maloja)
    AccountType.PLEROMA -> stringResource(Res.string.pleroma)
    AccountType.FILE -> stringResource(Res.string.scrobble_to_file)
}

@Composable
fun Modifier.shimmerWindowBounds() = this.shimmer(rememberShimmer(ShimmerBounds.Window))

@Composable
expect fun isImeVisible(): Boolean

@Composable
expect fun getActivityOrNull(): Any?

@Composable
expect fun AppIcon(
    appItem: AppItem?,
    modifier: Modifier = Modifier,
)

expect fun Modifier.testTagsAsResId(): Modifier