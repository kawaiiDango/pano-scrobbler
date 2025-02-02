package com.arn.scrobble.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.UserCached
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
import com.arn.scrobble.help.HelpScreen
import com.arn.scrobble.info.InfoPagerScreen
import com.arn.scrobble.info.SimilarTracksScreen
import com.arn.scrobble.main.HomePagerScreen
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.onboarding.ChangelogScreen
import com.arn.scrobble.onboarding.FileLoginScreen
import com.arn.scrobble.onboarding.GnufmLoginScreen
import com.arn.scrobble.onboarding.ListenBrainzLoginScreen
import com.arn.scrobble.onboarding.MalojaLoginScreen
import com.arn.scrobble.onboarding.OnboardingScreen
import com.arn.scrobble.onboarding.OobLibrefmLoginScreen
import com.arn.scrobble.onboarding.OobPleromaLoginScreen
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
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.billing_troubleshoot_title
import pano_scrobbler.composeapp.generated.resources.choose_an_app
import pano_scrobbler.composeapp.generated.resources.choose_apps
import pano_scrobbler.composeapp.generated.resources.custom_listenbrainz
import pano_scrobbler.composeapp.generated.resources.delete_account
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.edit_regex
import pano_scrobbler.composeapp.generated.resources.edit_regex_test
import pano_scrobbler.composeapp.generated.resources.gnufm
import pano_scrobbler.composeapp.generated.resources.help
import pano_scrobbler.composeapp.generated.resources.listenbrainz
import pano_scrobbler.composeapp.generated.resources.maloja
import pano_scrobbler.composeapp.generated.resources.my_scrobbles
import pano_scrobbler.composeapp.generated.resources.pleroma
import pano_scrobbler.composeapp.generated.resources.pref_blocked_metadata
import pano_scrobbler.composeapp.generated.resources.pref_export
import pano_scrobbler.composeapp.generated.resources.pref_import
import pano_scrobbler.composeapp.generated.resources.pref_login
import pano_scrobbler.composeapp.generated.resources.pref_oss_credits
import pano_scrobbler.composeapp.generated.resources.pref_regex_edits
import pano_scrobbler.composeapp.generated.resources.pref_themes
import pano_scrobbler.composeapp.generated.resources.pref_translate_credits
import pano_scrobbler.composeapp.generated.resources.random_text
import pano_scrobbler.composeapp.generated.resources.scrobble_to_file
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.settings
import pano_scrobbler.composeapp.generated.resources.simple_edits
import kotlin.reflect.typeOf

fun NavGraphBuilder.panoNavGraph(
    onSetTitle: (String?) -> Unit,
    onSetOtherUser: (UserCached?) -> Unit,
    navMetadataList: () -> List<PanoNavMetadata>?,
    onSetNavMetadataList: (List<PanoNavMetadata>) -> Unit,
    tabIdxFlow: Flow<Int>,
    tabDataFlow: Flow<List<PanoTabs>?>,
    onSetTabIdx: (Int) -> Unit,
    navigate: (PanoRoute) -> Unit,
    onOnboardingFinished: () -> Unit,
    goBack: () -> Unit,
    goUp: () -> Unit,
    mainViewModel: MainViewModel,
) {

    @Composable
    fun onSetTitleRes(resId: StringResource?) {
        onSetTitle(resId?.let { stringResource(it) })
    }

    @Composable
    fun modifier() = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)

    composable<PanoRoute.SelfHomePager>(
        typeMap = mapOf(
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        val currentUserSelf by PlatformStuff.mainPrefs.data
            .collectAsStateWithInitialValue { prefs ->
                prefs.scrobbleAccounts
                    .firstOrNull { it.type == prefs.currentAccountType }?.user
            }

        val initialTab by PlatformStuff.mainPrefs.data
            .collectAsStateWithInitialValue { it.lastHomePagerTab }
        val tabIdx by tabIdxFlow.collectAsStateWithLifecycle(null)
        val tabData by tabDataFlow.collectAsStateWithLifecycle(null)

        if (currentUserSelf != null && tabData != null && tabIdx != null) {
            HomePagerScreen(
                user = currentUserSelf!!,
                onSetOtherUser = onSetOtherUser,
                tabsList = tabData!!,
                onTitleChange = onSetTitle,
                tabIdx = tabIdx ?: initialTab,
                initialTabIdx = initialTab,
                onSetTabIdx = onSetTabIdx,
                onSetNavMetadataList = onSetNavMetadataList,
                onNavigate = navigate,
                modifier = modifier()
            )
        }
    }

    composable<PanoRoute.OthersHomePager>(
        typeMap = mapOf(
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        val tabIdx by tabIdxFlow.collectAsStateWithLifecycle(null)
        val arguments = it.toRoute<PanoRoute.OthersHomePager>()
        val tabData by tabDataFlow.collectAsStateWithLifecycle(null)

        if (tabIdx != null && tabData != null) {
            HomePagerScreen(
                user = arguments.user,
                onSetOtherUser = onSetOtherUser,
                tabsList = tabData!!,
                onTitleChange = onSetTitle,
                initialTabIdx = 0,
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
        onSetTitleRes(Res.string.random_text)
        val arguments = it.toRoute<PanoRoute.Random>()

        RandomScreen(
            user = arguments.user,
            onNavigate = navigate,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.OssCredits> {
        onSetTitleRes(Res.string.pref_oss_credits)
        OssCreditsScreen(
            modifier = modifier()
        )
    }

    composable<PanoRoute.AppList> {
        val arguments = it.toRoute<PanoRoute.AppList>()
        onSetTitleRes(
            if (arguments.isSingleSelect)
                Res.string.choose_an_app
            else
                Res.string.choose_apps
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
        onSetTitleRes(Res.string.pref_themes)
        ThemeChooserScreen(
            onNavigateToBilling = { navigate(PanoRoute.Billing) },
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.DeleteAccount> {
        onSetTitleRes(Res.string.delete_account)
        DeleteAccountScreen(
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.BillingTroubleshoot> {
        onSetTitleRes(Res.string.billing_troubleshoot_title)
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
            viewModel = mainViewModel,
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
        onSetTitleRes(Res.string.settings)
        PrefsScreen(
            onNavigate = navigate,
            modifier = modifier()
        )
    }

    composable<PanoRoute.SimpleEdits> {
        onSetTitleRes(Res.string.simple_edits)
        SimpleEditsScreen(
            onEdit = { navigate(PanoRoute.SimpleEditsAdd(it)) },
            modifier = modifier()
        )
    }

    composable<PanoRoute.SimpleEditsAdd>(
        typeMap = mapOf(typeOf<SimpleEdit?>() to serializableType<SimpleEdit?>())
    ) {
        onSetTitleRes(Res.string.edit)

        val arguments = it.toRoute<PanoRoute.SimpleEditsAdd>()

        SimpleEditsAddScreen(
            simpleEdit = arguments.simpleEdit,
            onBack = goBack,
            modifier = modifier().padding(panoContentPadding())
        )
    }

    composable<PanoRoute.RegexEdits> {
        onSetTitleRes(Res.string.pref_regex_edits)
        RegexEditsScreen(
            onNavigateToTest = { navigate(PanoRoute.RegexEditsTest) },
            onNavigateToEdit = { navigate(PanoRoute.RegexEditsAdd(it)) },
            modifier = modifier()
        )
    }

    composable<PanoRoute.RegexEditsAdd>(
        typeMap = mapOf(typeOf<RegexEdit?>() to serializableType<RegexEdit?>())
    ) {
        onSetTitleRes(Res.string.edit_regex)

        val arguments = it.toRoute<PanoRoute.RegexEditsAdd>()

        RegexEditsAddScreen(
            mainViewModel = mainViewModel,
            regexEdit = arguments.regexEdit,
            onNavigate = navigate,
            onBack = goBack,
            modifier = modifier().padding(panoContentPadding())
        )
    }

    composable<PanoRoute.RegexEditsTest> {
        onSetTitleRes(Res.string.edit_regex_test)

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
        onSetTitleRes(Res.string.pref_blocked_metadata)

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
        onSetTitleRes(Res.string.search)

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
        onSetTitleRes(Res.string.pref_translate_credits)

        TranslatorsScreen(
            modifier = modifier()
        )
    }

    composable<PanoRoute.Import> {
        onSetTitleRes(Res.string.pref_import)

        ImportScreen(
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.Export> {
        onSetTitleRes(Res.string.pref_export)

        ExportScreen(
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginFile> {
        onSetTitleRes(Res.string.scrobble_to_file)

        FileLoginScreen(
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginGnufm> {
        onSetTitleRes(Res.string.gnufm)

        GnufmLoginScreen(
            onDone = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginListenBrainz> {
        onSetTitleRes(Res.string.listenbrainz)

        ListenBrainzLoginScreen(
            onDone = goBack,
            hasCustomApiRoot = false,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginCustomListenBrainz> {
        onSetTitleRes(Res.string.custom_listenbrainz)

        ListenBrainzLoginScreen(
            onDone = goBack,
            hasCustomApiRoot = true,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginMaloja> {
        onSetTitleRes(Res.string.maloja)

        MalojaLoginScreen(
            onDone = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginPleroma> {
        onSetTitleRes(Res.string.pleroma)

        PleromaLoginScreen(
            onBackAndThenNavigate = {
                goBack()
                navigate(it)
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

    composable<PanoRoute.OobPleromaAuth>(
        typeMap = mapOf(
            typeOf<UserAccountTemp>() to serializableType<UserAccountTemp>(),
            typeOf<PleromaOauthClientCreds>() to serializableType<PleromaOauthClientCreds>()
        )
    ) {
        onSetTitleRes(Res.string.pleroma)

        val arguments = it.toRoute<PanoRoute.OobPleromaAuth>()

        OobPleromaLoginScreen(
            url = arguments.url,
            userAccountTemp = arguments.userAccountTemp,
            pleromaCreds = arguments.creds,
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.OobLibreFmAuth>(
        typeMap = mapOf(
            typeOf<UserAccountTemp>() to serializableType<UserAccountTemp>(),
        )
    ) {
        onSetTitleRes(Res.string.pref_login)

        val arguments = it.toRoute<PanoRoute.OobLibreFmAuth>()

        OobLibrefmLoginScreen(
            userAccountTemp = arguments.userAccountTemp,
            onBack = goBack,
            modifier = modifier().addColumnPadding()
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
        onSetTitleRes(Res.string.search)

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
                onOnboardingFinished()
            },
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.Help> {
        onSetTitleRes(Res.string.help)

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
        val tabData by tabDataFlow.collectAsStateWithLifecycle(null)
        val arguments = it.toRoute<PanoRoute.MusicEntryInfoPager>()

        onSetTitle(arguments.artist.name)

        if (tabIdx != null && tabData != null) {
            InfoPagerScreen(
                musicEntry = arguments.artist,
                user = arguments.user,
                pkgName = arguments.pkgName,
                tabIdx = tabIdx!!,
                tabsList = tabData!!,
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
        val tabData by tabDataFlow.collectAsStateWithLifecycle(null)
        val arguments = it.toRoute<PanoRoute.ChartsPager>()

        if (tabIdx != null && tabData != null) {
            ChartsPagerScreen(
                user = arguments.user,
                tabIdx = tabIdx!!,
                onSetTabIdx = onSetTabIdx,
                tabsList = tabData!!,
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
        val title = Stuff.formatBigHyphen(
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
        onSetTitleRes(Res.string.help)

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
            stringResource(Res.string.my_scrobbles) + ": " + formattedCount
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
        calledFromDialogActivity = false,
        mainViewModel = mainViewModel,
    )

    panoPlatformSpecificNavGraph(
        onSetTitle = onSetTitle,
        navigate = navigate,
        goUp = goUp,
        mainViewModel = mainViewModel,
    )
}
