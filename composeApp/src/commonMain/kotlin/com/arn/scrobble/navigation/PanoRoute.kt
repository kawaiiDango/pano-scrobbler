package com.arn.scrobble.navigation

import androidx.navigation3.runtime.NavKey
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.icons.Add
import com.arn.scrobble.icons.Check
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.pref.AppListSaveType
import com.arn.scrobble.updates.UpdateAction
import kotlinx.serialization.Serializable
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.add
import pano_scrobbler.composeapp.generated.resources.done

@Serializable
sealed interface PanoRoute : NavKey {

    @Serializable
    sealed interface DeepLinkable : PanoRoute

    sealed interface HasTabs : PanoRoute {
        fun getTabsList(accountType: AccountType): List<PanoTab>
    }

    sealed interface HasFab : PanoRoute {
        fun getFabData(): PanoFabData
    }

    sealed interface HasUser : PanoRoute {
        val user: UserCached?
    }

    @Serializable
    data class SelfHomePager(val digestTypeStr: String? = null) :
        PanoRoute, DeepLinkable, HasTabs, HasUser {
        override val user = null

        override fun getTabsList(accountType: AccountType) = homePagerTabData(accountType)

    }

    @Serializable
    data class OthersHomePager(override val user: UserCached) : PanoRoute, HasTabs, HasUser {
        override fun getTabsList(accountType: AccountType) = homePagerTabData(accountType)

    }

    @Serializable
    data object OssCredits : PanoRoute

    @Serializable
    data object Prefs : PanoRoute, DeepLinkable

    @Serializable
    data object DeleteAccount : PanoRoute

    @Serializable
    data object Translators : PanoRoute

    @Serializable
    data object BillingTroubleshoot : PanoRoute

    @Serializable
    data object Billing : PanoRoute, DeepLinkable

    @Serializable
    data object Import : PanoRoute

    @Serializable
    data object Export : PanoRoute

    @Serializable
    data object LoginFile : PanoRoute

    @Serializable
    data object LoginListenBrainz : PanoRoute

    @Serializable
    data object LoginCustomListenBrainz : PanoRoute

    @Serializable
    data object LoginPleroma : PanoRoute

//    @Serializable
//    data object LoginMaloja : PanoRoute

    @Serializable
    data object LoginGnufm : PanoRoute

    @Serializable
    data class AppList(
        val saveType: AppListSaveType,
        val packagesOverride: List<String>? = null,
        val preSelectedPackages: List<String>,
        val isSingleSelect: Boolean,
    ) : PanoRoute, HasFab {
        override fun getFabData() = PanoFabData(
            Res.string.done,
            Icons.Check,
            true,
            null
        )
    }

    @Serializable
    data class SimpleEditsAdd(val simpleEdit: SimpleEdit?) : PanoRoute

    @Serializable
    data object SimpleEdits : PanoRoute, HasFab {
        override fun getFabData() = PanoFabData(
            Res.string.add,
            Icons.Add,
            false,
            PanoRoute.SimpleEditsAdd(null)
        )
    }

    @Serializable
    data object RegexEditsTest : PanoRoute

    @Serializable
    data object RegexEdits : PanoRoute, HasFab {
        override fun getFabData() = PanoFabData(
            Res.string.add,
            Icons.Add,
            false,
            PanoRoute.RegexEditsAdd(null)
        )
    }

    @Serializable
    data class RegexEditsAdd(val regexEdit: RegexEdit?) : PanoRoute

    @Serializable
    data object BlockedMetadatas : PanoRoute, HasFab {
        override fun getFabData() = PanoFabData(
            Res.string.add,
            Icons.Add,
            false,
            route = Modal.BlockedMetadataAdd(
                blockedMetadata = null,
                ignoredArtist = null,
                hash = null
            )
        )
    }

    @Serializable
    data object ThemeChooser : PanoRoute, HasFab {
        override fun getFabData() = PanoFabData(
            Res.string.done,
            Icons.Check,
            true,
            null
        )
    }

    @Serializable
    data class ImageSearch(
        val artist: Artist? = null,
        val originalArtist: Artist? = null,
        val album: Album? = null,
        val originalAlbum: Album? = null,
    ) : PanoRoute, DeepLinkable

    @Serializable
    data object Onboarding : PanoRoute

    @Serializable
    data object Search : PanoRoute, DeepLinkable

    @Serializable
    data class WebView(
        val url: String,
        val userAccountTemp: UserAccountTemp? = null,
        val creds: PleromaOauthClientCreds? = null,
    ) : PanoRoute

    @Serializable
    data class OobPleromaAuth(
        val url: String,
        val userAccountTemp: UserAccountTemp,
        val creds: PleromaOauthClientCreds,
    ) : PanoRoute

    @Serializable
    data class OobLastfmLibreFmAuth(
        val userAccountTemp: UserAccountTemp,
    ) : PanoRoute

    @Serializable
    data class MusicEntryInfoPager(
        val artist: Artist,
        val user: UserCached,
        val entryType: Int,
        val appId: String? = null,
    ) : PanoRoute, DeepLinkable, HasTabs {

        override fun getTabsList(accountType: AccountType) = listOf(
            PanoTab.TopArtists,
            PanoTab.TopAlbums,
            PanoTab.TopTracks,
        )
    }

    @Serializable
    data class ChartsPager(
        val user: UserCached,
        val chartsType: Int,
    ) : PanoRoute, HasTabs {

        override fun getTabsList(accountType: AccountType) = listOf(
            PanoTab.TopArtists,
            PanoTab.TopAlbums,
            PanoTab.TopTracks,
        )
    }

    @Serializable
    data class SimilarTracks(
        val track: Track,
        val user: UserCached,
        val appId: String? = null,
    ) : PanoRoute, DeepLinkable

    @Serializable
    data class Random(val user: UserCached) : PanoRoute

    @Serializable
    data class TrackHistory(val track: Track, val user: UserCached) : PanoRoute, DeepLinkable


    @Serializable
    data object AutomationInfo : PanoRoute

    @Serializable
    data class Help(val searchTerm: String = "") : PanoRoute

    @Serializable
    data object PrivacyPolicy : PanoRoute

    @Serializable
    data object DiscordRpcSettings : PanoRoute

    @Serializable
    data object Blank : PanoRoute

    @Serializable
    sealed interface Modal : PanoRoute {

        @Serializable
        data class NavPopup(
            val otherUser: UserCached?,
            val initialDrawerData: DrawerData,
        ) : Modal

        @Serializable
        data class Changelog(val text: String) : Modal

        @Serializable
        data object ChartsLegend : Modal

        @Serializable
        data class UpdateAvailable(
            val updateAction: UpdateAction,
        ) : Modal


        @Serializable
        data object HiddenTags : Modal

        @Serializable
        data class CollageGenerator(
            val collageType: Int,
            val timePeriod: TimePeriod,
            val user: UserCached,
        ) : Modal, DeepLinkable

        @Serializable
        data class TagInfo(val tag: Tag) : Modal

        @Serializable
        data class MusicEntryInfo(
            val artist: Artist? = null,
            val album: Album? = null,
            val track: Track? = null,
            val user: UserCached,
            val appId: String? = null,
        ) : Modal, DeepLinkable

        @Serializable
        data object Index : Modal

        @Serializable
        data object FixIt : Modal

        @Serializable
        data class ShowLink(val url: String) : Modal

        @Serializable
        data class BlockedMetadataAdd(
            val blockedMetadata: BlockedMetadata?,
            val ignoredArtist: String? = null,
            val hash: Int? = null,
        ) : Modal, DeepLinkable

        @Serializable
        data class EditScrobble(
            val origScrobbleData: ScrobbleData,
            val msid: String? = null,
            val hash: Int? = null, // from notification
            val key: String? = null, // from main ui
        ) : Modal, DeepLinkable

        @Serializable
        data object MediaSearchPref : Modal
    }

    fun homePagerTabData(accountType: AccountType): List<PanoTab> {
        return when (accountType) {
            AccountType.LASTFM,
            AccountType.LISTENBRAINZ,
            AccountType.CUSTOM_LISTENBRAINZ,
                -> listOf(
                PanoTab.Scrobbles(),
                PanoTab.Following,
                PanoTab.Charts,
                PanoTab.Profile,
            )

            AccountType.LIBREFM,
            AccountType.GNUFM,
                -> listOf(
                PanoTab.Scrobbles(),
                PanoTab.Charts,
                PanoTab.Profile,
            )

            AccountType.PLEROMA,
            AccountType.FILE,
                -> listOf(
                PanoTab.Scrobbles(showChips = false),
                PanoTab.Profile,
            )
        }
    }
}