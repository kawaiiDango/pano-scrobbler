package com.arn.scrobble.navigation

import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.pref.AppListSaveType
import kotlinx.serialization.Serializable

@Serializable
sealed interface PanoRoute {

    data object SpecialGoBack : PanoRoute

    @Serializable
    data object SelfHomePager : PanoRoute

    @Serializable
    data class OthersHomePager(val user: UserCached) : PanoRoute

    @Serializable
    data object OssCredits : PanoRoute

    @Serializable
    data object Prefs : PanoRoute

    @Serializable
    data object DeleteAccount : PanoRoute

    @Serializable
    data object Translators : PanoRoute

    @Serializable
    data object BillingTroubleshoot : PanoRoute

    @Serializable
    data object Billing : PanoRoute

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

    @Serializable
    data object LoginMaloja : PanoRoute

    @Serializable
    data object LoginGnufm : PanoRoute

    @Serializable
    data class AppList(
        val saveType: AppListSaveType,
        val preSelectedPackages: List<String>,
        val isSingleSelect: Boolean,
    ) : PanoRoute

    @Serializable
    data class SimpleEditsAdd(val simpleEdit: SimpleEdit?) : PanoRoute

    @Serializable
    data object SimpleEdits : PanoRoute

    @Serializable
    data object RegexEditsTest : PanoRoute

    @Serializable
    data object RegexEdits : PanoRoute

    @Serializable
    data class RegexEditsAdd(val regexEdit: RegexEdit?) : PanoRoute

    @Serializable
    data object BlockedMetadatas : PanoRoute

    @Serializable
    data object ThemeChooser : PanoRoute

    @Serializable
    data class ImageSearch(
        val artist: Artist? = null,
        val originalArtist: Artist? = null,
        val album: Album? = null,
        val originalAlbum: Album? = null,
    ) : PanoRoute

    @Serializable
    data object Onboarding : PanoRoute

    @Serializable
    data object Search : PanoRoute

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
    data class OobLibreFmAuth(
        val userAccountTemp: UserAccountTemp,
    ) : PanoRoute

    @Serializable
    data class MusicEntryInfoPager(
        val artist: Artist,
        val user: UserCached,
        val type: Int,
        val pkgName: String? = null,
    ) : PanoRoute

    @Serializable
    data class ChartsPager(
        val user: UserCached,
        val type: Int,
    ) : PanoRoute

    @Serializable
    data class SimilarTracks(
        val track: Track,
        val user: UserCached,
        val pkgName: String? = null,
    ) : PanoRoute

    @Serializable
    data class Random(val user: UserCached) : PanoRoute

    @Serializable
    data class TrackHistory(val track: Track, val user: UserCached) : PanoRoute


    @Serializable
    data object AutomationInfo : PanoRoute

    @Serializable
    data object Help : PanoRoute

    @Serializable
    data object Exit : PanoRoute
}