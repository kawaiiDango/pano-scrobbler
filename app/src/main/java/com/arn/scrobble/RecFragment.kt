package com.arn.scrobble

import android.Manifest.permission.RECORD_AUDIO
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.acrcloud.rec.ACRCloudClient
import com.acrcloud.rec.ACRCloudConfig
import com.acrcloud.rec.ACRCloudResult
import com.acrcloud.rec.IACRCloudListener
import com.acrcloud.rec.IACRCloudRadioMetadataListener
import com.arn.scrobble.databinding.ContentRecBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.LoginFlows
import com.arn.scrobble.ui.UiUtils.focusOnTv
import com.arn.scrobble.ui.UiUtils.setTextAndAnimate
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject


class RecFragment : Fragment(),
    IACRCloudListener,
    IACRCloudRadioMetadataListener {
    private var started = false
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var client: ACRCloudClient? = null
    private lateinit var acrConfig: ACRCloudConfig
    private var fadeAnimator: ObjectAnimator? = null
    private var progressAnimator: ObjectAnimator? = null
    private val DURATION = 10000L
    private val prefs by lazy { MainPrefs(requireContext()) }
    private lateinit var micPermRequest: ActivityResultLauncher<String>
    private var _binding: ContentRecBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)

        micPermRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted)
                    startOrCancel()
                else
                    requireContext().toast(R.string.grant_rec_perm)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentRecBinding.inflate(inflater, container, false)
        binding.root.setupInsets()
        if (!Stuff.isTv)
            setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Stuff.DEMO_MODE)
            binding.recShazam.text = binding.recShazam.text.toString().replace("Shazam", "S app")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || Stuff.isTv)
            binding.recShazam.visibility = View.GONE
        else
            binding.recShazam.movementMethod = LinkMovementMethod.getInstance()

        acrConfig = ACRCloudConfig().apply {
            acrcloudListener = this@RecFragment
            context = this@RecFragment.requireContext()
            host = prefs.acrcloudHost ?: Tokens.ACR_HOST
            accessKey = prefs.acrcloudKey ?: Tokens.ACR_KEY
            accessSecret = prefs.acrcloudSecret ?: Tokens.ACR_SECRET
            recorderConfig.isVolumeCallback = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(300)
            startOrCancel()
            binding.recProgress.setOnClickListener { startOrCancel() }
        }
    }

    override fun onDestroyView() {
        if (started)
            startOrCancel()
        _binding = null
        super.onDestroyView()
    }
/*
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.rec_menu, menu)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            menu.findItem(R.id.menu_add_to_hs).isVisible = false
        if (prefs.acrcloudHost != null)
            menu.findItem(R.id.menu_add_acr_key).title = getString(R.string.remove_acr_key)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_add_to_hs && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = requireActivity().getSystemService(ShortcutManager::class.java)
            if (shortcutManager!!.isRequestPinShortcutSupported) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val pinShortcutInfo = ShortcutInfo.Builder(context, "rec").build()
                    val pinnedShortcutCallbackIntent =
                        shortcutManager.createShortcutResultIntent(pinShortcutInfo)
                    val successCallback = PendingIntent.getBroadcast(
                        context, 0,
                        pinnedShortcutCallbackIntent, Stuff.updateCurrentOrImmutable
                    )

                    shortcutManager.requestPinShortcut(
                        pinShortcutInfo,
                        successCallback.intentSender
                    )
                }
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
*/

    private fun removeKey() {
        prefs.acrcloudHost = null
        prefs.acrcloudKey = null
        prefs.acrcloudSecret = null
    }

    private fun showSnackbar() {
        Snackbar
            .make(requireView(), R.string.add_acr_consider, 8 * 1000)
            .setAction(R.string.add) {
                LoginFlows(findNavController()).acrCloud()
            }
            .focusOnTv()
            .show()
    }

    private fun startOrCancel() {
        context ?: return

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            micPermRequest.launch(RECORD_AUDIO)
            return
        }

        if (!Stuff.isOnline) {
            binding.recStatus.setTextAndAnimate(R.string.unavailable_offline)
            return
        }
        if (!started) {

            if (!startRecording() || _binding == null)
                return
            binding.recStatus.setTextAndAnimate(R.string.listening)
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
            progressAnimator =
                ObjectAnimator.ofInt(binding.recProgress, "progress", 0, binding.recProgress.max)
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
            } catch (e: Exception) {
            }
            binding.recStatus.setTextAndAnimate("")

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

            progressAnimator = ObjectAnimator.ofInt(binding.recProgress, "progress", 0)
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
        } catch (e: Exception) {
        }
        client = null

        if (started)
            startOrCancel()
    }

    override fun onResult(acrCloudResult: ACRCloudResult) {
        val result = acrCloudResult.result ?: return
        var artist = ""
        var album = ""
        var title = ""
        var statusCode: Int
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
        if (started)
            startOrCancel()

        when (statusCode) {
            0 -> {
                binding.recImg.setImageResource(R.drawable.vd_check_simple)
                binding.recStatus.setTextAndAnimate(
                    getString(R.string.state_scrobbled) + "\n" +
                            getString(R.string.artist_title, artist, title)
                )
                val trackInfo = PlayingTrackInfo(
                    requireContext().packageName,
                    playStartTime = System.currentTimeMillis()
                )

                trackInfo.putOriginals(artist, title, album, "")

                LFMRequester(CoroutineScope(Dispatchers.IO + Job()))
                    .scrobble(false, trackInfo)
            }

            1001 -> binding.recStatus.setTextAndAnimate(R.string.not_found)
            2000 -> {
                if (acrConfig.recorderConfig.source != MediaRecorder.AudioSource.VOICE_RECOGNITION) {
                    acrConfig.recorderConfig.source = MediaRecorder.AudioSource.VOICE_RECOGNITION
                    startOrCancel()
                } else
                    binding.recStatus.setTextAndAnimate(R.string.recording_failed)
            }

            2001 -> binding.recStatus.setTextAndAnimate(R.string.recording_failed)
            3003, 3015 -> showSnackbar()
            else -> {
                Stuff.log("rec error: $statusCode - $statusMsg")
                binding.recStatus.setTextAndAnimate(R.string.network_error)
            }
        }
    }

    override fun onVolumeChanged(volume: Double) {
    }

    override fun onRadioMetadataResult(result: String?) {
    }
}