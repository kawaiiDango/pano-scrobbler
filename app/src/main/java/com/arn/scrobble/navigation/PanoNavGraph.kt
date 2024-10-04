package com.arn.scrobble.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.arn.scrobble.PlaceholderScreen
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.billing.BillingScreen
import com.arn.scrobble.billing.BillingTroubleshootScreen
import com.arn.scrobble.charts.CollageGeneratorScreen
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.edits.BlockedMetadataAddScreen
import com.arn.scrobble.edits.BlockedMetadatasScreen
import com.arn.scrobble.edits.EditScrobbleDialog
import com.arn.scrobble.edits.RegexEditsAddScreen
import com.arn.scrobble.edits.RegexEditsScreen
import com.arn.scrobble.edits.RegexEditsTestScreen
import com.arn.scrobble.edits.SimpleEditsAddScreen
import com.arn.scrobble.edits.SimpleEditsScreen
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.info.MusicEntryInfoScreen
import com.arn.scrobble.info.TagInfoScreen
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.onboarding.FileLoginScreen
import com.arn.scrobble.onboarding.GnufmLoginScreen
import com.arn.scrobble.onboarding.ListenBrainzLoginScreen
import com.arn.scrobble.onboarding.MalojaLoginScreen
import com.arn.scrobble.onboarding.OnboardingScreen
import com.arn.scrobble.onboarding.PleromaLoginScreen
import com.arn.scrobble.pref.AppListScreen
import com.arn.scrobble.pref.DeleteAccountScreen
import com.arn.scrobble.pref.ExportScreen
import com.arn.scrobble.pref.ImportScreen
import com.arn.scrobble.pref.OssCreditsScreen
import com.arn.scrobble.pref.PrefsScreen
import com.arn.scrobble.pref.TranslatorsScreen
import com.arn.scrobble.search.ImageSearchScreen
import com.arn.scrobble.search.SearchScreen
import com.arn.scrobble.themes.ThemeChooserScreen
import com.arn.scrobble.ui.addColumnPadding
import com.arn.scrobble.utils.Stuff
import kotlin.reflect.typeOf

fun NavGraphBuilder.panoNavGraph(
    onSetTitle: (Int?) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goBack: () -> Unit,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier.fillMaxSize()
) {

    composable<PanoRoute.Placeholder> {
        onSetTitle(R.string.edit_regex_test)
        PlaceholderScreen(
            modifier = modifier
        )
    }

    composable<PanoRoute.OssCredits> {
        onSetTitle(R.string.pref_oss_credits)
        OssCreditsScreen(
            modifier = modifier
        )
    }

    composable<PanoRoute.AppList> {
        val arguments = it.toRoute<PanoRoute.AppList>()
        onSetTitle(
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
            modifier = modifier
        )
    }

    composable<PanoRoute.ThemeChooser> {
        onSetTitle(R.string.pref_themes)
        ThemeChooserScreen(
            onNavigateToBilling = { navigate(PanoRoute.Billing) },
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.DeleteAccount> {
        onSetTitle(R.string.delete_account)
        DeleteAccountScreen(
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.BillingTroubleshoot> {
        onSetTitle(R.string.billing_troubleshoot)
        BillingTroubleshootScreen(
            onBack = goBack,
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.Billing> {
        onSetTitle(null)
        BillingScreen(
            onBack = goBack,
            onNavigateToTroubleshoot = { navigate(PanoRoute.BillingTroubleshoot) },
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.Prefs>(
        deepLinks = listOf(
            navDeepLink {
                uriPattern = Stuff.DEEPLINK_PROTOCOL_NAME + "://screen/settings"
            })
    ) {
        onSetTitle(R.string.settings)
        PrefsScreen(
            onNavigate = navigate,
            modifier = modifier
        )
    }

    composable<PanoRoute.SimpleEdits> {
        onSetTitle(R.string.simple_edits)
        SimpleEditsScreen(
            onEdit = { navigate(PanoRoute.SimpleEditsAdd(it)) },
            modifier = modifier
        )
    }

    composable<PanoRoute.SimpleEditsAdd>(
        typeMap = mapOf(typeOf<SimpleEdit?>() to serializableType<SimpleEdit?>(nullable = true))
    ) {
        onSetTitle(R.string.edit)

        val arguments = it.toRoute<PanoRoute.SimpleEditsAdd>()

        SimpleEditsAddScreen(
            simpleEdit = arguments.simpleEdit,
            onBack = goBack,
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.RegexEdits> {
        onSetTitle(R.string.pref_regex_edits)
        RegexEditsScreen(
            onNavigateToTest = { navigate(PanoRoute.RegexEditsTest) },
            onNavigateToEdit = { navigate(PanoRoute.RegexEditsAdd(it)) },
            modifier = modifier
        )
    }

    composable<PanoRoute.RegexEditsAdd>(
        typeMap = mapOf(typeOf<RegexEdit?>() to serializableType<RegexEdit?>(nullable = true))
    ) {
        onSetTitle(R.string.edit_regex)

        val arguments = it.toRoute<PanoRoute.RegexEditsAdd>()

        RegexEditsAddScreen(
            mainViewModel = mainViewModel,
            regexEdit = arguments.regexEdit,
            onNavigate = navigate,
            onBack = goBack,
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.RegexEditsTest> {
        onSetTitle(R.string.edit_regex_test)

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
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.BlockedMetadatas> {
        onSetTitle(R.string.pref_blocked_metadata)

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
            modifier = modifier
        )
    }

    composable<PanoRoute.ImageSearch>(
        typeMap = mapOf(
            typeOf<MusicEntry>() to serializableType<MusicEntry>(),
            typeOf<MusicEntry?>() to serializableType<MusicEntry?>(nullable = true),
        )
    ) {
        onSetTitle(R.string.search)

        val arguments = it.toRoute<PanoRoute.ImageSearch>()

        ImageSearchScreen(
            onBack = goBack,
            musicEntry = arguments.musicEntry,
            originalMusicEntry = arguments.originalMusicEntry,
            modifier = modifier
        )
    }

    composable<PanoRoute.Translators> {
        onSetTitle(R.string.pref_translate_credits)

        TranslatorsScreen(
            modifier = modifier
        )
    }

    composable<PanoRoute.Import> {
        onSetTitle(R.string.pref_import)

        ImportScreen(
            onBack = goBack,
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.Export> {
        onSetTitle(R.string.pref_export)

        ExportScreen(
            onBack = goBack,
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.LoginFile> {
        onSetTitle(R.string.scrobble_to_file)

        FileLoginScreen(
            onBack = goBack,
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.LoginGnufm> {
        onSetTitle(R.string.gnufm)

        GnufmLoginScreen(
            onDone = goBack,
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.LoginListenBrainz> {
        onSetTitle(R.string.listenbrainz)

        ListenBrainzLoginScreen(
            onDone = goBack,
            hasCustomApiRoot = false,
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.LoginCustomListenBrainz> {
        onSetTitle(R.string.custom_listenbrainz)

        ListenBrainzLoginScreen(
            onDone = goBack,
            hasCustomApiRoot = true,
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.LoginMaloja> {
        onSetTitle(R.string.maloja)

        MalojaLoginScreen(
            onDone = goBack,
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.LoginPleroma> {
        onSetTitle(R.string.pleroma)

        PleromaLoginScreen(
            onNavigateToWebview = { url, userAccountTemp, creds ->
                navigate(PanoRoute.WebView(url, userAccountTemp, creds))
            },
            modifier = modifier.addColumnPadding()
        )
    }

    composable<PanoRoute.Search>(
        typeMap = mapOf(
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        val arguments = it.toRoute<PanoRoute.Search>()

        SearchScreen(
            user = arguments.user,
            onNavigate = navigate,
            modifier = modifier
        )
    }


    composable<PanoRoute.Onboarding> {
        onSetTitle(null)

        OnboardingScreen(
            onNavigate = navigate,
            onDone = {
                // todo make home the first screen
            },
            modifier = modifier.addColumnPadding()
        )
    }

    // dialogs

    dialog<PanoRoute.BlockedMetadataAdd>(
        typeMap = mapOf(typeOf<BlockedMetadata>() to serializableType<BlockedMetadata>())
    ) {
        val arguments = it.toRoute<PanoRoute.BlockedMetadataAdd>()

        BlockedMetadataAddScreen(
            blockedMetadata = arguments.blockedMetadata,
            ignoredArtist = arguments.ignoredArtist,
            hash = arguments.hash,
            onNavigateToBilling = { navigate(PanoRoute.Billing) },
            onBack = goBack,
        )
    }

    dialog<PanoRoute.EditScrobble>(
        typeMap = mapOf(typeOf<ScrobbleData>() to serializableType<ScrobbleData>())
    ) {
        val arguments = it.toRoute<PanoRoute.EditScrobble>()

        EditScrobbleDialog(
            mainViewModel = mainViewModel,
            scrobbleData = arguments.scrobbleData,
            msid = arguments.msid,
            hash = arguments.hash,
            onNavigate = navigate,
            onBack = goBack,
        )
    }

    dialog<PanoRoute.TagInfo>(
        typeMap = mapOf(typeOf<Tag>() to serializableType<Tag>())
    ) {
        val arguments = it.toRoute<PanoRoute.TagInfo>()

        TagInfoScreen(
            tag = arguments.tag
        )
    }

    dialog<PanoRoute.MusicEntryInfo>(
        typeMap = mapOf(
            typeOf<MusicEntry>() to serializableType<MusicEntry>(),
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        val arguments = it.toRoute<PanoRoute.MusicEntryInfo>()

        MusicEntryInfoScreen(
            musicEntry = arguments.musicEntry,
            pkgName = arguments.pkgName,
            user = arguments.user,
            onNavigate = navigate,
        )
    }

    dialog<PanoRoute.CollageGenerator>(
        typeMap = mapOf(
            typeOf<TimePeriod>() to serializableType<TimePeriod>(),
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        val arguments = it.toRoute<PanoRoute.CollageGenerator>()

        CollageGeneratorScreen(
            collageType = arguments.collageType,
            timePeriod = arguments.timePeriod,
            user = arguments.user,
        )
    }


//    }

}