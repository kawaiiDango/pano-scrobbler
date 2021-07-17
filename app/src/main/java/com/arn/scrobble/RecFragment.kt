package com.arn.scrobble

import android.Manifest.permission.RECORD_AUDIO
import android.animation.ObjectAnimator
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.media.MediaRecorder
import android.os.*
import android.text.method.LinkMovementMethod
import android.transition.Fade
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.acrcloud.rec.*
import com.arn.scrobble.databinding.ContentRecBinding
import com.arn.scrobble.pref.MultiPreferences
import com.google.android.material.snackbar.Snackbar
import de.umass.lastfm.scrobble.ScrobbleData
import org.json.JSONException
import org.json.JSONObject


class RecFragment: Fragment(),
    IACRCloudListener,
    IACRCloudRadioMetadataListener
{
    private var started = false
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var client: ACRCloudClient? = null
    private lateinit var acrConfig: ACRCloudConfig
    private var fadeAnimator: ObjectAnimator? = null
    private var progressAnimator: ObjectAnimator? = null
    private val DURATION = 10000L
    private val pref: MultiPreferences by lazy { MultiPreferences(context!!) }
    private var _binding: ContentRecBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentRecBinding.inflate(inflater, container, false)
        if (!Main.isTV)
            setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || Main.isTV)
            binding.recShazam.visibility = View.GONE
        else
            binding.recShazam.movementMethod = LinkMovementMethod.getInstance()

        acrConfig = ACRCloudConfig().apply {
            acrcloudListener = this@RecFragment
            context = this@RecFragment.context!!
            host = pref.getString(Stuff.PREF_ACR_HOST, Tokens.ACR_HOST)
            accessKey = pref.getString(Stuff.PREF_ACR_KEY, Tokens.ACR_KEY)
            accessSecret = pref.getString(Stuff.PREF_ACR_SECRET, Tokens.ACR_SECRET)
            recorderConfig.isVolumeCallback = false
        }

        handler.postDelayed({
            if (_binding != null) {
                startOrCancel()
                binding.recProgress.setOnClickListener { startOrCancel() }
            }
        }, 300)
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.scrobble_from_mic)
//        showSnackbar()
    }

    override fun onDestroyView() {
        if(started)
            startOrCancel()
        _binding = null
        super.onDestroyView()
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
                        pinnedShortcutCallbackIntent, Stuff.updateCurrentOrImmutable)

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
        val loginFragment = LoginFragment()
        loginFragment.arguments = Bundle().apply {
            putString(LoginFragment.HEADING, getString(R.string.add_acr_key))
            putString(LoginFragment.INFO, getString(R.string.add_acr_key_info))
            putString(LoginFragment.TEXTF1, getString(R.string.acr_host))
            putString(LoginFragment.TEXTF2, getString(R.string.acr_key))
            putString(LoginFragment.TEXTFL, getString(R.string.acr_secret))
        }
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
        Snackbar
                .make(view!!, R.string.add_acr_consider, 8*1000)
                .setAction(R.string.add) {
                    openAddKey()
                }
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
                .show()
    }

    private fun startOrCancel(){
        context ?: return

        if (ContextCompat.checkSelfPermission(context!!, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(RECORD_AUDIO), Stuff.REQUEST_CODE_MIC_PERM)
            return
        }

        if (!Main.isOnline){
            binding.recStatus.setText(R.string.unavailable_offline)
            return
        }
        if(!started) {

            if(!startRecording() || _binding == null)
                return
            binding.recStatus.setText(R.string.listening)
            binding.recImg.setImageResource(R.drawable.vd_wave_simple)
            if (fadeAnimator?.isRunning == true)
                fadeAnimator?.cancel()
            fadeAnimator = ObjectAnimator.ofFloat(binding.recImg, "alpha", 0.5f, 1f)
                .apply {
                    duration = 500
                    interpolator = AccelerateInterpolator()
                    start()
                }

            if (progressAnimator?.isRunning == true)
                progressAnimator?.cancel()
            progressAnimator = ObjectAnimator.ofFloat(binding.recProgress, "progress", 0f, 100f)
                .apply {
                    duration = DURATION
                    interpolator = LinearInterpolator()
                    start()
                }

            handler.postDelayed({
                finishRecording()
            }, DURATION)
            started = true
        } else {
            try {
                client?.release()
                client = null
            } catch (e:Exception) {}
            binding.recStatus.text = ""

            if (fadeAnimator?.isRunning == true)
                fadeAnimator?.cancel()
            fadeAnimator = ObjectAnimator.ofFloat(binding.recImg, "alpha", 0.5f)
                .apply {
                    duration = 300
                    interpolator = AccelerateInterpolator()
                    start()
                }

            if (progressAnimator?.isRunning == true)
                progressAnimator?.cancel()

            progressAnimator = ObjectAnimator.ofFloat(binding.recProgress, "progress", 0f)
                .apply {
                    duration = 300
                    interpolator = AccelerateInterpolator()
                    start()
                }

            handler.removeCallbacksAndMessages(null)
            started = false
        }
    }

    private fun startRecording(): Boolean {
        client = ACRCloudClient()
        val initState = client!!.initWithConfig(acrConfig)
        var startState = false
        if (initState)
            startState = client!!.startRecognize()

        if (!initState || !startState) {
            Stuff.log("rec initState = $initState, startState = $startState")
            return false
        }
        return true
    }

    private fun finishRecording() {
        try {
            client?.release()
        } catch (e:Exception) {}
        client = null

        if(started)
            startOrCancel()
    }

    override fun onResult(acrCloudResult: ACRCloudResult) {
        val result = acrCloudResult.result ?: return
        var artist = ""
        var album = ""
        var title = ""
        var statusCode:Int
        var statusMsg = ""
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

        when(statusCode) {
            0 -> {
                binding.recImg.setImageResource(R.drawable.vd_check_simple)
                binding.recStatus.text = getString(R.string.state_scrobbled) +
                        getString(R.string.artist_title, artist, title)
                val scrobbleData = ScrobbleData()
                scrobbleData.artist = artist
                scrobbleData.album = album
                scrobbleData.track = title
                scrobbleData.timestamp = (System.currentTimeMillis() / 1000).toInt() // in secs
                LFMRequester(context!!)
                    .scrobble(false, scrobbleData, Stuff.genHashCode(artist, album, title, "rec"))
                    .asAsyncTask()
            }
            1001 -> binding.recStatus.text = getString(R.string.not_found)
            2000 -> {
                if (acrConfig.recorderConfig.source != MediaRecorder.AudioSource.VOICE_RECOGNITION) {
                    acrConfig.recorderConfig.source = MediaRecorder.AudioSource.VOICE_RECOGNITION
                    startOrCancel()
                } else
                    binding.recStatus.setText(R.string.recording_failed)
            }
            2001 -> binding.recStatus.setText(R.string.recording_failed)
            3003, 3015 -> showSnackbar()
            else -> {
                Stuff.log("rec error: $statusCode - $statusMsg")
                binding.recStatus.text = getString(R.string.network_error)
            }
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

    override fun onVolumeChanged(volume: Double) {
    }

    override fun onRadioMetadataResult(result: String?) {
    }
}