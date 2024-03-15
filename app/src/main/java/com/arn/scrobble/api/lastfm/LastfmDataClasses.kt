package com.arn.scrobble.api.lastfm

import android.os.Parcelable
import com.arn.scrobble.search.SearchResultsAdapter
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonNames


enum class ImageSize {
    small, medium, large, extralarge
}

sealed class MusicEntry : Parcelable {
    abstract val name: String
    abstract val url: String?
    abstract val mbid: String?
    abstract val msid: String?
    abstract val listeners: Int?
    abstract val playcount: Int?
    abstract val userplaycount: Int?
    abstract val tags: Tags?
    abstract val wiki: Wiki?
    abstract val match: Float?
    protected abstract val _attr: MusicEntryAttr?
    val rank: Int? get() = _attr?.rank
    var stonksDelta: Int? = null
}

interface IHasImage {
    val image: List<LastFmImage>?
}

val IHasImage.webp300
    get() = image
        ?.find { it.size == ImageSize.extralarge.name }
        ?.let {
            val url = it.url
            if (url.endsWith(".png") || url.endsWith(".jpg"))
                "$url.webp"
            else
                url
        }

val IHasImage.webp600
    get() = webp300?.replace("300x300", "600x600")

@Serializable
data class Wiki(
    val content: String
)

@Parcelize
@Serializable
data class Tag(
    val name: String,
    val url: String? = null,
    @Serializable(with = StringOrIntSerializer::class)
    val reach: Int? = null,
    @Serializable(with = StringOrIntSerializer::class)
    @JsonNames("total")
    val count: Int? = null,
    @JsonNames("bio")
    @IgnoredOnParcel
    val wiki: Wiki? = null,
) : Parcelable

@Serializable
data class TagGetInfoResponse(
    val tag: Tag,
)

@Serializable
data class Stats(
    @Serializable(with = StringOrIntSerializer::class)
    val listeners: Int,
    @Serializable(with = StringOrIntSerializer::class)
    val playcount: Int,
    @Serializable(with = StringOrIntSerializer::class)
    val userplaycount: Int?
)

@Parcelize
@Serializable
data class LastFmImage(
    val size: String,
    @SerialName("#text")
    val url: String
) : Parcelable

@Parcelize
@Serializable
data class Artist(
    @JsonNames("#text")
    override val name: String,
    override val url: String? = null,
    @Transient
    override val mbid: String? = null, // lastfm sometimes provides invalid mbids, so we ignore them
    @Transient
    override val msid: String? = null,
    @SerialName("stats")
    @IgnoredOnParcel
    private val _stats: Stats? = null,
    override val listeners: Int? = _stats?.listeners,
    override val playcount: Int? = _stats?.playcount,
    override val userplaycount: Int? = _stats?.userplaycount,
    @Serializable(with = StringOrFloatSerializer::class)
    override val match: Float? = null,
    @Serializable(with = StringOrBoolSerializer::class)
    val ontour: Boolean? = null,
    @IgnoredOnParcel
    val similar: Artists? = null,
    @JsonNames("toptags")
    @IgnoredOnParcel
    @Serializable(with = TagsOrStringSerializer::class)
    override val tags: Tags? = null,
    @JsonNames("bio")
    @IgnoredOnParcel
    override val wiki: Wiki? = null,
    @SerialName("@attr")
    override val _attr: MusicEntryAttr? = null,
) : MusicEntry()

@Parcelize
@Serializable
data class Album(
    @JsonNames("#text", "title")
    override val name: String,
    @Serializable(with = ArtistOrStringSerializer::class)
    val artist: Artist? = null,
    override val url: String? = null,
    override val image: List<LastFmImage>? = null,
    @Transient
    override val mbid: String? = null, // lastfm sometimes provides invalid mbids, so we ignore them
    @Transient
    override val msid: String? = null,
    @Serializable(with = StringOrIntSerializer::class)
    override val listeners: Int? = null,
    @Serializable(with = StringOrIntSerializer::class)
    override val playcount: Int? = null,
    @Serializable(with = StringOrIntSerializer::class)
    override val userplaycount: Int? = null,
    @Serializable(with = StringOrFloatSerializer::class)
    override val match: Float? = null,
    @JsonNames("toptags")
    @IgnoredOnParcel
    @Serializable(with = TagsOrStringSerializer::class)
    override val tags: Tags? = null,
    @JsonNames("bio")
    @IgnoredOnParcel
    override val wiki: Wiki? = null,
    @IgnoredOnParcel
    val tracks: Tracks? = null,
    @SerialName("@attr")
    override val _attr: MusicEntryAttr? = null,
) : MusicEntry(), IHasImage

@Parcelize
@Serializable
data class Track(
    override val name: String,
    val album: Album?,
    @Serializable(with = ArtistOrStringSerializer::class)
    val artist: Artist,
    override val url: String? = null,
    @Serializable(with = StringSecsToMsSerializer::class)
    val duration: Long? = null,
    @Serializable(with = LastfmUnixTimestampSerializer::class)
    val date: Long? = null,
    @SerialName("image")
    private val _image: List<LastFmImage>? = null,
    @Transient
    override val mbid: String? = null, // lastfm sometimes provides invalid mbids, so we ignore them
    @Transient
    override val msid: String? = null,
    @Serializable(with = StringOrIntSerializer::class)
    override val listeners: Int? = null,
    @Serializable(with = StringOrIntSerializer::class)
    override val playcount: Int? = null,
    @Serializable(with = StringOrIntSerializer::class)
    override val userplaycount: Int? = null,
    @Serializable(with = StringOrFloatSerializer::class)
    override val match: Float? = null,
    @Serializable(with = StringOrBoolSerializer::class)
    @JsonNames("loved")
    val userloved: Boolean? = null,
    @Transient
    val userHated: Boolean? = null,
    @JsonNames("toptags")
    @IgnoredOnParcel
    @Serializable(with = TagsOrStringSerializer::class)
    override val tags: Tags? = null,
    @JsonNames("bio")
    @IgnoredOnParcel
    override val wiki: Wiki? = null,
    @SerialName("@attr")
    override val _attr: MusicEntryAttr? = null,
    val isNowPlaying: Boolean = _attr?.nowplaying == true
) : MusicEntry(), IHasImage {
    @IgnoredOnParcel
    val feedbackScore = if (userloved == true) 1 else if (userHated == true) -1 else 0

    // album?.image has more priority since _image can be a white star
    @IgnoredOnParcel
    override val image get() = album?.image ?: _image
}

@Parcelize
@Serializable
data class MusicEntryAttr(
    @Serializable(with = StringOrBoolSerializer::class)
    val nowplaying: Boolean?,
    @Serializable(with = StringOrIntSerializer::class)
    val rank: Int? = null,
) : Parcelable

@Serializable
data class PageEntries<T>(
    @JsonNames("track", "artist", "album", "user", "tag")
    val entries: List<T>,
    @SerialName("@attr")
    val attr: PageAttr? = null,
)

//data class Entries<T>(
//    @JsonNames("track", "artist", "album", "user", "tag")
//    val entries: List<T>,
//)
@Serializable
data class Tags(
    @Serializable(with = ArrayOrObjectTagSerializer::class)
    val tag: List<Tag>
)

@Serializable
data class Tracks(
    @Serializable(with = ArrayOrObjectTrackSerializer::class)
    val track: List<Track>,
)

@Serializable
data class Albums(
    @Serializable(with = ArrayOrObjectAlbumSerializer::class)
    val album: List<Album>,
)

@Serializable
data class Artists(
    @Serializable(with = ArrayOrObjectArtistSerializer::class)
    val artist: List<Artist>,
)

@Serializable
data class PageAttr(
    @Serializable(with = StringOrIntSerializer::class)
    val page: Int,
    @Serializable(with = StringOrIntSerializer::class)
    val totalPages: Int,
    @Serializable(with = StringOrIntSerializer::class)
    val total: Int?,
)

data class PageResult<T>(
    val attr: PageAttr,
    val entries: List<T>,
)


@Serializable
data class UserGetInfoResponse(
    val user: User,
)

@Serializable
data class User(
    val name: String,
    val url: String,
    val realname: String? = null,
    @Serializable(with = StringOrIntSerializer::class)
    val playcount: Int? = null,
    @Serializable(with = StringOrIntSerializer::class)
    val artist_count: Int? = null,
    @Serializable(with = StringOrIntSerializer::class)
    val playlists: Int? = null,
    @Serializable(with = StringOrIntSerializer::class)
    val track_count: Int? = null,
    @Serializable(with = StringOrIntSerializer::class)
    val album_count: Int? = null,
    override val image: List<LastFmImage>? = null,
    @Serializable(with = LastfmUnixTimestampSerializer::class)
    val registered: Long? = null,
    val country: String? = null,
) : IHasImage

@Serializable
data class RecentTracksResponse(
    val recenttracks: PageEntries<Track>,
)


enum class Period(val value: String) {
    WEEK("7day"),
    MONTH("1month"),
    QUARTER("3month"),
    HALF_YEAR("6month"),
    YEAR("12month"),
    OVERALL("overall"),
}

@Serializable
data class Session(
    val name: String?,
    val key: String
)

@Serializable
data class SessionResponse(
    val session: Session
)

@Serializable
data class LastfmResult(
    val status: String,
) {
    val isOk get() = status == "ok"
}

@Serializable
data class GnuFmResult(
    val lfm: LastfmResult
)

@Serializable
data class TopTracksResponse(
    val toptracks: PageEntries<Track>,
)


@Serializable
data class TopArtistsResponse(
    val topartists: PageEntries<Artist>
)


@Serializable
data class TopAlbumsResponse(
    val topalbums: PageEntries<Album>
)


@Serializable
data class WeeklyTrackChartResponse(
    val weeklytrackchart: Tracks,
)

@Serializable
data class WeeklyArtistChartResponse(
    val weeklyartistchart: Artists
)

@Serializable
data class WeeklyAlbumChartResponse(
    val weeklyalbumchart: Albums
)

@Serializable
data class TagsResponse(
    @JsonNames("tags")
    val toptags: Tags
)

@Serializable
data class FriendsResponse(
    val friends: PageEntries<User>,
)


@Serializable
data class LovedTracksResponse(
    val lovedtracks: PageEntries<Track>,
)


@Serializable(with = ApiErrorDeserializer::class)
data class ApiErrorResponse(
    val code: Int,
    val message: String
)

@Serializable
data class ArtistInfoResponse(
    val artist: Artist,
)

@Serializable
data class TrackInfoResponse(
    val track: Track,
)

@Serializable
data class AlbumInfoResponse(
    val album: Album,
)

@Serializable
data class SimilarArtistsResponse(
    val similarartists: Artists,
)

@Serializable
data class SimilarTracksResponse(
    val similartracks: Tracks,
)

@Serializable
data class ArtistSearchResponse(
    val results: ArtistMatches,
) {
    @Serializable
    data class ArtistMatches(
        val artistmatches: PageEntries<Artist>,
    )
}

@Serializable
data class AlbumSearchResponse(
    val results: AlbumMatches,
) {
    @Serializable
    data class AlbumMatches(
        val albummatches: PageEntries<Album>,
    )
}

@Serializable
data class TrackSearchResponse(
    val results: TrackMatches,
) {
    @Serializable
    data class TrackMatches(
        val trackmatches: PageEntries<Track>,
    )
}

@Serializable
data class TrackScrobblesResponse(
    val trackscrobbles: PageEntries<Track>,
)


@Serializable
data class NowPlayingResponse(
    val nowplaying: ScrobbleDetails
)

@Serializable
data class ScrobbleDetails(
    val artist: CorrectedDetails,
    val track: CorrectedDetails,
    val albumArtist: CorrectedDetails,
    val album: CorrectedDetails,
    val ignoredMessage: IgnoredDetails,
)

@Serializable
data class CorrectedDetails(
    @Serializable(with = StringOrBoolSerializer::class)
    val corrected: Boolean,
    @SerialName("#text")
    val name: String?
)

@Serializable
data class IgnoredDetails(
    @Serializable(with = StringOrIntSerializer::class)
    val code: Int,
    @SerialName("#text")
    val message: String
)

@Serializable
data class ScrobbleResponse(
    val scrobbles: Scrobbles,
) {
    @Serializable
    data class Scrobbles(
        @SerialName("@attr")
        val attr: Attr,
    ) {
        @Serializable
        data class Attr(
            @Serializable(with = StringOrIntSerializer::class)
            val accepted: Int = 0,
            @Serializable(with = StringOrIntSerializer::class)
            val ignored: Int,
        )
    }
}

@Serializable
data class DeleteScrobbleResponse(
    val result: Boolean,
)


data class SearchResults(
    val term: String,
    val searchType: SearchResultsAdapter.SearchType,
    val lovedTracks: List<Track>,
    val tracks: List<Track>,
    val artists: List<Artist>,
    val albums: List<Album>,
) {
    val isEmpty: Boolean
        get() = lovedTracks.isEmpty() && tracks.isEmpty() && artists.isEmpty() && albums.isEmpty()
}