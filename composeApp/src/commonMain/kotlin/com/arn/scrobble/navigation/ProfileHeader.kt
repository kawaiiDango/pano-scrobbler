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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
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
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.icons.AccountCircle
import com.arn.scrobble.icons.Album
import com.arn.scrobble.icons.ArrowDropDown
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Mic
import com.arn.scrobble.icons.MusicNote
import com.arn.scrobble.icons.OpenInBrowser
import com.arn.scrobble.icons.PlayArrow
import com.arn.scrobble.icons.Today
import com.arn.scrobble.icons.automirrored.Help
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.TextWithIcon
import com.arn.scrobble.ui.accountTypeLabel
import com.arn.scrobble.ui.drawSnowflake
import com.arn.scrobble.ui.generateRandomSnowflake
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
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
    user: UserCached?,
    drawerData: DrawerData?,
    compact: Boolean,
    drawSnowfall: Boolean,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentAccountType by
    PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }

    val userName = user?.name ?: BuildKonfig.APP_NAME

    val displayText = when {
        Stuff.isInDemoMode -> "nobody"
        currentAccountType == AccountType.LASTFM -> userName
        else -> accountTypeLabel(currentAccountType) + ": " + userName
    }


    val profilePicUrl by remember(drawerData, user) {
        mutableStateOf(
            when {
                user != null && !user.isSelf -> user.largeImage
                drawerData != null -> drawerData.profilePicUrl
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

                if (user?.isSelf == true) {
                    ProfileHeaderDropdown(
                        currentUser = user,
                        accountType = currentAccountType,
                        onNavigate = onNavigate,
                    )
                }
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

                if (user?.isSelf == true) {
                    ProfileHeaderDropdown(
                        currentUser = user,
                        accountType = currentAccountType,
                        onNavigate = onNavigate,
                    )
                }

                if (drawerData != null && drawerData.scrobblesToday > 0) {
                    TextWithIcon(
                        icon = Icons.Today,
                        text = pluralStringResource(
                            Res.plurals.num_scrobbles_today,
                            drawerData.scrobblesToday,
                            drawerData.scrobblesToday.format()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
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
                        icon = Icons.PlayArrow,
                        text = drawerData.scrobblesTotal.format(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (drawerData.artistCount > 0) {
                    TextWithIcon(
                        icon = Icons.Mic,
                        text = drawerData.artistCount.format(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (drawerData.albumCount > 0) {
                    TextWithIcon(
                        icon = Icons.Album,
                        text = drawerData.albumCount.format(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (drawerData.trackCount > 0) {
                    TextWithIcon(
                        icon = Icons.MusicNote,
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

    val otherAccounts by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { prefs ->
        prefs.scrobbleAccounts
            .distinctBy { it.type }
            .filterNot { it.type == prefs.currentAccountType }
    }
    var dropDownShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
    ) {
        OutlinedIconToggleButton(
            checked = dropDownShown,
            onCheckedChange = { dropDownShown = it },
        ) {
            Icon(
                Icons.ArrowDropDown,
                contentDescription = stringResource(Res.string.item_options)
            )
        }
        DropdownMenu(
            expanded = dropDownShown,
            onDismissRequest = { dropDownShown = false },
        ) {
            if (currentUser.isSelf) {
                otherAccounts.forEach { account ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                accountTypeLabel(account.type) + ": " + account.user.name,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.AccountCircle,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            scope.launch {
                                PlatformStuff.mainPrefs.updateData { it.copy(currentAccountType = account.type) }
                            }
                            dropDownShown = false
                        },
                    )
                }

                if (otherAccounts.isNotEmpty())
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
            }

            if (!PlatformStuff.isTv) {

                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.profile)) },
                    leadingIcon = {
                        Icon(
                            Icons.OpenInBrowser,
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
                                Icons.OpenInBrowser,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            val url = when (accountType) {
                                AccountType.LASTFM -> "https://www.last.fm/user/${currentUser.name}/listening-report/week"
                                AccountType.LIBREFM -> "https://libre.fm/user/${currentUser.name}/stats"
                                AccountType.LISTENBRAINZ -> "https://listenbrainz.org/user/${currentUser.name}/reports"
                            }
                            dropDownShown = false
                            PlatformStuff.openInBrowser(url)
                        }
                    )
                }
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.help)) },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Help,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onNavigate(PanoRoute.Help())
                        dropDownShown = false
                    }
                )
            }
        }
    }
}