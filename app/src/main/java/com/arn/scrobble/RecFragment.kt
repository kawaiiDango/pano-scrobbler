package com.arn.scrobble

import android.Manifest.permission.RECORD_AUDIO
import android.animation.ObjectAnimator
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.media.MediaRecorder
import android.os.*
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.arn.scrobble.pref.MultiPreferences
import com.google.android.material.snackbar.Snackbar
import de.umass.lastfm.scrobble.ScrobbleData
import kotlinx.android.synthetic.main.content_rec.*
import kotlinx.android.synthetic.main.content_rec.view.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File


class RecFragment:Fragment(){
    private var started = false
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var recorder: MediaRecorder? = null
    private val duration = 10000L
    private lateinit var path: String
    private var fadeAnimator: ObjectAnimator? = null
    private var progressAnimator: ObjectAnimator? = null
    private var asyncTask: SubmitAsync? = null
    private val pref: MultiPreferences by lazy { MultiPreferences(context!!) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.content_rec, container, false)
        if (!Main.isTV)
            setHasOptionsMenu(true)
        if (!view.rec_progress.isInTouchMode) {
            val outValue = TypedValue()
            context!!.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            view.rec_progress.setBackgroundResource(outValue.resourceId)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        path = activity!!.filesDir.absolutePath+"/sample.rec"
        handler.postDelayed({
            if (rec_progress!= null) {
                startOrCancel()
                rec_progress.setOnClickListener { startOrCancel() }
            }
        }, 300)

    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.menu_rec)
//        showSnackbar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if(started)
            startOrCancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.rec_menu, menu)
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            menu.findItem(R.id.menu_add_to_hs).isVisible = false
        if (pref.getString(Stuff.PREF_ACR_HOST, "") != "")
            menu.findItem(R.id.menu_add_acr_key).title = getString(R.string.remove_acr_key)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_add_to_hs && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val shortcutManager = activity!!.getSystemService(ShortcutManager::class.java)
            if (shortcutManager!!.isRequestPinShortcutSupported) {
                val pinShortcutInfo = ShortcutInfo.Builder(context, "rec").build()
                val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(pinShortcutInfo)
                val successCallback = PendingIntent.getBroadcast(context,0,
                        pinnedShortcutCallbackIntent,0)

                shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.intentSender)
            }
            return true
        } else if (item.itemId == R.id.menu_add_acr_key) {
            if (item.title == getString(R.string.remove_acr_key)) {
                item.title = getString(R.string.add_acr_key)
                removeKey()
            } else
                openAddKey()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openAddKey() {
        val b = Bundle()
        b.putString(LoginFragment.HEADING, getString(R.string.add_acr_key))
        b.putString(LoginFragment.INFO, getString(R.string.add_acr_key_info))
        b.putString(LoginFragment.TEXTF1, getString(R.string.acr_host))
        b.putString(LoginFragment.TEXTF2, getString(R.string.acr_key))
        b.putString(LoginFragment.TEXTFL, getString(R.string.acr_secret))

        val loginFragment = LoginFragment()
        loginFragment.arguments = b
        parentFragmentManager.beginTransaction()
                .replace(R.id.frame, loginFragment)
                .addToBackStack(null)
                .commit()
    }
    private fun removeKey() {
        pref.remove(Stuff.PREF_ACR_HOST)
        pref.remove(Stuff.PREF_ACR_KEY)
        pref.remove(Stuff.PREF_ACR_SECRET)
    }

    private fun showSnackbar(){
        val snackbar = Snackbar
                .make(view!!, R.string.add_acr_consider, 8*1000)
                .setAction(R.string.add) {
                    openAddKey()
                }
                .setActionTextColor(Color.YELLOW)
                .addCallback(object : Snackbar.Callback() {
            override fun onShown(sb: Snackbar?) {
                super.onShown(sb)
                if (sb != null && Main.isTV)
                    sb.view.postDelayed({
                        sb.view.findViewById<View>(com.google.android.material.R.id.snackbar_action)
                                .requestFocus()
                    }, 200)
            }
        })
        snackbar.view.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPrimary))
        snackbar.show()
    }

    private fun startOrCancel(){
        context ?: return
        if (ContextCompat.checkSelfPermission(context!!, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(RECORD_AUDIO), Stuff.REQUEST_CODE_MIC_PERM)
            return
        }

        if (!Main.isOnline){
            rec_status.setText(R.string.unavailable_offline)
            return
        }
        if(!started) {

            if(!startRecording() || rec_status == null)
                return
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

    private fun startRecording():Boolean {
        recorder = MediaRecorder()
        recorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder!!.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
        recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
        recorder!!.setOutputFile(path)
        try {
            recorder!!.prepare()
            recorder!!.start()
        } catch (e: Exception) {
            Stuff.log("prepare/start failed MIC")
            recorder!!.reset()
            recorder!!.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION) // needed on fire tv
            recorder!!.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
            recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
            recorder!!.setOutputFile(path)
            try {
                recorder!!.prepare()
                recorder!!.start()
            } catch (e: Exception) {
                Stuff.log("prepare/start failed VOICE_RECOGNITION")
                rec_status.setText(R.string.recording_failed)
                return false
            }
        }
        return true
    }

    private fun finishRecording() {
        try {
            recorder?.stop()
            recorder?.reset()
            recorder?.release()
        } catch (e:Exception) {}

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
            val scrobbleData = ScrobbleData()
            scrobbleData.artist = artist
            scrobbleData.album = album
            scrobbleData.track = title
            scrobbleData.timestamp = (System.currentTimeMillis()/1000).toInt() // in secs
            LFMRequester(context!!)
                    .scrobble(false, scrobbleData, Stuff.genHashCode(artist, album, title))
                    .asAsyncTask()
        } else {
            if (statusCode == 3003)
                showSnackbar()
            rec_status.text = statusMsg
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Stuff.REQUEST_CODE_MIC_PERM && grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
            startOrCancel()
        else
            Stuff.toast(context, getString(R.string.grant_rec_perm))
    }

    inner class SubmitAsync: AsyncTask<String, Int, String>() {
        override fun doInBackground(vararg path: String): String {
            val file = File(path[0])
            val i = IdentifyProtocolV1()
            val host = pref.getString(Stuff.PREF_ACR_HOST, Tokens.ACR_HOST)
            val key = pref.getString(Stuff.PREF_ACR_KEY, Tokens.ACR_KEY)
            val secret = pref.getString(Stuff.PREF_ACR_SECRET, Tokens.ACR_SECRET)
            return i.recognize(host, key, secret, file, "audio", Stuff.CONNECT_TIMEOUT)
        }

        override fun onPostExecute(result: String?) {
            onResult(result!!)
        }
    }
}