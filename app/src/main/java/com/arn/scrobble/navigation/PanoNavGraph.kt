package com.arn.scrobble.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.arn.scrobble.HomePagerScreen
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.billing.BillingScreen
import com.arn.scrobble.billing.BillingTroubleshootScreen
import com.arn.scrobble.charts.ChartsLegendScreen
import com.arn.scrobble.charts.ChartsPagerScreen
import com.arn.scrobble.charts.HiddenTagsScreen
import com.arn.scrobble.charts.RandomScreen
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.edits.BlockedMetadatasScreen
import com.arn.scrobble.edits.RegexEditsAddScreen
import com.arn.scrobble.edits.RegexEditsScreen
import com.arn.scrobble.edits.RegexEditsTestScreen
import com.arn.scrobble.edits.SimpleEditsAddScreen
import com.arn.scrobble.edits.SimpleEditsScreen
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.help.HelpScreen
import com.arn.scrobble.info.InfoPagerScreen
import com.arn.scrobble.info.SimilarTracksScreen
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.mic.MicScrobbleScreen
import com.arn.scrobble.onboarding.ChangelogScreen
import com.arn.scrobble.onboarding.FileLoginScreen
import com.arn.scrobble.onboarding.FixItScreen
import com.arn.scrobble.onboarding.GnufmLoginScreen
import com.arn.scrobble.onboarding.ListenBrainzLoginScreen
import com.arn.scrobble.onboarding.MalojaLoginScreen
import com.arn.scrobble.onboarding.OnboardingScreen
import com.arn.scrobble.onboarding.PleromaLoginScreen
import com.arn.scrobble.onboarding.WebViewScreen
import com.arn.scrobble.pref.AppListScreen
import com.arn.scrobble.pref.DeleteAccountScreen
import com.arn.scrobble.pref.ExportScreen
import com.arn.scrobble.pref.ImportScreen
import com.arn.scrobble.pref.OssCreditsScreen
import com.arn.scrobble.pref.PrefsScreen
import com.arn.scrobble.pref.TranslatorsScreen
import com.arn.scrobble.recents.TrackHistoryScreen
import com.arn.scrobble.search.ImageSearchScreen
import com.arn.scrobble.search.IndexingScreen
import com.arn.scrobble.search.SearchScreen
import com.arn.scrobble.themes.ThemeChooserScreen
import com.arn.scrobble.ui.addColumnPadding
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.typeOf

fun NavGraphBuilder.panoNavGraph(
    onSetTitle: (String?) -> Unit,
    onSetOtherUser: (UserCached?) -> Unit,
    navMetadataList: () -> List<PanoNavMetadata>?,
    onSetNavMetadataList: (List<PanoNavMetadata>) -> Unit,
    tabIdxFlow: Flow<Int>,
    onSetTabIdx: (Int) -> Unit,
    navigate: (PanoRoute) -> Unit,
    onLoginChanged: () -> Unit,
    goBack: () -> Unit,
    goUp: () -> Unit,
    mainViewModel: MainViewModel,
) {

    fun onSetTitleRes(resId: Int?) {
        onSetTitle(resId?.let { PlatformStuff.application.getString(it) })
    }

    @Composable
    fun modifier() = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)

    composable<PanoRoute.HomePager>(
        typeMap = mapOf(
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        val tabIdx by tabIdxFlow.collectAsStateWithLifecycle(null)
        val arguments = it.toRoute<PanoRoute.HomePager>()

        if (tabIdx != null) {
            HomePagerScreen(
                user = arguments.user,
                onSetOtherUser = onSetOtherUser,
                tabsList = getTabData(it.destination) ?: emptyList(),
                onTitleChange = onSetTitle,
                tabIdx = tabIdx!!,
                onSetTabIdx = onSetTabIdx,
                onSetNavMetadataList = onSetNavMetadataList,
                onNavigate = navigate,
                modifier = modifier()
            )
        }
    }

    composable<PanoRoute.Random>(
        typeMap = mapOf(
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        onSetTitleRes(R.string.random)
        val arguments = it.toRoute<PanoRoute.Random>()

        RandomScreen(
            user = arguments.user,
            onNavigate = navigate,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.OssCredits> {
        onSetTitleRes(R.string.pref_oss_credits)
        OssCreditsScreen(
            modifier = modifier()
        )
    }

    composable<PanoRoute.AppList> {
        val arguments = it.toRoute<PanoRoute.AppList>()
        onSetTitleRes(
            if (arguments.isSingleSelect)
                R.string.choose_an_app
            else
                R.string.choose_apps
        )

        AppListScreen(
            isSingleSelect = arguments.isSingleSelect,
            hasPreSelection = arguments.hasPreSelection,
            preSelectedPackages = arguments.preSelectedPackages.toSet(),
            onSetSelectedPackages = { mainViewModel.setSelectedPackages(it) },
            modifier = modifier()
        )
    }

    composable<PanoRoute.ThemeChooser> {
        onSetTitleRes(R.string.pref_themes)
        ThemeChooserScreen(
            onNavigateToBilling = { navigate(PanoRoute.Billing) },
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.DeleteAccount> {
        onSetTitleRes(R.string.delete_account)
        DeleteAccountScreen(
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.BillingTroubleshoot> {
        onSetTitleRes(R.string.billing_troubleshoot)
        BillingTroubleshootScreen(
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.Billing> {
        onSetTitle(null)
        BillingScreen(
            onBack = goBack,
            onNavigateToTroubleshoot = { navigate(PanoRoute.BillingTroubleshoot) },
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.Prefs>(
        deepLinks = listOf(
            navDeepLink {
                uriPattern = Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.Prefs::class.simpleName
            },
            navDeepLink {
                action = "android.service.quicksettings.action.QS_TILE_PREFERENCES"
            }
        )
    ) {
        onSetTitleRes(R.string.settings)
        PrefsScreen(
            onNavigate = navigate,
            modifier = modifier()
        )
    }

    composable<PanoRoute.SimpleEdits> {
        onSetTitleRes(R.string.simple_edits)
        SimpleEditsScreen(
            onEdit = { navigate(PanoRoute.SimpleEditsAdd(it)) },
            modifier = modifier()
        )
    }

    composable<PanoRoute.SimpleEditsAdd>(
        typeMap = mapOf(typeOf<SimpleEdit?>() to serializableType<SimpleEdit?>())
    ) {
        onSetTitleRes(R.string.edit)

        val arguments = it.toRoute<PanoRoute.SimpleEditsAdd>()

        SimpleEditsAddScreen(
            simpleEdit = arguments.simpleEdit,
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.RegexEdits> {
        onSetTitleRes(R.string.pref_regex_edits)
        RegexEditsScreen(
            onNavigateToTest = { navigate(PanoRoute.RegexEditsTest) },
            onNavigateToEdit = { navigate(PanoRoute.RegexEditsAdd(it)) },
            modifier = modifier()
        )
    }

    composable<PanoRoute.RegexEditsAdd>(
        typeMap = mapOf(typeOf<RegexEdit?>() to serializableType<RegexEdit?>())
    ) {
        onSetTitleRes(R.string.edit_regex)

        val arguments = it.toRoute<PanoRoute.RegexEditsAdd>()

        RegexEditsAddScreen(
            mainViewModel = mainViewModel,
            regexEdit = arguments.regexEdit,
            onNavigate = navigate,
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.RegexEditsTest> {
        onSetTitleRes(R.string.edit_regex_test)

        RegexEditsTestScreen(
            mainViewModel = mainViewModel,
            onNavigateToAppList = {
                navigate(
                    PanoRoute.AppList(
                        hasPreSelection = true,
                        preSelectedPackages = emptyList(),
                        isSingleSelect = true
                    )
                )
            },
            onNavigateToRegexEditsAdd = { navigate(PanoRoute.RegexEditsAdd(it)) },
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.BlockedMetadatas> {
        onSetTitleRes(R.string.pref_blocked_metadata)

        BlockedMetadatasScreen(
            onEdit = {
                navigate(
                    PanoRoute.BlockedMetadataAdd(
                        it,
                        ignoredArtist = null,
                        hash = null
                    )
                )
            },
            modifier = modifier()
        )
    }

    composable<PanoRoute.ImageSearch>(
        deepLinks = listOf(
            navDeepLink<PanoRoute.ImageSearch>(
                basePath = Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.ImageSearch::class.simpleName,
                typeMap = mapOf(
                    typeOf<Artist?>() to serializableType<Artist?>(),
                    typeOf<Album?>() to serializableType<Album?>(),
                )
            )
        ),
        typeMap = mapOf(
            typeOf<Artist?>() to serializableType<Artist?>(),
            typeOf<Album?>() to serializableType<Album?>(),
        )
    ) {
        onSetTitleRes(R.string.search)

        val arguments = it.toRoute<PanoRoute.ImageSearch>()

        ImageSearchScreen(
            onBack = goBack,
            artist = arguments.artist,
            originalArtist = arguments.originalArtist,
            album = arguments.album,
            originalAlbum = arguments.originalAlbum,
            modifier = modifier()
        )
    }

    composable<PanoRoute.Translators> {
        onSetTitleRes(R.string.pref_translate_credits)

        TranslatorsScreen(
            modifier = modifier()
        )
    }

    composable<PanoRoute.Import> {
        onSetTitleRes(R.string.pref_import)

        ImportScreen(
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.Export> {
        onSetTitleRes(R.string.pref_export)

        ExportScreen(
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginFile> {
        onSetTitleRes(R.string.scrobble_to_file)

        FileLoginScreen(
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginGnufm> {
        onSetTitleRes(R.string.gnufm)

        GnufmLoginScreen(
            onDone = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginListenBrainz> {
        onSetTitleRes(R.string.listenbrainz)

        ListenBrainzLoginScreen(
            onDone = goBack,
            hasCustomApiRoot = false,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginCustomListenBrainz> {
        onSetTitleRes(R.string.custom_listenbrainz)

        ListenBrainzLoginScreen(
            onDone = goBack,
            hasCustomApiRoot = true,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginMaloja> {
        onSetTitleRes(R.string.maloja)

        MalojaLoginScreen(
            onDone = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginPleroma> {
        onSetTitleRes(R.string.pleroma)

        PleromaLoginScreen(
            onNavigateToWebview = { url, userAccountTemp, creds ->
                navigate(PanoRoute.WebView(url, userAccountTemp, creds))
            },
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.WebView>(
        typeMap = mapOf(
            typeOf<UserAccountTemp?>() to serializableType<UserAccountTemp?>(),
            typeOf<PleromaOauthClientCreds?>() to serializableType<PleromaOauthClientCreds?>()
        )
    ) {
        val arguments = it.toRoute<PanoRoute.WebView>()

        WebViewScreen(
            initialUrl = arguments.url,
            userAccountTemp = arguments.userAccountTemp,
            creds = arguments.creds,
            onTitleChange = onSetTitle,
            onBack = goBack,
            modifier = modifier().padding(panoContentPadding())
            // webview has issues with nested scroll
        )
    }

    composable<PanoRoute.Search>(
        deepLinks = listOf(
            navDeepLink {
                uriPattern = Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.Search::class.simpleName
            },
            navDeepLink {
                action = "android.intent.action.SEARCH"
            }
        )
    ) {
        onSetTitleRes(R.string.search)

        SearchScreen(
            onNavigate = navigate,
            modifier = modifier()
        )
    }


    composable<PanoRoute.Onboarding> {
        onSetTitle(null)

        OnboardingScreen(
            onNavigate = navigate,
            onDone = {
                onLoginChanged()
            },
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.MicScrobble>(
        deepLinks = listOf(
            navDeepLink {
                uriPattern =
                    Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.MicScrobble::class.simpleName
            },
        )
    ) {
        onSetTitleRes(R.string.scrobble_from_mic)

        MicScrobbleScreen(
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.Help> {
        onSetTitleRes(R.string.help)

        HelpScreen(
            modifier = modifier().padding(panoContentPadding())
            // webview has issues with nested scroll
        )
    }

    composable<PanoRoute.MusicEntryInfoPager>(
        deepLinks = listOf(
            navDeepLink<PanoRoute.MusicEntryInfoPager>(
                basePath = Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.MusicEntryInfoPager::class.simpleName,
                typeMap = mapOf(
                    typeOf<Artist>() to serializableType<Artist>(),
                    typeOf<UserCached>() to serializableType<UserCached>()
                )
            )
        ),
        typeMap = mapOf(
            typeOf<Artist>() to serializableType<Artist>(),
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        val tabIdx by tabIdxFlow.collectAsStateWithLifecycle(null)

        val arguments = it.toRoute<PanoRoute.MusicEntryInfoPager>()

        onSetTitle(arguments.artist.name)

        tabIdx?.let { tabIdx ->
            InfoPagerScreen(
                musicEntry = arguments.artist,
                user = arguments.user,
                pkgName = arguments.pkgName,
                tabIdx = tabIdx,
                tabsList = getTabData(it.destination) ?: emptyList(),
                onSetTabIdx = onSetTabIdx,
                initialTabIdx = when (arguments.type) {
                    Stuff.TYPE_TRACKS -> 0
                    Stuff.TYPE_ALBUMS -> 1
                    Stuff.TYPE_ARTISTS -> 2
                    else -> 0
                },
                onNavigate = navigate,
                modifier = modifier()
            )
        }
    }


    composable<PanoRoute.ChartsPager>(
        typeMap = mapOf(
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        val tabIdx by tabIdxFlow.collectAsStateWithLifecycle(null)

        val arguments = it.toRoute<PanoRoute.ChartsPager>()

        tabIdx?.let { tabIdx ->
            ChartsPagerScreen(
                user = arguments.user,
                tabIdx = tabIdx,
                onSetTabIdx = onSetTabIdx,
                tabsList = getTabData(it.destination) ?: emptyList(),
                initialTabIdx = when (arguments.type) {
                    Stuff.TYPE_ARTISTS -> 0
                    Stuff.TYPE_ALBUMS -> 1
                    Stuff.TYPE_TRACKS -> 2
                    else -> 0
                },
                onTitleChange = onSetTitle,
                onSetNavMetadataList = onSetNavMetadataList,
                onNavigate = navigate,
                modifier = modifier()
            )
        }
    }

    composable<PanoRoute.SimilarTracks>(
        deepLinks = listOf(
            navDeepLink<PanoRoute.SimilarTracks>(
                basePath = Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.SimilarTracks::class.simpleName,
                typeMap = mapOf(
                    typeOf<Track>() to serializableType<Track>(),
                    typeOf<UserCached>() to serializableType<UserCached>()
                )
            )
        ),
        typeMap = mapOf(
            typeOf<Track>() to serializableType<Track>(),
            typeOf<UserCached>() to serializableType<UserCached>(),
        )
    ) {
        val arguments = it.toRoute<PanoRoute.SimilarTracks>()
        val entry = arguments.track
        val title = stringResource(
            R.string.artist_title,
            entry.artist.name,
            entry.name
        )

        onSetTitle(title)

        SimilarTracksScreen(
            musicEntry = arguments.track,
            user = arguments.user,
            pkgName = arguments.pkgName,
            onNavigate = navigate,
            modifier = modifier()
        )
    }

    composable<PanoRoute.Help> {
        onSetTitleRes(R.string.help)

        HelpScreen(
            modifier = modifier().padding(panoContentPadding())
            // webview has issues with nested scroll
        )
    }

    composable<PanoRoute.TrackHistory>(
        deepLinks = listOf(
            navDeepLink<PanoRoute.TrackHistory>(
                basePath = Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.TrackHistory::class.simpleName,
                typeMap = mapOf(
                    typeOf<Track>() to serializableType<Track>(),
                    typeOf<UserCached>() to serializableType<UserCached>()
                )
            )
        ),
        typeMap = mapOf(
            typeOf<Track>() to serializableType<Track>(),
            typeOf<UserCached>() to serializableType<UserCached>(),
        )
    ) {
        val arguments = it.toRoute<PanoRoute.TrackHistory>()
        val track = arguments.track
        val user = arguments.user

        val formattedCount = track.userplaycount?.format() ?: "0"
        val title = if (user.isSelf) {
            stringResource(R.string.my_scrobbles) + ": " + formattedCount
        } else {
            "${user.name}: $formattedCount"
        }
        onSetTitle(title)

        TrackHistoryScreen(
            track = track,
            user = user,
            onNavigate = navigate,
            modifier = modifier()
        )
    }

    dialog<PanoRoute.NavPopup>(
        typeMap = mapOf(
            typeOf<UserCached?>() to serializableType<UserCached?>(),
        )
    ) {
        val arguments = it.toRoute<PanoRoute.NavPopup>()

        NavPopupScreen(
            onDismiss = goUp,
            otherUser = arguments.otherUser,
            drawerDataFlow = mainViewModel.drawerDataFlow,
            drawSnowfall = mainViewModel.isItChristmas,
            loadOtherUserDrawerData = mainViewModel::loadOtherUserDrawerData,
            navMetadataList = navMetadataList() ?: emptyList(),
            onNavigate = navigate
        )
    }

    dialog<PanoRoute.Changelog> {
        ChangelogScreen(
            onDismiss = goUp
        )
    }

    dialog<PanoRoute.FixIt> {
        FixItScreen(
            onDismiss = goUp
        )
    }

    dialog<PanoRoute.Index> {
        IndexingScreen(
            onDismiss = goUp
        )
    }

    dialog<PanoRoute.HiddenTags> {
        HiddenTagsScreen(
            onDismiss = goUp
        )
    }

    dialog<PanoRoute.ChartsLegend> {
        ChartsLegendScreen(
            onDismiss = goUp
        )
    }

    panoDialogNavGraph(
        navigate = navigate,
        goUp = goUp,
        usingInDialogActivity = false,
        mainViewModel = mainViewModel,
    )
}
