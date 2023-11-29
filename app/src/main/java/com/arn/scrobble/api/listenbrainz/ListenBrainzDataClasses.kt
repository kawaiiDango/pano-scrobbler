package com.arn.scrobble.api.listenbrainz

import kotlinx.serialization.Serializable


@Serializable
data class ValidateToken(
    val valid: Boolean,
    val user_name: String?,
)

@Serializable
data class ListenBrainzResponse(
    val code: Int?,
    val status: String?,
    val error: String?,
) {
    val isOk get() = status == "ok"
}

@Serializable
data class ListenBrainzListen(
    val listen_type: String,
    val payload: List<ListenBrainzPayload>
)

@Serializable
data class ListenBrainzPayload(
    val listened_at: Int?,
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
    val duration_ms: Int?,
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
    val listened_at: Int,
    val recording_msid: String
)

@Serializable
data class ListenBrainzData<T>(
    val payload: T
)

@Serializable
data class ListenBrainzListensPayload(
    val count: Int,
    val listens: List<ListenBrainzListensListens>,
)

@Serializable
data class ListenBrainzListensListens(
    val inserted_at: Int?,
    val listened_at: Int?,
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
    val created: Int,
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
data class ListenBrainzCountPayload(
    val count: Int
)

@Serializable
data class ListenBrainzStatsEntriesPayload(
    val artists: List<ListenBrainzStatsEntry>?,
    val releases: List<ListenBrainzStatsEntry>?,
    val recordings: List<ListenBrainzStatsEntry>?,
    val count: Int,
    val from_ts: Int,
    val last_updated: Int,
    val offset: Int,
    val range: String,
    val to_ts: Int,
    val total_artist_count: Int?,
    val total_release_count: Int?,
    val total_recording_count: Int?,
)

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
data class ListenBrainzActivityPayload(
    val from_ts: Int,
    val last_updated: Int,
    val listening_activity: List<ListeningActivity>,
    val to_ts: Int
)

@Serializable
data class ListeningActivity(
    val from_ts: Int,
    val listen_count: Int,
    val time_range: String,
    val to_ts: Int
)