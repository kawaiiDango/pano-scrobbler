package com.arn.scrobble.navigation

import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.friends.UserCached
import kotlinx.serialization.Serializable

@Serializable
sealed interface PanoRoute {

    data object SpecialGoBack : PanoRoute

    @Serializable
    data object Placeholder : PanoRoute

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
        val hasPreSelection: Boolean,
        val preSelectedPackages: List<String>,
        val isSingleSelect: Boolean
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
    data class BlockedMetadataAdd(
        val blockedMetadata: BlockedMetadata = BlockedMetadata(skip = true),
        val ignoredArtist: String?,
        val hash: Int?
    ) : PanoRoute

    @Serializable
    data object ThemeChooser : PanoRoute

    @Serializable
    data class ImageSearch(
        val musicEntry: MusicEntry,
        val originalMusicEntry: MusicEntry?
    ) : PanoRoute

    @Serializable
    data object Onboarding : PanoRoute

    @Serializable
    data class Search(val user: UserCached) : PanoRoute

    @Serializable
    data object Rec : PanoRoute

    @Serializable
    data class WebView(
        val url: String,
        val userAccountTemp: UserAccountTemp? = null,
        val creds: PleromaOauthClientCreds? = null
    ) : PanoRoute

    @Serializable
    data class EditScrobble(val scrobbleData: ScrobbleData, val msid: String?, val hash: Int) :
        PanoRoute

    @Serializable
    data class TagInfo(val tag: Tag) : PanoRoute

    @Serializable
    data class MusicEntryInfo(
        val musicEntry: MusicEntry,
        val pkgName: String?,
        val user: UserCached
    ) : PanoRoute

    @Serializable
    data class CollageGenerator(
        val collageType: Int,
        val timePeriod: TimePeriod,
        val user: UserCached,
    ) : PanoRoute

    @Serializable
    data class TrackHistory(val track: Track, val user: UserCached) : PanoRoute

    @Serializable
    data object Index : PanoRoute
}