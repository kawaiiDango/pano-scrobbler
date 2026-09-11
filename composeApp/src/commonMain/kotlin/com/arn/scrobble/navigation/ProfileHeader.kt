package com.arn.scrobble.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.icons.Album
import com.arn.scrobble.icons.ArrowDropDown
import com.arn.scrobble.icons.HelpAutoMirrored
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Mic
import com.arn.scrobble.icons.MusicNote
import com.arn.scrobble.icons.OpenInBrowser
import com.arn.scrobble.icons.PlayArrow
import com.arn.scrobble.icons.Search
import com.arn.scrobble.icons.Settings
import com.arn.scrobble.icons.Today
import com.arn.scrobble.icons.WorkspacePremium
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.PanoDropdownMenu
import com.arn.scrobble.ui.TextWithIcon
import com.arn.scrobble.ui.accountTypeLabel
import com.arn.scrobble.ui.drawSnowflake
import com.arn.scrobble.ui.generateRandomSnowflake
import com.arn.scrobble.ui.shapedClickable
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.get_pro
import pano_scrobbler.composeapp.generated.resources.help
import pano_scrobbler.composeapp.generated.resources.item_options
import pano_scrobbler.composeapp.generated.resources.num_scrobbles_today
import pano_scrobbler.composeapp.generated.resources.profile
import pano_scrobbler.composeapp.generated.resources.reports
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.settings
import java.util.Calendar

@Composable
fun ProfilePopup(
    shown: Boolean,
    onDismiss: () -> Unit,
    user: UserCached,
    drawerData: DrawerData,
    onNavigate: (PanoRoute) -> Unit,
) {
    var profileOptionsShown by remember { mutableStateOf(false) }

    PanoDropdownMenu(
        expanded = shown,
        onDismissRequest = onDismiss,
        position = MenuAnchorPosition.End,
        shadowElevation = 8.dp,
        headerContent = {
            ProfileAvatar(
                user,
                drawerData = drawerData,
                profileOptionsShown = profileOptionsShown,
                onProfileOptionsShown = { profileOptionsShown = it }
            )
        },
        modifier = Modifier
            .widthIn(max = 360.dp)
    ) {

        custom {
            Spacer(
                modifier = Modifier
                    .height(8.dp)
            )

            ProfileTexts(
                user = user,
                drawerData = drawerData,
                onDismiss = onDismiss,
                profileOptionsShown = profileOptionsShown,
                onNavigate = onNavigate,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                if (user.isSelf) {
                    ProfileTopLevelNav(
                        onNavigate = {
                            onDismiss()
                            onNavigate(it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDialogContent(
    onDismiss: () -> Unit,
    user: UserCached,
    drawerData: DrawerData,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    var profileOptionsShown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        ProfileAvatar(
            user,
            drawerData = drawerData,
            profileOptionsShown = profileOptionsShown,
            onProfileOptionsShown = { profileOptionsShown = it }
        )

        ProfileTexts(
            user = user,
            drawerData = drawerData,
            onDismiss = onDismiss,
            profileOptionsShown = profileOptionsShown,
            onNavigate = onNavigate
        ) {
            if (user.isSelf) {
                ProfileTopLevelNav(
                    onNavigate = {
                        onDismiss()
                        onNavigate(it)
                    }
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ProfileAvatar(
    user: UserCached?,
    drawerData: DrawerData?,
    profileOptionsShown: Boolean,
    onProfileOptionsShown: (Boolean) -> Unit,
) {
    val currentAccountType by
    PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }

    val displayName = if (Stuff.isInDemoMode)
        "nobody"
    else
        user?.name ?: BuildKonfig.APP_NAME

    val profilePicUrl by remember(user) { mutableStateOf(user?.largeImage) }
    val drawSnowfall by rememberSaveable {
        val cal = Calendar.getInstance()

        mutableStateOf(
            BuildKonfig.DEBUG ||
                    (cal.get(Calendar.MONTH) == Calendar.DECEMBER && cal.get(Calendar.DAY_OF_MONTH) >= 24) ||
                    (cal.get(Calendar.MONTH) == Calendar.JANUARY && cal.get(Calendar.DAY_OF_MONTH) <= 5)
        )
    }

    AvatarOrInitials(
        avatarUrl = profilePicUrl,
        avatarName = displayName,
        textStyle = MaterialTheme.typography.displayLarge,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(8.dp)
            .heightIn(min = 24.dp, max = 240.dp)
            .weight(1f, fill = false)
            .aspectRatio(1f, true)
            .clip(MaterialTheme.shapes.large)
            .then(
                if (drawSnowfall) {
                    val snowflakes = remember { List(25) { generateRandomSnowflake() } }
                    val infiniteTransition =
                        rememberInfiniteTransition(label = "SnowfallTransition")
                    var size by remember { mutableStateOf(IntSize.Zero) }
                    val offsetY by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1000f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 60000,
                                easing = LinearEasing
                            ),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "Snowfall"
                    )

                    Modifier
                        .onSizeChanged {
                            size = it
                        }
                        .drawWithContent {
                            drawContent()
                            snowflakes.forEach { snowflake ->
                                drawSnowflake(
                                    snowflake,
                                    offsetY % size.height,
                                    size.width,
                                    size.height
                                )
                            }
                        }
                } else
                    Modifier
            )
    )

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(bottom = 8.dp)
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f, false)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )

            Text(
                accountTypeLabel(currentAccountType),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (user?.isSelf == true) {
            OutlinedIconToggleButton(
                shapes = IconButtonDefaults.toggleableShapes(),
                checked = profileOptionsShown,
                onCheckedChange = onProfileOptionsShown,
            ) {
                Icon(
                    Icons.ArrowDropDown,
                    contentDescription = stringResource(Res.string.item_options)
                )
            }
        }

        if (drawerData != null && drawerData.scrobblesToday > 0) {
            TextWithIcon(
                icon = Icons.Today,
                text = pluralStringResource(
                    Res.plurals.num_scrobbles_today,
                    drawerData.scrobblesToday,
                    drawerData.scrobblesToday.format()
                ),
                style = MaterialTheme.typography.bodyMediumEmphasized,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f, false)
            )
        }
    }
}

@Composable
private fun ColumnScope.ProfileTexts(
    user: UserCached?,
    drawerData: DrawerData,
    profileOptionsShown: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
    otherContent: @Composable ColumnScope.() -> Unit,
) {
    val currentAccountType by
    PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }

    if (user?.isSelf == false && !PlatformStuff.isTv) {
        ProfileLinks(
            user = user,
            accountType = currentAccountType,
            onOpenUrl = { url ->
                onDismiss()
                PlatformStuff.openInBrowser(url)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }

    AnimatedContent(
        targetState = profileOptionsShown,
        modifier = modifier
            .padding(bottom = 8.dp)
            .padding(horizontal = 8.dp)
            .align(Alignment.CenterHorizontally)
    ) {
        if (it) {
            ProfileOptions(
                currentUser = user,
                accountType = currentAccountType,
                onDismiss = onDismiss,
                onNavigate = onNavigate,
            )
        } else {
            val textStyle = MaterialTheme.typography.bodyMediumEmphasized
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FlowRow(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        16.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (drawerData.scrobblesTotal > 0) {
                        TextWithIcon(
                            icon = Icons.PlayArrow,
                            text = drawerData.scrobblesTotal.format(),
                            style = textStyle
                        )
                    }

                    if (drawerData.artistCount > 0) {
                        TextWithIcon(
                            icon = Icons.Mic,
                            text = drawerData.artistCount.format(),
                            style = textStyle
                        )
                    }

                    if (drawerData.albumCount > 0) {
                        TextWithIcon(
                            icon = Icons.Album,
                            text = drawerData.albumCount.format(),
                            style = textStyle
                        )
                    }

                    if (drawerData.trackCount > 0) {
                        TextWithIcon(
                            icon = Icons.MusicNote,
                            text = drawerData.trackCount.format(),
                            style = textStyle
                        )
                    }
                }

                otherContent()
            }
        }
    }
}

@Composable
private fun ProfileOptions(
    currentUser: UserCached?,
    accountType: AccountType?,
    onDismiss: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    currentUser ?: return

    val otherAccounts by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { prefs ->
        prefs.scrobbleAccounts
            .distinctBy { it.type }
            .filterNot { it.type == prefs.currentAccountType }
    }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
    ) {
        if (PlatformStuff.isTv) {
            ButtonWithIcon(
                icon = Icons.HelpAutoMirrored,
                text = stringResource(Res.string.help),
                onClick = {
                    onDismiss()
                    onNavigate(PanoRoute.Help())
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
        } else {
            ProfileLinks(
                user = currentUser,
                accountType = accountType,
                onOpenUrl = { url ->
                    onDismiss()
                    PlatformStuff.openInBrowser(url)
                }
            )
        }

        otherAccounts.forEach { account ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shapedClickable {
                        scope.launch {
                            PlatformStuff.mainPrefs.updateData { it.copy(currentAccountType = account.type) }
                            onDismiss()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                AvatarOrInitials(
                    avatarUrl = account.user.largeImage,
                    avatarName = account.user.name,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
                Column {
                    Text(
                        account.user.name,
                        overflow = TextOverflow.MiddleEllipsis,
                        maxLines = 1
                    )
                    Text(
                        accountTypeLabel(account.type),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ProfileLinks(
    user: UserCached,
    accountType: AccountType?,
    onOpenUrl: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
    ) {
        ButtonWithIcon(
            icon = Icons.OpenInBrowser,
            text = stringResource(Res.string.profile),
            onClick = {
                onOpenUrl(user.url)
            }
        )

        if (accountType == AccountType.LASTFM ||
            accountType == AccountType.LIBREFM ||
            accountType == AccountType.LISTENBRAINZ
        ) {
            ButtonWithIcon(
                icon = Icons.OpenInBrowser,
                text = stringResource(Res.string.reports),
                onClick = {
                    val url = when (accountType) {
                        AccountType.LASTFM -> "https://www.last.fm/user/${user.name}/listening-report/week"
                        AccountType.LIBREFM -> "https://libre.fm/user/${user.name}/stats"
                        AccountType.LISTENBRAINZ -> "https://listenbrainz.org/user/${user.name}/reports"
                    }
                    onOpenUrl(url)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.ProfileTopLevelNav(
    onNavigate: (PanoRoute) -> Unit,
) {
    val isLicenseValid = LocalLicenseValidState.current

    if (!isLicenseValid) {
        FilledTonalButton(
            shapes = ButtonDefaults.shapes(),
            onClick = {
                onNavigate(PanoRoute.Billing)
            },
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 4.dp)
        ) {
            Icon(
                Icons.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(Res.string.get_pro), maxLines = 1)
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.align(Alignment.CenterHorizontally),
    ) {
        ButtonWithIcon(
            text = stringResource(Res.string.settings),
            icon = Icons.Settings,
            onClick = {
                onNavigate(PanoRoute.Prefs)
            },
        )

        if (!PlatformStuff.isTv) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above
                ),
                tooltip = { PlainTooltip { Text(stringResource(Res.string.search)) } },
                state = rememberTooltipState(),
            ) {
                OutlinedIconButton(
                    shapes = IconButtonDefaults.shapes(),
                    onClick = {
                        onNavigate(PanoRoute.Search)
                    },
                    border = ButtonDefaults.outlinedButtonBorder(true),
                ) {
                    Icon(
                        imageVector = Icons.Search,
                        contentDescription = stringResource(Res.string.search),
                    )
                }
            }

            ButtonWithIcon(
                text = stringResource(Res.string.help),
                icon = Icons.HelpAutoMirrored,
                onClick = {
                    onNavigate(PanoRoute.Help())
                },
            )
        } else {
            ButtonWithIcon(
                text = stringResource(Res.string.search),
                icon = Icons.Search,
                onClick = {
                    onNavigate(PanoRoute.Search)
                },
            )
        }
    }
}