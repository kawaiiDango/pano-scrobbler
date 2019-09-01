package com.arn.scrobble

import android.Manifest.permission.RECORD_AUDIO
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.content_rec.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException




class RecFragment:Fragment(){
    private var started = false
    private val code = 200
    private val handler = Handler()
    private var recorder:MediaRecorder? = null
    private val duration = 10000L
    private lateinit var path:String
    private var fadeAnimator: ObjectAnimator? = null
    private var progressAnimator: ObjectAnimator? = null
    private var asyncTask: AsyncTask<String, Int, String>? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.content_rec, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        path = activity!!.filesDir.absolutePath+"/sample.rec"
        handler.postDelayed({
            startOrCancel()
            rec_progress.setOnClickListener { startOrCancel() }
        }, 300)

    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.menu_rec)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if(started)
            startOrCancel()
    }

    private fun startOrCancel(){
        if (ContextCompat.checkSelfPermission(context!!, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Stuff.toast(context, getString(R.string.grant_rec_perm))
            requestPermissions(arrayOf(RECORD_AUDIO), code)
            return
        }

        if (!Main.isOnline){
            rec_status.setText(R.string.unavailable_offline)
            return
        }
        if(!started) {

            startRecording()
            rec_status.setText(R.string.listening)
            rec_img.setImageResource(R.drawable.vd_wave_simple)
            if (fadeAnimator?.isRunning == true)
                fadeAnimator?.cancel()
            fadeAnimator = ObjectAnimator.ofFloat(rec_img, "alpha", 0.5f, 1f)
            fadeAnimator!!.duration = 500
            fadeAnimator!!.interpolator = AccelerateInterpolator()
            fadeAnimator!!.start()

            if (progressAnimator?.isRunning == true)
                progressAnimator?.cancel()
            progressAnimator = ObjectAnimator.ofFloat(rec_progress, "progress", 0f, 100f)
            progressAnimator!!.duration = duration + 2000
            progressAnimator!!.interpolator = LinearInterpolator()
            progressAnimator!!.start()

            handler.postDelayed({
                finishRecording()
            }, 10000)
            started = true
        } else {
            try {
                recorder?.stop()
                recorder?.reset()
                recorder?.release()
                recorder = null
            } catch (e:Exception) {}
            asyncTask?.cancel(true)
            asyncTask = null
            rec_status.text = ""

            if (fadeAnimator?.isRunning == true)
                fadeAnimator?.cancel()
            fadeAnimator = ObjectAnimator.ofFloat(rec_img, "alpha", 0.5f)
            fadeAnimator!!.duration = 300
            fadeAnimator!!.interpolator = AccelerateInterpolator()
            fadeAnimator!!.start()

            if (progressAnimator?.isRunning == true)
                progressAnimator?.cancel()

            progressAnimator = ObjectAnimator.ofFloat(rec_progress, "progress", 0f)
            progressAnimator!!.duration = 300
            progressAnimator!!.interpolator = AccelerateInterpolator()
            progressAnimator!!.start()

            handler.removeCallbacksAndMessages(null)
            started = false
        }
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
            setOutputFile(path)
            setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)

            try {
                prepare()
            } catch (e: IOException) {
                Stuff.log("prepare() failed")
            }

            start()
        }
    }

    private fun finishRecording() {
        recorder?.stop()
        recorder?.reset()
        recorder?.release()
        recorder = null
        rec_status.setText(R.string.uploading)

        asyncTask = SubmitAsync()
        asyncTask!!.execute(path)
    }

    private fun onResult(result: String) {
        var artist = ""
        var album = ""
        var title = ""
        var statusCode:Int
        var statusMsg = ""
        asyncTask = null
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
                    throw java.lang.Exception("not found")
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
            Stuff.log(result)
            e.printStackTrace()
        }
        if(started)
            startOrCancel()

        if(statusCode == 0) {
            rec_img.setImageResource(R.drawable.vd_check_simple)
            rec_status.text = getString(R.string.state_scrobbled) + "\n$artist — $title"
            LFMRequester(Stuff.SCROBBLE, artist, album, title, "", System.currentTimeMillis().toString(), "0")
                    .asSerialAsyncTask(context!!)
        } else
            rec_status.text = statusMsg

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == code && grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
            startOrCancel()
    }

    inner class SubmitAsync: AsyncTask<String, Int, String>() {
        override fun doInBackground(vararg path: String?): String {
            val file = File(path[0])
            val a = IdentifyProtocolV1()
            return a.recognize(Tokens.ACR_HOST, Tokens.ACR_KEY, Tokens.ACR_SECRET, file, "audio", 10000)
        }

        override fun onPostExecute(result: String?) {
            onResult(result!!)
        }
    }
}