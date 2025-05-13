package com.arn.scrobble.mic

import android.animation.ValueAnimator
import android.media.MediaRecorder
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.acrcloud.rec.ACRCloudClient
import com.acrcloud.rec.ACRCloudConfig
import com.acrcloud.rec.ACRCloudResult
import com.acrcloud.rec.IACRCloudListener
import com.arn.scrobble.Tokens
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.media.PlayingTrackInfo
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.json.JSONException
import org.json.JSONObject
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.listening
import pano_scrobbler.composeapp.generated.resources.network_error
import pano_scrobbler.composeapp.generated.resources.not_found
import pano_scrobbler.composeapp.generated.resources.recording_failed
import pano_scrobbler.composeapp.generated.resources.unavailable_offline

class MicScrobbleVM : ViewModel(), IACRCloudListener {

    private val context = AndroidStuff.application
    var started = false
    private var inited = false
    val progressValue = MutableStateFlow(0)
    val fadeValue = MutableStateFlow(1f)
    val statusText = MutableStateFlow("")
    private val _rateLimitedEvent = MutableSharedFlow<Unit>()
    val rateLimitedEvent = _rateLimitedEvent.asSharedFlow()
    private val _scrobbleEvent = MutableSharedFlow<Unit>()
    val scrobbleEvent = _scrobbleEvent.asSharedFlow()
    private var progressAnimator: ValueAnimator? = null
    private var stopJob: Job? = null
    private var scrobbleJob: Job? = null

    private val acrConfig by lazy {
        ACRCloudConfig().also {
            it.acrcloudListener = this@MicScrobbleVM
            it.context = context
            it.host = Tokens.ACR_HOST
            it.accessKey = Tokens.ACR_KEY
            it.accessSecret = Tokens.ACR_SECRET

            if (PlatformStuff.isTv) // this intermittently works on TV
                it.recorderConfig.source = MediaRecorder.AudioSource.VOICE_RECOGNITION
        }
    }

    private val client by lazy { ACRCloudClient() }

    fun start() {
        viewModelScope.launch {
            _start()
        }
    }

    private suspend fun _start() {
        if (started) return

        if (!Stuff.isOnline) {
            statusText.value = getString(Res.string.unavailable_offline)
            return
        }

        inited = client.initWithConfig(acrConfig) && client.startRecognize()
        if (!inited) {
            statusText.value = getString(Res.string.recording_failed)
            return
        }

        statusText.value = getString(Res.string.listening)

        stopJob?.cancel()
        stopJob = viewModelScope.launch {
            delay(DURATION)
            stop()
        }

        started = true

        if (progressAnimator?.isRunning == true)
            progressAnimator?.cancel()
        progressAnimator =
            ValueAnimator.ofInt(0, 1000).apply {
                duration = DURATION
                interpolator = LinearInterpolator()
                addUpdateListener {
                    progressValue.value = it.animatedValue as Int
                }
                start()
            }
    }

    fun stop() {
        if (!started) return
        started = false
        stopJob?.cancel()
        runCatching { client.stopRecordToRecognize() }
        runCatching { client.release() }

        val startValue = if (progressAnimator?.isRunning == true) {
            progressAnimator?.cancel()
            progressAnimator!!.animatedValue as Int
        } else
            1000

        progressAnimator = ValueAnimator.ofInt(startValue, 0).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progressValue.value = it.animatedValue as Int
            }
            start()
        }

        statusText.value = ""
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    override fun onResult(acrCloudResult: ACRCloudResult) {
        viewModelScope.launch {
            _onResult(acrCloudResult)
        }
    }

    private suspend fun _onResult(acrCloudResult: ACRCloudResult) {
        val result = acrCloudResult.result ?: return
        var artist = ""
        var album = ""
        var title = ""
        var statusCode: Int
        var statusMsg = ""

        if (started)
            stop()
        try {
            val j = JSONObject(result)
            val status = j.getJSONObject("status")
            statusCode = status.getInt("code")
            statusMsg = status.getString("msg")
            if (statusCode == 0) {
                val metadata = j.getJSONObject("metadata")
                val entries = if (metadata.has("humming"))
                    metadata.getJSONArray("humming")
                else if (metadata.has("music"))
                    metadata.getJSONArray("music")
                else
                    throw NullPointerException("ACRCloud metadata is null")
                for (i in 0 until entries.length()) {
                    val tt = entries.get(i) as JSONObject
                    title = tt.getString("title")
                    if (tt.has("album"))
                        album = tt.getJSONObject("album").getString("name")
                    val artistt = tt.getJSONArray("artists")
                    val art = (artistt.get(0) as JSONObject)
                    artist = art.getString("name")
                }
            }
        } catch (e: JSONException) {
            statusCode = -1
            Logger.w(e) { "Acrcloud response parse failed" }
        }


        when (statusCode) {
            0 -> {
                viewModelScope.launch {
                    statusText.emit(
                        "⌛ " + Stuff.formatBigHyphen(artist, title)
                    )

                    _scrobbleEvent.emit(Unit)

                    val trackInfo = PlayingTrackInfo(
                        context.packageName,
                        "acr",
                        playStartTime = System.currentTimeMillis()
                    )

                    trackInfo.putOriginals(artist, title, album, "")

                    scrobbleJob = launch(Dispatchers.IO) {
                        delay(Stuff.SCROBBLE_FROM_MIC_DELAY)
                        ScrobbleEverywhere.scrobble(false, trackInfo)

                        statusText.emit(
                            "✅ " + Stuff.formatBigHyphen(artist, title)
                        )
                    }
                }
            }

            1001 -> statusText.value = getString(Res.string.not_found)
            2000 -> {
                if (acrConfig.recorderConfig.source != MediaRecorder.AudioSource.VOICE_RECOGNITION) {
                    acrConfig.recorderConfig.source = MediaRecorder.AudioSource.VOICE_RECOGNITION
                    start()
                } else
                    statusText.value = getString(Res.string.recording_failed)
            }

            2001 -> statusText.value = getString(Res.string.recording_failed)
            3003, 3015 -> _rateLimitedEvent.tryEmit(Unit)
            else -> {
                Logger.w { "rec error: $statusCode - $statusMsg" }
                statusText.value = getString(Res.string.network_error)
            }
        }
    }

    override fun onVolumeChanged(vol: Double) {
        fadeValue.value = (0.1f + vol.toFloat()).coerceAtMost(1f)
    }

    fun cancelScrobble() {
        scrobbleJob?.cancel()
        statusText.value = ""
    }

    companion object {
        private const val DURATION = 10000L
    }
}