package com.arn.scrobble.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.billing.BillingScreen
import com.arn.scrobble.billing.BillingTroubleshootScreen
import com.arn.scrobble.charts.ChartsPagerScreen
import com.arn.scrobble.charts.RandomScreen
import com.arn.scrobble.edits.BlockedMetadatasScreen
import com.arn.scrobble.edits.RegexEditsAddScreen
import com.arn.scrobble.edits.RegexEditsScreen
import com.arn.scrobble.edits.RegexEditsTestScreen
import com.arn.scrobble.edits.SimpleEditsAddScreen
import com.arn.scrobble.edits.SimpleEditsScreen
import com.arn.scrobble.help.HelpScreen
import com.arn.scrobble.help.PrivacyPolicyScreen
import com.arn.scrobble.info.InfoPagerScreen
import com.arn.scrobble.info.SimilarTracksScreen
import com.arn.scrobble.main.HomePagerScreen
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.onboarding.FileLoginScreen
import com.arn.scrobble.onboarding.GnufmLoginScreen
import com.arn.scrobble.onboarding.ListenBrainzLoginScreen
import com.arn.scrobble.onboarding.OnboardingScreen
import com.arn.scrobble.onboarding.OobLastfmLibrefmLoginScreen
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
import pano_scrobbler.composeapp.generated.resources.lastfm
import pano_scrobbler.composeapp.generated.resources.listenbrainz
import pano_scrobbler.composeapp.generated.resources.login_in_browser
import pano_scrobbler.composeapp.generated.resources.pleroma
import pano_scrobbler.composeapp.generated.resources.pref_blocked_metadata
import pano_scrobbler.composeapp.generated.resources.pref_export
import pano_scrobbler.composeapp.generated.resources.pref_import
import pano_scrobbler.composeapp.generated.resources.pref_oss_credits
import pano_scrobbler.composeapp.generated.resources.pref_privacy_policy
import pano_scrobbler.composeapp.generated.resources.pref_themes
import pano_scrobbler.composeapp.generated.resources.pref_translate_credits
import pano_scrobbler.composeapp.generated.resources.random_text
import pano_scrobbler.composeapp.generated.resources.regex_rules
import pano_scrobbler.composeapp.generated.resources.scan_qr
import pano_scrobbler.composeapp.generated.resources.scrobble_to_file
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.settings
import pano_scrobbler.composeapp.generated.resources.simple_edits
import pano_scrobbler.composeapp.generated.resources.spotify

object PanoNavGraph {
    fun panoNavEntryProvider(
        onSetTitle: (PanoRoute, String) -> Unit,
        getTabIdx: (PanoRoute.HasTabs, Int) -> Int,
        onSetTabIdx: (PanoRoute.HasTabs, Int) -> Unit,
        navigate: (PanoRoute) -> Unit,
        onSetOnboardingFinished: () -> Unit,
        goBack: () -> Unit,
        pullToRefreshState: () -> PullToRefreshState,
        onSetRefreshing: (Int, PanoPullToRefreshStateForTab) -> Unit,
        onSetDrawerData: (DrawerData) -> Unit,
        mainViewModel: MainViewModel,
    ) = entryProvider {

        @Composable
        fun onSetTitleString(route: PanoRoute, title: String) {
            LaunchedEffect(title) {
                onSetTitle(route, title)
            }
        }

        @Composable
        fun onSetTitleRes(route: PanoRoute, resId: StringResource) {
            onSetTitleString(route, stringResource(resId))
        }

        @Composable
        fun modifier() = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)

        @Composable
        fun getTabData(route: PanoRoute.HasTabs): List<PanoTab> {
            val currentAccountType by PlatformStuff.mainPrefs.data
                .collectAsStateWithInitialValue { it.currentAccountType }

            return remember(currentAccountType) { route.getTabsList(currentAccountType) }
        }

        entry<PanoRoute.Blank> {
        }

        entry<PanoRoute.SelfHomePager> { route ->
            val currentUserSelf by PlatformStuff.mainPrefs.data
                .collectAsStateWithInitialValue { it.currentAccount?.user }

            val savedTab by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.lastHomePagerTab }

            if (currentUserSelf != null) {
                HomePagerScreen(
                    user = currentUserSelf!!,
                    onSetTitle = { title -> onSetTitle(route, title) },
                    tabIdx = getTabIdx(
                        route,
                        if (route.digestTypeStr != null)
                            2 // charts tab
                        else
                            savedTab
                    ),
                    digestTimePeriod = route.digestTypeStr?.let { LastfmPeriod.valueOf(it) },
                    onSetTabIdx = { tab ->
                        onSetTabIdx(route, tab)
                    },
                    tabsList = getTabData(route),
                    onNavigate = navigate,
                    pullToRefreshState = pullToRefreshState(),
                    onSetRefreshing = onSetRefreshing,
                    mainViewModel = mainViewModel,
                    getPullToRefreshTrigger = { mainViewModel.getPullToRefreshTrigger(it) },
                    modifier = modifier()
                )
            }
        }

        entry<PanoRoute.OthersHomePager> { route ->
            HomePagerScreen(
                user = route.user,
                onSetTitle = { title -> onSetTitle(route, title) },
                tabIdx = getTabIdx(route, 0),
                digestTimePeriod = null,
                onSetTabIdx = { tab ->
                    onSetTabIdx(route, tab)
                },
                tabsList = getTabData(route),
                onNavigate = navigate,
                pullToRefreshState = pullToRefreshState(),
                onSetRefreshing = onSetRefreshing,
                mainViewModel = mainViewModel,
                getPullToRefreshTrigger = { mainViewModel.getPullToRefreshTrigger(it) },
                modifier = modifier()
            )
        }

        entry<PanoRoute.Random> { route ->

            onSetTitleRes(route, Res.string.random_text)
            RandomScreen(
                user = route.user,
                onNavigate = navigate,
                modifier = modifier().padding(panoContentPadding())
            )
        }

        entry<PanoRoute.OssCredits> { route ->

            onSetTitleRes(route, Res.string.pref_oss_credits)
            OssCreditsScreen(
                modifier = modifier()
            )
        }

        entry<PanoRoute.AppList> { route ->
            onSetTitleRes(
                route,
                if (route.isSingleSelect)
                    Res.string.choose_an_app
                else
                    Res.string.choose_apps
            )
            AppListScreen(
                isSingleSelect = route.isSingleSelect,
                saveType = route.saveType,
                packagesOverride = route.packagesOverride?.toSet(),
                preSelectedPackages = route.preSelectedPackages.toSet(),
                onSetPackagesSelection = { checked, unchecked ->
                    mainViewModel.onSetPackagesSelection(checked, unchecked)
                },
                modifier = modifier()
            )
        }

        entry<PanoRoute.ThemeChooser> { route ->

            onSetTitleRes(route, Res.string.pref_themes)
            ThemeChooserScreen(
                onNavigateToBilling = { navigate(PanoRoute.Billing) },
                modifier = modifier()
                    .verticalScroll(rememberScrollState())
                    .padding(panoContentPadding(mayHaveBottomFab = true))
            )
        }

        entry<PanoRoute.DeleteAccount> { route ->

            onSetTitleRes(route, Res.string.delete_account)
            DeleteAccountScreen(
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.BillingTroubleshoot> { route ->

            onSetTitleRes(route, Res.string.help)
            BillingTroubleshootScreen(
                onBack = goBack,
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.Billing> {
            BillingScreen(
                onBack = goBack,
                onNavigateToTroubleshoot = { navigate(PanoRoute.BillingTroubleshoot) },
                viewModel = mainViewModel,
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.Prefs> { route ->

            onSetTitleRes(route, Res.string.settings)
            PrefsScreen(
                onNavigate = navigate,
                modifier = modifier()
            )
        }

        entry<PanoRoute.SimpleEdits> { route ->

            onSetTitleRes(route, Res.string.simple_edits)
            SimpleEditsScreen(
                onNavigate = navigate,
                modifier = modifier()
            )
        }

        entry<PanoRoute.SimpleEditsAdd> { route ->

            onSetTitleRes(route, Res.string.edit)
            SimpleEditsAddScreen(
                simpleEdit = route.simpleEdit,
                onDone = goBack,
                onReauthenticate = {},
                origScrobbleData = null,
                msid = null,
                hash = null,
                key = null,
                viewModel = mainViewModel,
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.RegexEdits> { route ->

            onSetTitleRes(route, Res.string.regex_rules)
            RegexEditsScreen(
                onNavigate = navigate,
                modifier = modifier()
            )
        }

        entry<PanoRoute.RegexEditsAdd> { route ->

            onSetTitleRes(route, Res.string.edit_regex)
            RegexEditsAddScreen(
                mainViewModel = mainViewModel,
                regexEdit = route.regexEdit,
                onNavigate = navigate,
                onBack = goBack,
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.RegexEditsTest> { route ->

            onSetTitleRes(route, Res.string.edit_regex_test)
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

        entry<PanoRoute.BlockedMetadatas> { route ->

            onSetTitleRes(route, Res.string.pref_blocked_metadata)
            BlockedMetadatasScreen(
                onNavigate = navigate,
                modifier = modifier()
            )
        }

        entry<PanoRoute.ImageSearch> { route ->
            onSetTitleString(
                route,
                stringResource(Res.string.search) + ": " + stringResource(Res.string.spotify)
            )

            ImageSearchScreen(
                onBack = goBack,
                artist = route.artist,
                originalArtist = route.originalArtist,
                album = route.album,
                originalAlbum = route.originalAlbum,
                modifier = modifier()
            )
        }

        entry<PanoRoute.Translators> { route ->
            onSetTitleRes(route, Res.string.pref_translate_credits)
            TranslatorsScreen(
                modifier = modifier()
            )
        }

        entry<PanoRoute.Import> { route ->
            onSetTitleRes(route, Res.string.pref_import)
            ImportScreen(
                onBack = goBack,
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.Export> { route ->
            onSetTitleRes(route, Res.string.pref_export)
            ExportScreen(
                onBack = goBack,
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.LoginFile> { route ->
            onSetTitleRes(route, Res.string.scrobble_to_file)

            FileLoginScreen(
                onBack = goBack,
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.LoginGnufm> { route ->

            onSetTitleString(route, accountTypeLabel(AccountType.GNUFM))
            GnufmLoginScreen(
                onDone = goBack,
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.LoginListenBrainz> { route ->

            onSetTitleRes(route, Res.string.listenbrainz)
            ListenBrainzLoginScreen(
                onDone = goBack,
                hasCustomApiRoot = false,
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.LoginCustomListenBrainz> { route ->

            onSetTitleString(route, accountTypeLabel(AccountType.CUSTOM_LISTENBRAINZ))
            ListenBrainzLoginScreen(
                onDone = goBack,
                hasCustomApiRoot = true,
                modifier = modifier().addColumnPadding()
            )
        }

//    entry<PanoRoute.LoginMaloja> {
//        onSetTitleRes(it.id, Res.string.maloja)
//
//        MalojaLoginScreen(
//            onDone = goBack,
//            modifier = modifier().addColumnPadding()
//        )
//    }

        entry<PanoRoute.LoginPleroma> { route ->
            onSetTitleRes(route, Res.string.pleroma)

            PleromaLoginScreen(
                onBackAndThenNavigate = {
                    goBack()
                    navigate(it)
                },
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.WebView> { route ->
            WebViewScreen(
                initialUrl = route.url,
                userAccountTemp = route.userAccountTemp,
                pleromaOauthClientCreds = route.creds,
                onSetTitle = { title -> onSetTitle(route, title) },
                onBack = goBack,
                onNavigate = navigate,
                modifier = modifier().padding(panoContentPadding())
                // webview has issues with nested scroll
            )
        }

        entry<PanoRoute.OobPleromaAuth> { route ->
            onSetTitleRes(route, Res.string.pleroma)

            OobPleromaLoginScreen(
                url = route.url,
                userAccountTemp = route.userAccountTemp,
                pleromaCreds = route.creds,
                onBack = goBack,
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.OobLastfmLibreFmAuth> { route ->
            onSetTitleRes(
                route,
                if (PlatformStuff.isTv)
                    Res.string.scan_qr
                else
                    Res.string.login_in_browser
            )
            OobLastfmLibrefmLoginScreen(
                userAccountTemp = route.userAccountTemp,
                onBack = goBack,
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.Search> { route ->
            onSetTitleString(
                route,
                stringResource(Res.string.search) + ": " + stringResource(Res.string.lastfm)
            )

            SearchScreen(
                onNavigate = navigate,
                modifier = modifier()
            )
        }


        entry<PanoRoute.Onboarding> {
            OnboardingScreen(
                onNavigate = navigate,
                onDone = {
                    onSetOnboardingFinished()
                },
                modifier = modifier().addColumnPadding()
            )
        }

        entry<PanoRoute.MusicEntryInfoPager> { route ->
            onSetTitleString(route, route.artist.name)

            InfoPagerScreen(
                artist = route.artist,
                user = route.user,
                appId = route.appId,
                tabIdx = getTabIdx(
                    route,
                    when (route.entryType) {
                        Stuff.TYPE_ARTISTS -> 0
                        Stuff.TYPE_ALBUMS -> 1
                        Stuff.TYPE_TRACKS -> 2
                        else -> 0
                    }
                ),
                onSetTabIdx = { tab ->
                    onSetTabIdx(route, tab)
                },
                tabsList = getTabData(route),
                onNavigate = navigate,
                modifier = modifier()
            )
        }


        entry<PanoRoute.ChartsPager> { route ->
            ChartsPagerScreen(
                user = route.user,
                tabIdx = getTabIdx(
                    route,
                    when (route.chartsType) {
                        Stuff.TYPE_ARTISTS -> 0
                        Stuff.TYPE_ALBUMS -> 1
                        Stuff.TYPE_TRACKS -> 2
                        else -> 0
                    }
                ),
                onSetTabIdx = { tab ->
                    onSetTabIdx(route, tab)
                },
                tabsList = getTabData(route),
                onSetTitle = { title -> onSetTitle(route, title) },
                onNavigate = navigate,
                modifier = modifier()
            )
        }

        entry<PanoRoute.SimilarTracks> { route ->
            val entry = route.track
            val title = Stuff.formatBigHyphen(
                entry.artist.name,
                entry.name
            )

            onSetTitleString(route, title)

            SimilarTracksScreen(
                track = route.track,
                user = route.user,
                appId = route.appId,
                onNavigate = navigate,
                modifier = modifier()
            )
        }

        entry<PanoRoute.Help> { route ->
            onSetTitleRes(route, Res.string.help)

            HelpScreen(
                searchTerm = route.searchTerm,
                modifier = modifier().padding(panoContentPadding())
            )
        }

        entry<PanoRoute.PrivacyPolicy> { route ->
            onSetTitleRes(route, Res.string.pref_privacy_policy)

            PrivacyPolicyScreen(
                modifier = modifier()
            )
        }

        entry<PanoRoute.TrackHistory> { route ->
            val track = route.track
            val user = route.user

            TrackHistoryScreen(
                track = track,
                user = user,
                onSetTitle = { title -> onSetTitle(route, title) },
                onNavigate = navigate,
                editDataFlow = mainViewModel.editScrobbleUtils.editDataFlow,
                modifier = modifier()
            )
        }

        entry<PanoRoute.AutomationInfo> { route ->
            onSetTitleRes(
                route,
                if (PlatformStuff.isDesktop)
                    Res.string.automation_cli
                else
                    Res.string.automation_cp
            )

            AutomationInfoScreen(
                onNavigate = navigate,
                modifier = modifier().padding(panoContentPadding())
            )
        }

        panoModalNavGraph(
            navigate = navigate,
            goBack = goBack,
            onSetDrawerData = onSetDrawerData,
            mainViewModel = mainViewModel,
        )

        panoPlatformSpecificNavGraph(
            onSetTitle = onSetTitle,
            navigate = navigate,
            goBack = goBack,
        )
    }
}
