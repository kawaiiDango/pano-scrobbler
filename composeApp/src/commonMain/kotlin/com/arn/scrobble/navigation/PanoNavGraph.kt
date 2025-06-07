package com.arn.scrobble.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.billing.BillingScreen
import com.arn.scrobble.billing.BillingTroubleshootScreen
import com.arn.scrobble.charts.ChartsPagerScreen
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
import com.arn.scrobble.onboarding.FileLoginScreen
import com.arn.scrobble.onboarding.GnufmLoginScreen
import com.arn.scrobble.onboarding.ListenBrainzLoginScreen
import com.arn.scrobble.onboarding.MalojaLoginScreen
import com.arn.scrobble.onboarding.OnboardingScreen
import com.arn.scrobble.onboarding.OobLibrefmLoginScreen
import com.arn.scrobble.onboarding.OobPleromaLoginScreen
import com.arn.scrobble.onboarding.PleromaLoginScreen
import com.arn.scrobble.onboarding.WebViewScreen
import com.arn.scrobble.pref.AppListSaveType
import com.arn.scrobble.pref.AppListScreen
import com.arn.scrobble.pref.AutomationInfoScreen
import com.arn.scrobble.pref.DeleteAccountScreen
import com.arn.scrobble.pref.ExportScreen
import com.arn.scrobble.pref.ImportScreen
import com.arn.scrobble.pref.OssCreditsScreen
import com.arn.scrobble.pref.PrefsScreen
import com.arn.scrobble.pref.TranslatorsScreen
import com.arn.scrobble.recents.TrackHistoryScreen
import com.arn.scrobble.search.ImageSearchScreen
import com.arn.scrobble.search.SearchScreen
import com.arn.scrobble.themes.ThemeChooserScreen
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.ui.accountTypeLabel
import com.arn.scrobble.ui.addColumnPadding
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.automation_cli
import pano_scrobbler.composeapp.generated.resources.automation_cp
import pano_scrobbler.composeapp.generated.resources.choose_an_app
import pano_scrobbler.composeapp.generated.resources.choose_apps
import pano_scrobbler.composeapp.generated.resources.delete_account
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.edit_regex
import pano_scrobbler.composeapp.generated.resources.edit_regex_test
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

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.panoNavGraph(
    onSetTitle: (String, String?) -> Unit,
    onSetOtherUser: (UserCached?) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    onSetNavMetadataList: (List<PanoNavMetadata>) -> Unit,
    tabIdx: () -> Int,
    onSetTabData: (String, List<PanoTab>?) -> Unit,
    onSetTabIdx: (Int) -> Unit,
    navigate: (PanoRoute) -> Unit,
    onOnboardingFinished: () -> Unit,
    goBack: () -> Unit,
    goUp: () -> Unit,
    pullToRefreshState: () -> PullToRefreshState,
    onSetRefreshing: (Int, PanoPullToRefreshStateForTab) -> Unit,
    mainViewModel: MainViewModel,
) {

    @Composable
    fun onSetTitleString(destId: String, title: String) {
        DisposableEffect(title) {
            onSetTitle(destId, title)

            onDispose {
                onSetTitle(destId, null)
            }
        }
    }

    @Composable
    fun onSetTitleRes(destId: String, resId: StringResource) {
        onSetTitleString(destId, stringResource(resId))
    }

    @Composable
    fun modifier() = Modifier
        .fillMaxSize()
//        .background(MaterialTheme.colorScheme.background)

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

        if (currentUserSelf != null) {
            HomePagerScreen(
                user = currentUserSelf!!,
                onSetOtherUser = onSetOtherUser,
                onSetTitle = { title -> onSetTitle(it.id, title) },
                tabIdx = tabIdx(),
                initialTabIdx = initialTab,
                onSetTabIdx = onSetTabIdx,
                onSetTabData = onSetTabData,
                onSetNavMetadataList = onSetNavMetadataList,
                onNavigate = navigate,
                onOpenDialog = onOpenDialog,
                pullToRefreshState = pullToRefreshState(),
                onSetRefreshing = onSetRefreshing,
                mainViewModel = mainViewModel,
                getPullToRefreshTrigger = { mainViewModel.getPullToRefreshTrigger(it) },
                modifier = modifier()
            )
        }
    }

    composable<PanoRoute.OthersHomePager>(
        typeMap = mapOf(
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        val arguments = it.toRoute<PanoRoute.OthersHomePager>()

        HomePagerScreen(
            user = arguments.user,
            onSetOtherUser = onSetOtherUser,
            onSetTitle = { title -> onSetTitle(it.id, title) },
            initialTabIdx = 0,
            tabIdx = tabIdx(),
            onSetTabIdx = onSetTabIdx,
            onSetTabData = onSetTabData,
            onSetNavMetadataList = onSetNavMetadataList,
            onNavigate = navigate,
            onOpenDialog = onOpenDialog,
            pullToRefreshState = pullToRefreshState(),
            onSetRefreshing = onSetRefreshing,
            mainViewModel = mainViewModel,
            getPullToRefreshTrigger = { mainViewModel.getPullToRefreshTrigger(it) },
            modifier = modifier()
        )
    }

    composable<PanoRoute.Random>(
        typeMap = mapOf(
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        onSetTitleRes(it.id, Res.string.random_text)
        val arguments = it.toRoute<PanoRoute.Random>()

        RandomScreen(
            user = arguments.user,
            onOpenDialog = onOpenDialog,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.OssCredits> {
        onSetTitleRes(it.id, Res.string.pref_oss_credits)
        OssCreditsScreen(
            modifier = modifier()
        )
    }

    composable<PanoRoute.AppList>(
        typeMap = mapOf(
            typeOf<AppListSaveType>() to serializableType<AppListSaveType>()
        )
    ) {
        val arguments = it.toRoute<PanoRoute.AppList>()
        onSetTitleRes(
            it.id,
            if (arguments.isSingleSelect)
                Res.string.choose_an_app
            else
                Res.string.choose_apps
        )

        AppListScreen(
            isSingleSelect = arguments.isSingleSelect,
            saveType = arguments.saveType,
            preSelectedPackages = arguments.preSelectedPackages.toSet(),
            onSetPackagesSelection = { checked, unchecked ->
                mainViewModel.onSetPackagesSelection(checked, unchecked)
            },
            modifier = modifier()
        )
    }

    composable<PanoRoute.ThemeChooser> {
        onSetTitleRes(it.id, Res.string.pref_themes)
        ThemeChooserScreen(
            onNavigateToBilling = { navigate(PanoRoute.Billing) },
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.DeleteAccount> {
        onSetTitleRes(it.id, Res.string.delete_account)
        DeleteAccountScreen(
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.BillingTroubleshoot> {
        onSetTitleRes(it.id, Res.string.help)
        BillingTroubleshootScreen(
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.Billing> {
        onSetTitleString(it.id, "")
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
        onSetTitleRes(it.id, Res.string.settings)
        PrefsScreen(
            onNavigate = navigate,
            modifier = modifier()
        )
    }

    composable<PanoRoute.SimpleEdits> {
        onSetTitleRes(it.id, Res.string.simple_edits)
        SimpleEditsScreen(
            onEdit = { navigate(PanoRoute.SimpleEditsAdd(it)) },
            modifier = modifier()
        )
    }

    composable<PanoRoute.SimpleEditsAdd>(
        typeMap = mapOf(typeOf<SimpleEdit?>() to serializableType<SimpleEdit?>())
    ) {
        onSetTitleRes(it.id, Res.string.edit)

        val arguments = it.toRoute<PanoRoute.SimpleEditsAdd>()

        SimpleEditsAddScreen(
            simpleEdit = arguments.simpleEdit,
            onBack = goBack,
            modifier = modifier().padding(panoContentPadding())
        )
    }

    composable<PanoRoute.RegexEdits> {
        onSetTitleRes(it.id, Res.string.pref_regex_edits)
        RegexEditsScreen(
            onNavigateToTest = { navigate(PanoRoute.RegexEditsTest) },
            onNavigateToEdit = { navigate(PanoRoute.RegexEditsAdd(it)) },
            modifier = modifier()
        )
    }

    composable<PanoRoute.RegexEditsAdd>(
        typeMap = mapOf(typeOf<RegexEdit?>() to serializableType<RegexEdit?>())
    ) {
        onSetTitleRes(it.id, Res.string.edit_regex)

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
        onSetTitleRes(it.id, Res.string.edit_regex_test)

        RegexEditsTestScreen(
            mainViewModel = mainViewModel,
            onNavigateToAppList = {
                navigate(
                    PanoRoute.AppList(
                        saveType = AppListSaveType.Callback,
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
        onSetTitleRes(it.id, Res.string.pref_blocked_metadata)

        BlockedMetadatasScreen(
            onEdit = {
                onOpenDialog(
                    PanoDialog.BlockedMetadataAdd(
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
        onSetTitleRes(it.id, Res.string.search)

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
        onSetTitleRes(it.id, Res.string.pref_translate_credits)

        TranslatorsScreen(
            modifier = modifier()
        )
    }

    composable<PanoRoute.Import> {
        onSetTitleRes(it.id, Res.string.pref_import)

        ImportScreen(
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.Export> {
        onSetTitleRes(it.id, Res.string.pref_export)

        ExportScreen(
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginFile> {
        onSetTitleRes(it.id, Res.string.scrobble_to_file)

        FileLoginScreen(
            onBack = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginGnufm> {
        onSetTitleString(it.id, accountTypeLabel(AccountType.GNUFM))

        GnufmLoginScreen(
            onDone = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginListenBrainz> {
        onSetTitleRes(it.id, Res.string.listenbrainz)

        ListenBrainzLoginScreen(
            onDone = goBack,
            hasCustomApiRoot = false,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginCustomListenBrainz> {
        onSetTitleString(it.id, accountTypeLabel(AccountType.CUSTOM_LISTENBRAINZ))

        ListenBrainzLoginScreen(
            onDone = goBack,
            hasCustomApiRoot = true,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginMaloja> {
        onSetTitleRes(it.id, Res.string.maloja)

        MalojaLoginScreen(
            onDone = goBack,
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.LoginPleroma> {
        onSetTitleRes(it.id, Res.string.pleroma)

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
            pleromaOauthClientCreds = arguments.creds,
            onSetTitle = { title -> onSetTitle(it.id, title) },
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
        onSetTitleRes(it.id, Res.string.pleroma)

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
        onSetTitleRes(it.id, Res.string.pref_login)

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
        onSetTitleRes(it.id, Res.string.search)

        SearchScreen(
            onOpenDialog = onOpenDialog,
            modifier = modifier()
        )
    }


    composable<PanoRoute.Onboarding> {
        onSetTitleString(it.id, "")

        OnboardingScreen(
            onNavigate = navigate,
            onDone = {
                onOnboardingFinished()
            },
            modifier = modifier().addColumnPadding()
        )
    }

    composable<PanoRoute.Help> {
        onSetTitleRes(it.id, Res.string.help)

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
        val arguments = it.toRoute<PanoRoute.MusicEntryInfoPager>()

        onSetTitleString(it.id, arguments.artist.name)

        InfoPagerScreen(
            musicEntry = arguments.artist,
            user = arguments.user,
            pkgName = arguments.pkgName,
            tabIdx = tabIdx(),
            onSetTabData = onSetTabData,
            onSetTabIdx = onSetTabIdx,
            initialTabIdx = when (arguments.type) {
                Stuff.TYPE_ARTISTS -> 0
                Stuff.TYPE_ALBUMS -> 1
                Stuff.TYPE_TRACKS -> 2
                else -> 0
            },
            onOpenDialog = onOpenDialog,
            modifier = modifier()
        )
    }


    composable<PanoRoute.ChartsPager>(
        typeMap = mapOf(
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        val arguments = it.toRoute<PanoRoute.ChartsPager>()

        ChartsPagerScreen(
            user = arguments.user,
            tabIdx = tabIdx(),
            onSetTabIdx = onSetTabIdx,
            onSetTabData = onSetTabData,
            initialTabIdx = when (arguments.type) {
                Stuff.TYPE_ARTISTS -> 0
                Stuff.TYPE_ALBUMS -> 1
                Stuff.TYPE_TRACKS -> 2
                else -> 0
            },
            onSetTitle = { title -> onSetTitle(it.id, title) },
            onOpenDialog = onOpenDialog,
            modifier = modifier()
        )
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

        onSetTitleString(it.id, title)

        SimilarTracksScreen(
            musicEntry = arguments.track,
            user = arguments.user,
            pkgName = arguments.pkgName,
            onOpenDialog = onOpenDialog,
            modifier = modifier()
        )
    }

    composable<PanoRoute.Help> {
        onSetTitleRes(it.id, Res.string.help)

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
        onSetTitleString(it.id, title)

        TrackHistoryScreen(
            track = track,
            user = user,
            onOpenDialog = onOpenDialog,
            editDataFlow = mainViewModel.editDataFlow,
            modifier = modifier()
        )
    }

    composable<PanoRoute.AutomationInfo> {
        onSetTitleString(
            it.id, stringResource(
                if (PlatformStuff.isDesktop)
                    Res.string.automation_cli
                else
                    Res.string.automation_cp
            )
        )

        AutomationInfoScreen(
            onNavigate = navigate,
            modifier = modifier().padding(panoContentPadding())
        )
    }

    panoPlatformSpecificNavGraph(
        onSetTitle = onSetTitle,
        navigate = navigate,
        goUp = goUp,
        mainViewModel = mainViewModel,
    )
}
