package com.arn.scrobble

import android.animation.ValueAnimator
import android.app.Application
import android.media.MediaRecorder
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.acrcloud.rec.ACRCloudClient
import com.acrcloud.rec.ACRCloudConfig
import com.acrcloud.rec.ACRCloudResult
import com.acrcloud.rec.IACRCloudListener
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class RecVM(application: Application) : AndroidViewModel(application), IACRCloudListener {

    private val prefs = App.prefs
    var started = false
    private var inited = false
    val progressValue = MutableLiveData<Int>()
    val fadeValue = MutableLiveData<Float>()
    val statusText = MutableLiveData<String>()
    val rateLimitedEvent = LiveEvent<Unit>()
    val scrobbleEvent = LiveEvent<Unit>()
    private var progressAnimator: ValueAnimator? = null
    private var stopJob: Job? = null
    var scrobbleJob: Job? = null
    private var delayDeferred: Deferred<Unit>? = null

    private val acrConfig by lazy {
        ACRCloudConfig().apply {
            acrcloudListener = this@RecVM
            context = getApplication()
            host = prefs.acrcloudHost ?: Tokens.ACR_HOST
            accessKey = prefs.acrcloudKey ?: Tokens.ACR_KEY
            accessSecret = prefs.acrcloudSecret ?: Tokens.ACR_SECRET
        }
    }

    private val client by lazy { ACRCloudClient() }


    fun start() {
        if (started) return

        if (!Stuff.isOnline) {
            statusText.value = App.context.getString(R.string.unavailable_offline)
            return
        }

        inited = client.initWithConfig(acrConfig) && client.startRecognize()
        if (!inited) {
            statusText.value = App.context.getString(R.string.recording_failed)
            return
        }

        statusText.value = App.context.getString(R.string.listening)

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
    }

    override fun onResult(acrCloudResult: ACRCloudResult) {
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
            Stuff.logW(result)
            e.printStackTrace()
        }


        when (statusCode) {
            0 -> {
//                binding.recImg.setImageResource(R.drawable.vd_check_simple)
                statusText.value =
                    "✅ " + App.context.getString(R.string.artist_title, artist, title)
                scrobbleEvent.value = Unit

                val trackInfo = PlayingTrackInfo(
                    App.context.packageName,
                    playStartTime = System.currentTimeMillis()
                )

                trackInfo.putOriginals(artist, title, album, "")

                scrobbleJob = GlobalScope.launch {
                    delay(Stuff.SCROBBLE_FROM_MIC_DELAY.toLong())
                    LFMRequester(this).scrobble(false, trackInfo)
                }
            }

            1001 -> statusText.value = App.context.getString(R.string.not_found)
            2000 -> {
                if (acrConfig.recorderConfig.source != MediaRecorder.AudioSource.VOICE_RECOGNITION) {
                    acrConfig.recorderConfig.source = MediaRecorder.AudioSource.VOICE_RECOGNITION
                    start()
                } else
                    statusText.value = App.context.getString(R.string.recording_failed)
            }

            2001 -> statusText.value = App.context.getString(R.string.recording_failed)
            3003, 3015 -> rateLimitedEvent.value = Unit
            else -> {
                Stuff.logW("rec error: $statusCode - $statusMsg")
                statusText.value = App.context.getString(R.string.network_error)
            }
        }
    }

    override fun onVolumeChanged(vol: Double) {
        fadeValue.value = (0.1f + vol.toFloat()).coerceAtMost(1f)
    }

    companion object {
        private const val DURATION = 10000L
    }
}