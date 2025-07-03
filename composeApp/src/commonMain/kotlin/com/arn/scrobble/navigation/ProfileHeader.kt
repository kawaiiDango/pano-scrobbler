package com.arn.scrobble.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.TextWithIcon
import com.arn.scrobble.ui.accountTypeLabel
import com.arn.scrobble.ui.drawSnowflake
import com.arn.scrobble.ui.generateRandomSnowflake
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.help
import pano_scrobbler.composeapp.generated.resources.item_options
import pano_scrobbler.composeapp.generated.resources.num_scrobbles_today
import pano_scrobbler.composeapp.generated.resources.profile
import pano_scrobbler.composeapp.generated.resources.reports


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileHeader(
    otherUser: UserCached?,
    drawerData: DrawerData?,
    compact: Boolean,
    drawSnowfall: Boolean,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentScrobblable by Scrobblables.current.collectAsStateWithLifecycle()

    val displayText = when {
        otherUser != null -> otherUser.name
        Stuff.isInDemoMode -> "nobody"
        currentScrobblable == null -> BuildKonfig.APP_NAME
        currentScrobblable!!.userAccount.type == AccountType.LASTFM -> currentScrobblable!!.userAccount.user.name
        else -> accountTypeLabel(currentScrobblable!!.userAccount.type) + ": " + currentScrobblable!!.userAccount.user.name
    }


    val userName by remember(displayText) {
        mutableStateOf(
            when {
                otherUser != null -> otherUser.name
                else -> currentScrobblable?.userAccount?.user?.name
            }
        )
    }

    val profilePicUrl by remember(currentScrobblable, drawerData, otherUser) {
        mutableStateOf(
            when {
                otherUser != null -> otherUser.largeImage
                currentScrobblable != null && drawerData != null -> drawerData.profilePicUrl
                else -> null
            }
        )
    }


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (compact) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                AvatarOrInitials(
                    avatarUrl = profilePicUrl,
                    avatarName = userName,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                )

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )

                ProfileHeaderDropdown(
                    currentUser = otherUser ?: currentScrobblable?.userAccount?.user,
                    accountType = currentScrobblable?.userAccount?.type,
                    onNavigate = onNavigate,
                )
            }

        } else {
            AvatarOrInitials(
                avatarUrl = profilePicUrl,
                avatarName = userName,
                textStyle = MaterialTheme.typography.displayLarge,
                modifier = Modifier
                    .heightIn(max = 240.dp)
                    .aspectRatio(1f, true)
                    .clip(MaterialTheme.shapes.large)
                    .then(
                        if (drawSnowfall) {
                            val snowflakes = remember { List(50) { generateRandomSnowflake() } }
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
                    .padding(vertical = 8.dp)
            ) {

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f, false)
                )

                ProfileHeaderDropdown(
                    currentUser = otherUser ?: currentScrobblable?.userAccount?.user,
                    accountType = currentScrobblable?.userAccount?.type,
                    onNavigate = onNavigate,
                )

                if (drawerData != null && drawerData.scrobblesToday > 0) {
                    TextWithIcon(
                        icon = Icons.Outlined.Today,
                        text = pluralStringResource(
                            Res.plurals.num_scrobbles_today,
                            drawerData.scrobblesToday,
                            drawerData.scrobblesToday.format()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f, false)
                    )
                }
            }
        }

        drawerData?.let { drawerData ->
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                if (drawerData.scrobblesTotal > 0) {
                    TextWithIcon(
                        icon = Icons.Outlined.PlayArrow,
                        text = drawerData.scrobblesTotal.format(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (drawerData.artistCount > 0) {
                    TextWithIcon(
                        icon = Icons.Outlined.Mic,
                        text = drawerData.artistCount.format(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (drawerData.albumCount > 0) {
                    TextWithIcon(
                        icon = Icons.Outlined.Album,
                        text = drawerData.albumCount.format(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (drawerData.trackCount > 0) {
                    TextWithIcon(
                        icon = Icons.Outlined.MusicNote,
                        text = drawerData.trackCount.format(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderDropdown(
    currentUser: UserCached?,
    accountType: AccountType?,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    currentUser ?: return

    val accounts by Scrobblables.all.collectAsStateWithLifecycle()
    var dropDownShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
    ) {
        FilledTonalIconButton(
            onClick = { dropDownShown = true },
        ) {
            Icon(
                Icons.Outlined.ArrowDropDown,
                contentDescription = stringResource(Res.string.item_options)
            )
        }
        DropdownMenu(
            expanded = dropDownShown,
            onDismissRequest = { dropDownShown = false },
        ) {
            if (currentUser.isSelf) {
                val accountsFiltered = accounts.filterNot { it == Scrobblables.current.value }
                accountsFiltered.forEach {
                    DropdownMenuItem(
                        text = {
                            Text(
                                accountTypeLabel(it.userAccount.type) + ": " + it.userAccount.user.name,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.AccountCircle,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            scope.launch {
                                Scrobblables.setCurrent(it.userAccount.type)
                            }
                            dropDownShown = false
                        },
                    )
                }

                if (accountsFiltered.isNotEmpty())
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
            }

            if (!PlatformStuff.isTv) {

                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.profile)) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.OpenInBrowser,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        PlatformStuff.openInBrowser(currentUser.url)
                        dropDownShown = false
                    }
                )

                if (accountType == AccountType.LASTFM ||
                    accountType == AccountType.LIBREFM ||
                    accountType == AccountType.LISTENBRAINZ
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(Res.string.reports),
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.OpenInBrowser,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            val url = when (accountType) {
                                AccountType.LASTFM -> "https://www.last.fm/user/${currentUser.name}/listening-report/week"
                                AccountType.LIBREFM -> "https://libre.fm/user/${currentUser.name}/stats"
                                AccountType.LISTENBRAINZ -> "https://listenbrainz.org/user/${currentUser.name}/reports"
                                else -> null
                            }
                            dropDownShown = false
                            PlatformStuff.openInBrowser(url ?: return@DropdownMenuItem)
                        }
                    )
                }
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.help)) },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onNavigate(PanoRoute.Help)
                        dropDownShown = false
                    }
                )
            }
        }
    }
}