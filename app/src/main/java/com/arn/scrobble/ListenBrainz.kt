package com.arn.scrobble

import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL


class ListenBrainz(private val token: String) {
    private var apiRoot = Stuff.LISTENBRAINZ_API_ROOT

    fun setApiRoot(url: String): ListenBrainz {
        apiRoot = url
        return this
    }

    private fun submitListens(
        scrobbledatas: List<ScrobbleData>,
        listenType: String
    ): ScrobbleResult {
        val payload = JSONArray()
        scrobbledatas.forEach {
            val payloadTrack = JSONObject()
                .put(
                    "track_metadata", JSONObject()
                        .put("artist_name", it.artist ?: "")
                        .put("release_name", it.album ?: "")
                        .put("track_name", it.track ?: "")
                )
            if (listenType != "playing_now")
                payloadTrack.put("listened_at", it.timestamp)
            payload.put(payloadTrack)
        }
        val data = JSONObject()
            .put("listen_type", listenType)
            .put("payload", payload)

        val url = URL("${apiRoot}1/submit-listens")

        try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "token $token")
                .post(
                    data.toString().toRequestBody("application/json".toMediaType())
                )
                .build()

            return LFMRequester.okHttpClient.newCall(request).execute()
                .use { response ->
                    var errMsg = ""
                    val bodyJson = JSONObject(response.body!!.string())
                    if (bodyJson.has("status"))
                        errMsg = bodyJson.getString("status")
                    else if (bodyJson.has("error")) {
                        errMsg = bodyJson.getString("error")
                    }

                    ScrobbleResult.createHttp200OKResult(
                        response.code,
                        response.message, errMsg
                    )
                }

        } catch (e: Exception) {
            e.printStackTrace()

            return if (Thread.interrupted())
                ScrobbleResult.createHttp200OKResult(
                    200,
                    e.message,
                    "ok"
                ) //suppress err notification
            else
                ScrobbleResult.createHttp200OKResult(0, e.message, "")
        }
    }

    fun updateNowPlaying(scrobbledata: ScrobbleData) =
        submitListens(listOf(scrobbledata), "playing_now")

    fun scrobble(scrobbledata: ScrobbleData) = submitListens(listOf(scrobbledata), "single")
    fun scrobble(scrobbledatas: MutableList<ScrobbleData>) = submitListens(scrobbledatas, "import")

    fun recents(username: String, limit: Int): JSONObject? {
        val url = URL("${apiRoot}1/user/$username/listens?count=$limit")
        try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "token $token")
                // this works even without the token or an incorrect token
                .build()

            return LFMRequester.okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful)
                    JSONObject(response.body!!.string())
                else
                    null
            }
        } catch (e: IOException) {
        }

        return null
    }

    fun username(): String? {
        val url = URL("${apiRoot}1/validate-token")

        var username: String? = null
        try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "token $token")
                .build()
            LFMRequester.okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respJson = JSONObject(response.body!!.string())
                    if (respJson.getBoolean("valid"))
                        username = respJson.getString("user_name")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return username
    }

}