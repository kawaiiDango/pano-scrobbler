package com.arn.scrobble.navigation

import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.updates.UpdateAction
import kotlinx.serialization.Serializable


@Serializable
sealed interface PanoDialog {

    sealed interface NestedScrollable

    @Serializable
    data class NavPopup(
        val otherUser: UserCached?,
    ) : PanoDialog

    @Serializable
    data object Changelog : PanoDialog, NestedScrollable

    @Serializable
    data object ChartsLegend : PanoDialog

    @Serializable
    data class UpdateAvailable(
        val updateAction: UpdateAction,
    ) : PanoDialog, NestedScrollable


    @Serializable
    data object HiddenTags : PanoDialog

    @Serializable
    data class CollageGenerator(
        val collageType: Int,
        val timePeriod: TimePeriod,
        val user: UserCached,
    ) : PanoDialog

    @Serializable
    data class TagInfo(val tag: Tag) : PanoDialog, NestedScrollable

    @Serializable
    data class MusicEntryInfo(
        val artist: Artist? = null,
        val album: Album? = null,
        val track: Track? = null,
        val user: UserCached,
        val appId: String? = null,
    ) : PanoDialog, NestedScrollable

    // lightweight version to prevent ANRs
    @Serializable
    data class MusicEntryInfoFromWidget(
        val artist: String,
        val album: String? = null,
        val track: String? = null,
    ) : PanoDialog, NestedScrollable

    @Serializable
    data object Index : PanoDialog

    @Serializable
    data object FixIt : PanoDialog

    @Serializable
    data class ShowLink(val url: String) : PanoDialog

    @Serializable
    data class BlockedMetadataAdd(
        val blockedMetadata: BlockedMetadata = BlockedMetadata(),
        val ignoredArtist: String? = null,
        val hash: Int? = null,
    ) : PanoDialog

    @Serializable
    data class EditScrobble(
        val scrobbleData: ScrobbleData,
        val msid: String? = null,
        val hash: Int? = null, // from notification
        val key: String? = null, // from main ui
    ) : PanoDialog, NestedScrollable

}