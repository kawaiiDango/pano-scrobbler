package com.arn.scrobble.api.listenbrainz

import kotlinx.serialization.Serializable


@Serializable
data class ValidateToken(
    val valid: Boolean,
    val user_name: String?,
)

@Serializable
data class ListenBrainzSubmitResponse(
    val code: Int?,
    val status: String?,
    val error: String?,
    val recording_msid: String?,
) {
    val isOk get() = status == "ok"
}

@Serializable
data class ListenBrainzListen(
    val listen_type: ListenBrainzListenType,
    val payload: List<ListenBrainzPayload>
)

@Serializable
data class ListenBrainzPayload(
    @Serializable(with = TimeSerializer::class)
    val listened_at: Long?,
    val track_metadata: ListenBrainzTrackMetadata
)

@Serializable
data class ListenBrainzTrackMetadata(
    val artist_name: String,
    val release_name: String?,
    val track_name: String,
    val additional_info: ListenBrainzAdditionalInfo?,
    val mbid_mapping: ListenBrainzMbidMapping? = null,
)

@Serializable
data class ListenBrainzAdditionalInfo(
    val duration_ms: Long?,
    val artist_msid: String? = null,
    val recording_msid: String? = null,
    val release_msid: String? = null,
    val media_player: String? = null,
    val media_player_version: String? = null,
    val submission_client: String?,
    val submission_client_version: String?,
)

@Serializable
data class ListenBrainzMbidLookup(
    val artist_credit_name: String?,
    val artist_mbids: List<String>?,
    val recording_mbid: String?,
    val recording_name: String?,
    val release_mbid: String?,
    val release_name: String?,
)

@Serializable
data class ListenBrainzFeedback(
    val recording_mbid: String?,
    val recording_msid: String?,
    val score: Int
)

@Serializable
data class ListenBrainzDeleteRequest(
    @Serializable(with = TimeSerializer::class)
    val listened_at: Long,
    val recording_msid: String
)

@Serializable
data class ListenBrainzListensData(
    val payload: ListenBrainzListensPayload
) {
    @Serializable
    data class ListenBrainzListensPayload(
        val count: Int,
        val listens: List<ListenBrainzListensListens>,
        @Serializable(with = TimeSerializer::class)
        val latest_listen_ts: Long?,
        @Serializable(with = TimeSerializer::class)
        val oldest_listen_ts: Long?,
    )
}

@Serializable
data class ListenBrainzListensListens(
    @Serializable(with = TimeSerializer::class)
    val inserted_at: Long?,
    @Serializable(with = TimeSerializer::class)
    val listened_at: Long?,
    val recording_msid: String?,
    val playing_now: Boolean?,
    val track_metadata: ListenBrainzTrackMetadata,
)

@Serializable
data class ListenBrainzMbidMapping(
    val artist_mbids: List<String>?,
    val recording_mbid: String?,
    val release_mbid: String?
)

@Serializable
data class ListenBrainzFeedbacks(
    val count: Int,
    val total_count: Int,
    val feedback: List<ListenBrainzFeedbackPayload>
)

@Serializable
data class ListenBrainzFeedbackPayload(
    @Serializable(with = TimeSerializer::class)
    val created: Long,
    val recording_mbid: String?,
    val recording_msid: String?,
    val score: Int,
    val track_metadata: ListenBrainzTrackMetadata?
)

@Serializable
data class ListenBrainzFollowing(
    val following: List<String>,
    val user: String,
)

@Serializable
data class ListenBrainzCountData(
    val payload: ListenBrainzCountPayload
) {
    @Serializable
    data class ListenBrainzCountPayload(
        val count: Int
    )
}

@Serializable
data class ListenBrainzStatsEntriesData(
    val payload: ListenBrainzStatsEntriesPayload
) {
    @Serializable
    data class ListenBrainzStatsEntriesPayload(
        val artists: List<ListenBrainzStatsEntry>?,
        val releases: List<ListenBrainzStatsEntry>?,
        val recordings: List<ListenBrainzStatsEntry>?,
        val count: Int,
        @Serializable(with = TimeSerializer::class)
        val from_ts: Long,
        @Serializable(with = TimeSerializer::class)
        val last_updated: Long,
        val offset: Int,
        val range: String,
        @Serializable(with = TimeSerializer::class)
        val to_ts: Long,
        val total_artist_count: Int?,
        val total_release_count: Int?,
        val total_recording_count: Int?,
    )
}

@Serializable
data class ListenBrainzStatsEntry(
    val artist_mbids: List<String>?,
    val artist_name: String,
    val recording_mbid: String?,
    val release_mbid: String?,
    val release_name: String?,
    val track_name: String?,
    val listen_count: Int,
)

@Serializable
data class ListenBrainzActivityData(
    val payload: ListenBrainzActivityPayload
) {
    @Serializable
    data class ListenBrainzActivityPayload(
        @Serializable(with = TimeSerializer::class)
        val from_ts: Long,
        @Serializable(with = TimeSerializer::class)
        val last_updated: Long,
        val listening_activity: List<ListenBrainzListeningActivity>,
        @Serializable(with = TimeSerializer::class)
        val to_ts: Long
    )
}

@Serializable
data class ListenBrainzListeningActivity(
    @Serializable(with = TimeSerializer::class)
    val from_ts: Long,
    val listen_count: Int,
    val time_range: String,
    @Serializable(with = TimeSerializer::class)
    val to_ts: Long
)

enum class ListenBrainzRange {
    this_week, this_month, this_year, week, month, year, quarter, half_yearly, all_time
}

enum class ListenBrainzListenType {
    playing_now, single, import
}