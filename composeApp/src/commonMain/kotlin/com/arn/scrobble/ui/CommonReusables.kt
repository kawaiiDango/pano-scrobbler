package com.arn.scrobble.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.FilledTonalToggleButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.OutlinedToggleButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.icons.ArrowDropDown
import com.arn.scrobble.icons.Error
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Info
import com.arn.scrobble.icons.OpenInBrowser
import com.arn.scrobble.icons.Person
import com.arn.scrobble.icons.Refresh
import com.arn.scrobble.icons.ToggleOff
import com.arn.scrobble.icons.ToggleOn
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.themes.LocalThemeAttributes
import com.arn.scrobble.utils.LocalNetworkHandshakeExceptionWrapper
import com.arn.scrobble.utils.LocalNetworkPermissionNeededException
import com.arn.scrobble.utils.LocalNetworkPermissionsRequest
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.add_exception
import pano_scrobbler.composeapp.generated.resources.disable
import pano_scrobbler.composeapp.generated.resources.enable
import pano_scrobbler.composeapp.generated.resources.fix_it_action
import pano_scrobbler.composeapp.generated.resources.lastfm
import pano_scrobbler.composeapp.generated.resources.librefm
import pano_scrobbler.composeapp.generated.resources.like_instance
import pano_scrobbler.composeapp.generated.resources.listenbrainz
import pano_scrobbler.composeapp.generated.resources.login_submit
import pano_scrobbler.composeapp.generated.resources.missing_local_network_permission
import pano_scrobbler.composeapp.generated.resources.no
import pano_scrobbler.composeapp.generated.resources.ok
import pano_scrobbler.composeapp.generated.resources.pleroma
import pano_scrobbler.composeapp.generated.resources.pref_import
import pano_scrobbler.composeapp.generated.resources.profile_pic
import pano_scrobbler.composeapp.generated.resources.retry
import pano_scrobbler.composeapp.generated.resources.scrobble_to_file
import pano_scrobbler.composeapp.generated.resources.unsupported_api
import pano_scrobbler.composeapp.generated.resources.yes
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AlertDialogOk(
    text: String,
    icon: ImageVector = Icons.Info,
    onConfirmation: () -> Unit = {},
    onDismissRequest: () -> Unit = onConfirmation,
    title: String? = null,
    confirmText: String = stringResource(Res.string.ok),
    scrollable: Boolean = false,
) {
    AlertDialog(
        title = if (title != null) {
            {
                Text(text = title)
            }
        } else
            null,
        text = {
            if (LocalThemeAttributes.current.blurSubWindow)
                ApplyWindowBlur(
                    behind = Stuff.BLUR_BACKDROP_RADIUS_DP,
                    bg = Stuff.BLUR_FROSTED_RADIUS_DP
                )

            Text(
                text = text,
                modifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
            )
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
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    onConfirmation()
                }
            ) {
                Text(text = confirmText)
            }
        }
    )
}

enum class PanoToggleButtonsMode {
    Text,
    Icon,
    Both,
    BothVertical
}

@Composable
fun PanoToggleButtonGroup(
    texts: Collection<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: Collection<ImageVector?> = List(texts.size) { null },
    chevronAt: Int? = null,
    horizontalArrangement: Arrangement.Horizontal = ButtonGroupDefaults.HorizontalArrangement,
    enabled: Boolean = true,
    mode: PanoToggleButtonsMode = PanoToggleButtonsMode.Text,
    colors: ToggleButtonColors = OutlinedToggleButtonDefaults.myColors(),
    border: Boolean = true,
    textStyle: TextStyle? = null,
) {
    val interactionSources =
        remember(texts.size) { List(texts.size) { MutableInteractionSource() } }

    ButtonGroup(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(
                menuState = menuState,
                colors = IconButtonDefaults.filledTonalIconButtonColors()
            )
        },
    ) {
        (texts zip icons).forEachIndexed { index, itemToIcon ->
            val (text, icon) = itemToIcon
            val checked = index == selectedIndex

            val iconVisible = (checked || mode != PanoToggleButtonsMode.Text) && icon != null
            val textVisible = checked || mode != PanoToggleButtonsMode.Icon
            val contentDescription = if (!textVisible && iconVisible) text else null

            val contentPadding =
                ToggleButtonDefaults.contentPaddingFor(ToggleButtonDefaults.MinHeight)

            customItem(
                buttonGroupContent = {
                    OutlinedToggleButton(
                        onCheckedChange = {
                            if (!checked && it || chevronAt == index) {
                                onSelected(index)
                            }
                        },
                        checked = checked,
                        enabled = enabled,
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            texts.size - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        contentPadding = contentPadding,
                        interactionSource = interactionSources[index],
                        colors = colors,
                        border = if (border)
                            OutlinedToggleButtonDefaults.border(enabled, checked)
                        else
                            null,
                        modifier = Modifier
                            .animateWidth(
                                interactionSource = interactionSources[index],
                                compressionLimit = contentPadding.calculateEndPadding(
                                    LocalLayoutDirection.current
                                ),
                            ) then (
                                if (mode == PanoToggleButtonsMode.BothVertical)
                                    Modifier.widthIn(min = 90.dp)
                                else
                                    Modifier
                                )
                    ) {
                        if (mode == PanoToggleButtonsMode.BothVertical) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (iconVisible) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.requiredSize(IconButtonDefaults.mediumIconSize)
                                    )

                                    if (textVisible)
                                        Spacer(Modifier.height(2.dp))
                                }

                                Text(
                                    text = text,
                                    style = textStyle ?: LocalTextStyle.current,
                                    softWrap = false,
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible,
                                )
                            }
                        } else {
                            if (iconVisible) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = contentDescription,
                                    modifier = Modifier.requiredSize(IconButtonDefaults.mediumIconSize)
                                )

                                if (textVisible)
                                    Spacer(Modifier.width(ButtonDefaults.ExtraSmallIconSpacing))
                            }

                            if (textVisible) {
                                Text(
                                    text = text,
                                    style = textStyle ?: LocalTextStyle.current,
                                    softWrap = false,
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible,
                                )
                            }

                            if (chevronAt == index) {
                                // no spacer, the chevron image has plenty
                                Icon(
                                    imageVector = Icons.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.requiredSize(IconButtonDefaults.mediumIconSize)
                                )
                            }
                        }
                    }
                },
                menuContent = {
                    DropdownMenuItem(
                        leadingIcon = icon?.let {
                            { Icon(imageVector = it, contentDescription = null) }
                        },
                        shape = MenuDefaults.standaloneItemShape,
                        text = { Text(text) },
                        onClick = {
                            if (!checked || chevronAt == index) {
                                onSelected(index)
                            }
                        },
                        interactionSource = interactionSources[index],
                    )
                }
            )
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
            style = style,
            maxLines = 1,
        )
    }
}

@Composable
fun SearchEffect(
    searchFieldState: TextFieldState,
    initialText: String = "",
    savedState: MutableState<String> = rememberSaveable { mutableStateOf(initialText) },
    onSearch: (String) -> Unit,
) {
    var restored by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LifecycleResumeEffect(searchFieldState.text) {
        val job: Job?
        if (!restored) {
            searchFieldState.setTextAndPlaceCursorAtEnd(savedState.value)
            restored = true
            onSearch(savedState.value)
            job = null
        } else {
            job = scope.launch {
                delay(500.milliseconds)

                val text = searchFieldState.text.toString()
                if (savedState.value != text) {
                    Logger.d { "onSearch $text" }
                    savedState.value = text
                    onSearch(text)
                }
            }
        }

        onPauseOrDispose {
            job?.cancel()
        }
    }
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
        shape = TextFieldDefaults.roundedShape,
        enabled = enabled && (!PlatformStuff.isTv || enabledOnTv),
//        readOnly = PlatformStuff.isTv,
        placeholder = placeholder,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 10,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

@Composable
fun <T> ButtonWithDropdown(
    prefixText: String?,
    itemToTexts: Map<T, String>,
    selected: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dropDownShown by remember { mutableStateOf(false) }

    FilledTonalToggleButton(
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
        Icon(Icons.ArrowDropDown, contentDescription = null)

        PanoDropdownMenu(
            expanded = dropDownShown,
            onDismissRequest = { dropDownShown = false }
        ) {
            itemToTexts.forEach { (item, text) ->
                item(
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
    checked: Boolean? = null,
    enabled: Boolean = true,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(contentDescription) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        if (checked != null) {
            OutlinedIconToggleButton(
                checked = checked,
                border = null,
                shapes = IconButtonDefaults.toggleableShapes(),
                onCheckedChange = { onClick() },
                enabled = enabled,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                )
            }
        } else {
            IconButton(
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.iconButtonVibrantColors(),
                onClick = onClick,
                enabled = enabled,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
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
    maxLines: Int = 1,
    contentColorOverride: Color? = null,
) {
    OutlinedButton(
        shapes = ButtonDefaults.shapes(),
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        enabled = enabled,
        modifier = modifier
    ) {
        val contentColor = contentColorOverride ?: LocalContentColor.current
        Icon(
            icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(
            text,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            color = contentColor,
        )
    }
}

fun LazyListScope.emptyText(
    string: @Composable () -> String,
) {
    item("list_empty_text", contentType = "list_empty_text") {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .animateItem()
                .fillParentMaxWidth()
                .fillParentMaxHeight(0.5f)
        ) {
            Text(
                text = string(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
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
                FilledTonalButton(
                    shapes = ButtonDefaults.shapes(),
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
    modifier: Modifier
) {
    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                LocalAbsoluteTonalElevation.current + 2.dp
            ),
        ),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun LabeledCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    transparentSurface: Boolean = true,
    isSwitch: Boolean = false,
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    maxLines: Int = 2,
) {
    val colors = if (transparentSurface) {
        ListItemDefaults.myTransparentCheckableItemColors()
    } else {
        ListItemDefaults.myCheckableItemColors()
    }

    ListItem(
        enabled = enabled,
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = colors,
        leadingContent =
            if (!isSwitch) {
                {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null
                    )
                }
            } else null,
        trailingContent =
            if (isSwitch) {
                {
                    Switch(
                        checked = checked,
                        onCheckedChange = null
                    )
                }
            } else null,
        modifier = modifier
    ) {
        Text(
            text,
            maxLines = maxLines,
            style = textStyle ?: LocalTextStyle.current
        )
    }
}

@Composable
fun VerifyButton(
    doStuff: () -> Unit,
    result: Result<*>?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onTrustAll: (() -> Unit)? = null,
) {
    var verifying by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }

    var gotTlsErrOnLocal by rememberSaveable { mutableStateOf(false) }
    var localNetworkPermissionMissing by rememberSaveable { mutableStateOf(false) }
    val missingPermText = stringResource(Res.string.missing_local_network_permission)

    LaunchedEffect(result) {
        result?.onFailure {
            verifying = false
            errorText = it.redactedMessage

            if (it is LocalNetworkHandshakeExceptionWrapper && onTrustAll != null && !gotTlsErrOnLocal) {
                gotTlsErrOnLocal = true
            } else if (it is LocalNetworkPermissionNeededException) {
                localNetworkPermissionMissing = true
            }
        }?.onSuccess {
            verifying = false
            errorText = null
            onDone()
        }
    }

    if (localNetworkPermissionMissing) {
        LocalNetworkPermissionsRequest(onGranted = {
            doStuff()
        }, onDenied = {
            errorText = missingPermText
        })
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.align(Alignment.End)
        ) {
            Box(
                modifier = Modifier
                    .height(60.dp)
            ) {
                if (verifying) {
                    CircularWavyProgressIndicator()
                } else {
                    FilledTonalButton(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            verifying = true
                            errorText = null
                            if (gotTlsErrOnLocal && onTrustAll != null) {
                                onTrustAll()
                            } else
                                doStuff()
                        },
                        modifier = Modifier.align(Alignment.CenterEnd).testTag("button_verify")
                    ) {
                        if (gotTlsErrOnLocal) {
                            Text(stringResource(Res.string.add_exception))
                        } else {
                            Text(stringResource(Res.string.login_submit), maxLines = 1)
                        }
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
    avatarName: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    initials: String? = null, // null is default parsing method
) {
    if (!avatarUrl.isNullOrEmpty()) {
        AsyncImage(
            model = avatarUrl,
            error = placeholderImageVectorPainter(null, Icons.Person),
            placeholder = placeholderPainter(),
            contentDescription = stringResource(Res.string.profile_pic),
            modifier = modifier,
        )
    } else {
        val themeAttributes = LocalThemeAttributes.current
        val index =
            abs(avatarName.hashCode()) % themeAttributes.avatarColors.size

        val initials by remember(avatarName, initials) {
            val i = initials ?: avatarName.split(" ", limit = 2)
                .joinToString("") { it.take(1).uppercase() }
                .takeIf { it.isNotEmpty() } ?: " "

            mutableStateOf(i)
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .background(themeAttributes.avatarContainerColors[index])
        ) {
            Text(
                text = initials,
                style = textStyle,
                softWrap = false,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = themeAttributes.avatarColors[index],
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
    val accountType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }
    val profileLink by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccount?.user?.url }
    val displayProfileLink = (accountType.isCustom) &&
            (throwable is ApiException || throwable is SerializationException) &&
            profileLink != null

    val errorText = if (displayProfileLink) {
        stringResource(Res.string.unsupported_api)
    } else {
        remember(throwable) { throwable.redactedMessage }
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        if (displayProfileLink) {
            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = {
                    val url = profileLink!!.toHttpUrl()
                    val topPrivateDomain = url.topPrivateDomain()
                    PlatformStuff.openInBrowser(url.scheme + "://" + topPrivateDomain)
                },
            ) {
                Icon(
                    imageVector = Icons.OpenInBrowser,
                    contentDescription = stringResource(Res.string.fix_it_action)
                )
            }
        } else {
            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = onRetry,
            ) {
                Icon(
                    imageVector = Icons.Refresh,
                    contentDescription = stringResource(Res.string.retry)
                )
            }
        }
    }
}

@Composable
fun YesNoDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onYes: () -> Unit,
    onNo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PanoDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        item(
            text = { Text(stringResource(Res.string.yes)) },
            onClick = {
                onYes()
                onDismissRequest()
            },
        )
        item(
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
        shapes = IconButtonDefaults.shapes(),
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
fun Modifier.shapedClickable(
    shape: Shape = MaterialTheme.shapes.medium,
    clickableAdded: Boolean = true,
    onClick: () -> Unit,
) = clip(shape)
    .then(
        if (clickableAdded)
            Modifier.clickable(
                interactionSource = null,
                onClick = onClick,
                role = Role.Button,
                indication = ripple(
                    focusRingShape = shape
                )
            )
        else Modifier
    )

@Composable
fun Modifier.backgroundForShimmer(
    isShimmer: Boolean,
    shape: Shape = MaterialTheme.shapes.medium,
) = if (!isShimmer)
    this
else
    background(MaterialTheme.colorScheme.surfaceContainerHighest, shape)

fun horizontalOverscanPadding(): Dp =
    if (PlatformStuff.isTv)
        48.dp
    else
        0.dp


fun verticalOverscanPadding() =
    if (PlatformStuff.isTv)
        27.dp
    else
        0.dp

@Composable
fun placeholderPainter(): ColorPainter {
    val color = MaterialTheme.colorScheme.surfaceContainerHigh
    return remember { ColorPainter(color.copy(alpha = 0.5f)) }
}

private class ClippedCenteredPainter(
    private val painter: Painter,
    private val clippedSize: Size,
) : Painter() {

    override val intrinsicSize: Size
        get() = clippedSize

    override fun DrawScope.onDraw() {
        val originalSize = painter.intrinsicSize
        val dx = (size.width - originalSize.width) / 2f
        val dy = (size.height - originalSize.height) / 2f

        clipRect {
            translate(left = dx, top = dy) {
                with(painter) {
                    draw(originalSize)
                }
            }
        }
    }
}

@Composable
fun rememberClippedPainter(
    imageVector: ImageVector,
    width: Dp,
    height: Dp = width,
): Painter {
    val vectorPainter = rememberVectorPainter(imageVector)
    val density = LocalDensity.current

    val clippedSizePx = remember(width, height, density) {
        with(density) { Size(width.toPx(), height.toPx()) }
    }

    return remember(vectorPainter, clippedSizePx) {
        ClippedCenteredPainter(vectorPainter, clippedSizePx)
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

fun accountTypeStringRes(accountType: AccountType): Pair<StringResource, StringResource?> {
    val strRes = when (accountType) {
        AccountType.LASTFM -> Res.string.lastfm
        AccountType.LIBREFM -> Res.string.librefm
        AccountType.GNUFM -> Res.string.like_instance
        AccountType.LISTENBRAINZ -> Res.string.listenbrainz
        AccountType.CUSTOM_LISTENBRAINZ,
        AccountType.CUSTOM_LISTENBRAINZ_2,
        AccountType.CUSTOM_LISTENBRAINZ_3
            -> Res.string.like_instance

        AccountType.PLEROMA -> Res.string.pleroma
        AccountType.FILE -> Res.string.scrobble_to_file
    }

    val formatRes = when (accountType) {
        AccountType.GNUFM -> Res.string.lastfm
        AccountType.CUSTOM_LISTENBRAINZ,
        AccountType.CUSTOM_LISTENBRAINZ_2,
        AccountType.CUSTOM_LISTENBRAINZ_3
            -> Res.string.listenbrainz

        else -> null
    }

    return strRes to formatRes
}

@Composable
fun accountTypeLabel(accountType: AccountType): String {
    val (strRes, formatRes) = accountTypeStringRes(accountType)
    return if (formatRes == null)
        stringResource(strRes)
    else {
        val slot = when (accountType) {
            AccountType.CUSTOM_LISTENBRAINZ -> 1
            AccountType.CUSTOM_LISTENBRAINZ_2 -> 2
            AccountType.CUSTOM_LISTENBRAINZ_3 -> 3
            else -> null
        }

        stringResource(strRes, stringResource(formatRes)) +
                if (slot != null) " $slot" else ""
    }
}

@Composable
fun ListItemDefaults.myTransparentCheckableItemColors() = ListItemDefaults.colors().let {
    ListItemDefaults.colors(
        containerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        selectedContentColor = it.contentColor,
        selectedContainerColor = Color.Transparent,
        selectedSupportingContentColor = it.leadingContentColor,
        selectedLeadingContentColor = it.leadingContentColor,
        selectedTrailingContentColor = it.trailingContentColor,
        selectedOverlineContentColor = it.overlineContentColor,
    )
}

@Composable
fun ListItemDefaults.myCheckableItemColors() = ListItemDefaults.colors().let {
    ListItemDefaults.colors(
        containerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        selectedContainerColor = it.selectedContainerColor.copy(alpha = 0.4f),
    )
}

@Composable
fun ListItemDefaults.myTogglableHeaderItemColors(): ListItemColors {
    val surface =
        MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current + 2.dp)

    return ListItemDefaults.colors(
        contentColor = MaterialTheme.colorScheme.primary,
        leadingContentColor = MaterialTheme.colorScheme.primary,
        trailingContentColor = MaterialTheme.colorScheme.primary,
        containerColor = surface,
        disabledContainerColor = surface,
//        selectedContainerColor = ListItemDefaults.colors().selectedContainerColor.copy(alpha = 0.3f),
        // forced colors look shit in high contrast mode
    )
}

@Composable
fun ListItemDefaults.myBigImageShapes() = ListItemDefaults.shapes(
    shape = MaterialTheme.shapes.medium,
    hoveredShape = MaterialTheme.shapes.large,
    focusedShape = MaterialTheme.shapes.extraLarge,
    pressedShape = MaterialTheme.shapes.extraLarge
)

val MenuDefaults.myGroupStandardContainerColor: Color
    @Composable
    get() =
        MenuDefaults.groupStandardContainerColor.makeOpaque()

@Composable
fun OutlinedToggleButtonDefaults.myColors() =
    OutlinedToggleButtonDefaults.colors(
        disabledContainerColor = Color.Transparent,
        checkedContainerColor = FilledTonalToggleButtonDefaults.colors().checkedContainerColor,
        checkedContentColor = FilledTonalToggleButtonDefaults.colors().checkedContentColor,
    )

fun Color.makeOpaque() = if (alpha < 1f) copy(alpha = 1f) else this

@Composable
expect fun ApplyWindowBlur(behind: Int, bg: Int)

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