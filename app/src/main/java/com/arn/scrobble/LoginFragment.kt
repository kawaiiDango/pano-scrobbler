package com.arn.scrobble

import android.os.Bundle
import android.text.util.Linkify
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.arn.scrobble.databinding.ContentLoginBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.ListenBrainz
import com.arn.scrobble.ui.UiUtils.hideKeyboard
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.*
import org.json.JSONObject


/**
 * Created by arn on 06/09/2017.
 */
open class LoginFragment : DialogFragment() {
    protected val prefs by lazy { MainPrefs(context!!) }
    protected open val checksLogin = true
    protected val isStandalone by lazy { activity is MainDialogActivity }
    private var _binding: ContentLoginBinding? = null
    protected val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        showsDialog = false
        _binding = ContentLoginBinding.inflate(inflater, container, false)
        val args = arguments
        args?.getString(INFO)?.let {
            binding.loginInfo.autoLinkMask = Linkify.WEB_URLS
            binding.loginInfo.text = it
            binding.loginInfo.visibility = View.VISIBLE
        }
        args?.getString(TEXTF1)?.let {
            binding.loginTextfield1.hint = it
            if (!binding.root.isInTouchMode)
                binding.loginTextfield1.requestFocus()
            binding.loginTextfield1.visibility = View.VISIBLE
        }
        args?.getString(TEXTF2)?.let {
            binding.loginTextfield2.hint = it
            binding.loginTextfield2.visibility = View.VISIBLE
        }
        args?.getString(TEXTFL)?.let {
            binding.loginTextfieldLast.hint = it
            binding.loginTextfieldLast.editText?.setOnEditorActionListener { textView, actionId, keyEvent ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)
                ) {
                    binding.loginSubmit.callOnClick()
                    true
                } else
                    false
            }
        }
        args?.getString(TEXT_CHECKBOX)?.let {
            binding.loginCheckbox.text = it
            binding.loginCheckbox.visibility = View.VISIBLE
        }

        binding.loginSubmit.setOnClickListener {
            it.visibility = View.GONE
            binding.loginProgress.show()
            validate()
        }

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    protected open fun success() {
        if (context == null || isStateSaved || _binding == null)
            return
        binding.loginProgress.hide()
        binding.loginStatus.setImageResource(R.drawable.vd_check)
        binding.loginStatus.visibility = View.VISIBLE

        // java.lang.IllegalStateException: Can't access the Fragment View's LifecycleOwner when getView() is null i.e., before onCreateView() or after onDestroyView()
        // Seems to not work in DialogFragments
        lifecycleScope.launch {
            delay(500)
            if (context == null || isStateSaved)
                return@launch  // why is this needed?

            if (showsDialog)
                dismiss()
            else
                activity?.onBackPressed()
        }
    }

    protected fun error(errMsg: String) {
        if (context == null || isStateSaved)
            return
        if (errMsg.isNotEmpty())
            context!!.toast(errMsg)
        binding.loginProgress.hide()
        binding.loginStatus.setImageResource(R.drawable.vd_ban)
        binding.loginStatus.visibility = View.VISIBLE

        lifecycleScope.launch {
            delay(1500)
            if (context == null || isStateSaved)
                return@launch

            binding.loginStatus.visibility = View.GONE
            binding.loginSubmit.visibility = View.VISIBLE
            binding.loginProgress.hide()
        }
    }

    private fun validate() {
        GlobalScope.launch {
            val errMsg = try {
                withContext(Dispatchers.IO) {
                    if (validateAsync())
                        null
                    else
                        ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
                e.message
            }
            withContext(Dispatchers.Main) {
                if (errMsg == null)
                    success()
                else
                    error(errMsg)
            }
        }
        hideKeyboard()
    }

    protected open suspend fun validateAsync(): Boolean {
        val title = arguments?.getString(HEADING)
        val t1 = binding.loginTextfield1.editText!!.text.toString()
        val t2 = binding.loginTextfield2.editText!!.text.toString()
        val tlast = binding.loginTextfieldLast.editText!!.text.toString()
        val tlsNoVerify = binding.loginCheckbox.isChecked

        var success = false

        when (title) {
            getString(R.string.listenbrainz) -> {
                if (tlast.isNotBlank()) {
                    val username = (ListenBrainz().setToken(tlast) as? ListenBrainz)?.validateAndGetUsername()
                    if (username != null) {
                        prefs.listenbrainzUsername = username
                        prefs.listenbrainzToken = tlast
                        success = true
                    }
                }
            }
            getString(R.string.custom_listenbrainz) -> {
                if (t1.isNotBlank() && tlast.isNotBlank()) {
                    if (URLUtil.isValidUrl(t1)) {
                        val url = if (!t1.endsWith('/'))
                            "$t1/"
                        else
                            t1
                        val username = (ListenBrainz().setToken(tlast)
                            ?.setTlsNoVerify(tlsNoVerify)
                            ?.setApiRoot(url) as? ListenBrainz)
                            ?.validateAndGetUsername()

                        if (username != null) {
                            prefs.customListenbrainzRoot = url
                            prefs.customListenbrainzUsername = username
                            prefs.customListenbrainzToken = tlast
                            prefs.customListenbrainzTlsNoVerify = tlsNoVerify
                            success = true
                        }
                    } else {
                        throw IllegalArgumentException(getString(R.string.failed_encode_url))
                    }
                }
            }
            getString(R.string.gnufm) -> {
                var url = tlast
                if (url.isNotBlank()) {
                    if (URLUtil.isValidUrl(url)) {
                        if (!url.endsWith('/'))
                            url += '/'
                        prefs.gnufmRoot = url
                        prefs.gnufmTlsNoVerify = tlsNoVerify
                        val wf = WebViewFragment()
                        wf.arguments = Bundle().apply {
                            putString(
                                Stuff.ARG_URL,
                                url + "api/auth?api_key=" + Stuff.LIBREFM_KEY + "&cb=${Stuff.DEEPLINK_PROTOCOL_NAME}://auth/gnufm"
                            )
                            putBoolean(Stuff.ARG_TLS_NO_VERIFY, tlsNoVerify)
                        }
                        withContext(Dispatchers.Main) {
                            parentFragmentManager.popBackStackImmediate()
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.frame, wf)
                                .addToBackStack(null)
                                .commit()
                        }
                        return true
                    } else {
                        throw IllegalArgumentException(getString(R.string.failed_encode_url))
                    }
                }
            }
            getString(R.string.add_acr_key) -> {
                if (t1.isNotBlank() && t2.isNotBlank() && tlast.isNotBlank()) {
                    val i = IdentifyProtocolV1()
                    var url = t1
                    if (!url.startsWith("http"))
                        url = "https://$url"
                    if (!URLUtil.isValidUrl(url)) {
                        withContext(Dispatchers.Main) {
                            activity!!.toast(R.string.failed_encode_url)
                        }
                        throw IllegalArgumentException(getString(R.string.failed_encode_url))
                    }
                    val res = i.recognize(t1, t2, tlast, null, "audio", 10000)
                    val j = JSONObject(res)
                    val statusCode = j.getJSONObject("status").getInt("code")

                    if (statusCode == 2004) {
                        // {"status":{"msg":"Can't generate fingerprint","version":"1.0","code":2004}}
                        prefs.acrcloudHost = t1
                        prefs.acrcloudKey = t2
                        prefs.acrcloudSecret = tlast
                        success = true
                    }
                }
            }
            else -> withContext(Dispatchers.Main) {
                activity!!.toast("service not implemented")
            }
        }
        return success
    }

    override fun onStart() {
        super.onStart()

        if (checksLogin) {
            setTitle(arguments?.getString(HEADING))
        }

    }

    companion object {
        const val HEADING = "head"
        const val INFO = "info"
        const val TEXTF1 = "t1"
        const val TEXTF2 = "t2"
        const val TEXTFL = "tl"
        const val TEXT_CHECKBOX = "cb"
    }

}