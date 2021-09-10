package com.arn.scrobble

import android.os.Bundle
import android.text.util.Linkify
import android.transition.Fade
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.arn.scrobble.Stuff.hideKeyboard
import com.arn.scrobble.databinding.ContentLoginBinding
import com.arn.scrobble.pref.MultiPreferences
import kotlinx.coroutines.*
import org.json.JSONObject


/**
 * Created by arn on 06/09/2017.
 */
open class LoginFragment: DialogFragment() {
    protected lateinit var pref: MultiPreferences
    open val checksLogin = true
    protected var isStandalone = false
    private var _binding: ContentLoginBinding? = null
    protected val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        showsDialog = false
        _binding = ContentLoginBinding.inflate(inflater, container, false)
        pref = MultiPreferences(context!!)
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
                        (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)) {
                    binding.loginSubmit.callOnClick()
                    true
                } else
                    false
            }
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
        if (context == null || !isAdded)
            return
        binding.loginProgress.hide()
        binding.loginStatus.setImageResource(R.drawable.vd_check)
        binding.loginStatus.visibility = View.VISIBLE

        lifecycleScope.launch {
            delay(500)
            _binding ?: return@launch  // why is this needed?

            if (showsDialog)
                dismiss()
            else
                activity?.onBackPressed()
        }
    }

    protected fun error(errMsg: String) {
        if (errMsg.isNotEmpty())
            Stuff.toast(context, errMsg)
        if (context == null || !isAdded)
            return
        binding.loginProgress.hide()
        binding.loginStatus.setImageResource(R.drawable.vd_ban)
        binding.loginStatus.visibility = View.VISIBLE

        lifecycleScope.launch {
            delay(1500)
            _binding ?: return@launch

            binding.loginStatus.visibility = View.GONE
            binding.loginSubmit.visibility = View.VISIBLE
            binding.loginProgress.hide()
        }
    }

    private fun validate() {
        CoroutineScope(Dispatchers.Main + Job()).launch {
            val errMsg = withContext(Dispatchers.IO) {
                validateAsync()
            }
            if (errMsg == null)
                success()
            else
                error(errMsg)
        }
        hideKeyboard()
    }

    open suspend fun validateAsync(): String? {
        val title = arguments?.getString(HEADING)
        val t1 = binding.loginTextfield1.editText!!.text.toString()
        val t2 = binding.loginTextfield2.editText!!.text.toString()
        val tlast = binding.loginTextfieldLast.editText!!.text.toString()

        var success = false

        when (title) {
            getString(R.string.listenbrainz) -> {
                if (tlast.isNotBlank()) {
                    val username = ListenBrainz(tlast).getUsername()
                    if (username != null) {
                        pref.putString(Stuff.PREF_LISTENBRAINZ_USERNAME, username)
                        pref.putString(Stuff.PREF_LISTENBRAINZ_TOKEN, tlast)
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
                        val username = ListenBrainz(tlast)
                                .setApiRoot(url)
                                .getUsername()

                        if (username != null) {
                            pref.putString(Stuff.PREF_LB_CUSTOM_ROOT, url)
                            pref.putString(Stuff.PREF_LB_CUSTOM_USERNAME, username)
                            pref.putString(Stuff.PREF_LB_CUSTOM_TOKEN, tlast)
                            success = true
                        }
                    } else
                        withContext(Dispatchers.Main) {
                            Stuff.toast(activity!!, getString(R.string.failed_encode_url))
                        }
                }
            }
            getString(R.string.add_acr_key) -> {
                if (t1.isNotBlank() && t2.isNotBlank() && tlast.isNotBlank()){
                    val i = IdentifyProtocolV1()
                    try {
                        var url = t1
                        if (!url.startsWith("http"))
                            url = "https://$url"
                        if (!URLUtil.isValidUrl(url)) {
                            withContext(Dispatchers.Main) {
                                Stuff.toast(activity!!, getString(R.string.failed_encode_url))
                            }
                            throw Exception(getString(R.string.failed_encode_url))
                        }
                        val res = i.recognize(t1, t2, tlast, null, "audio", 10000)
                        val j = JSONObject(res)
                        val statusCode = j.getJSONObject("status").getInt("code")

                        if (statusCode == 2004) {
                            // {"status":{"msg":"Can't generate fingerprint","version":"1.0","code":2004}}
                            pref.putString(Stuff.PREF_ACR_HOST, t1)
                            pref.putString(Stuff.PREF_ACR_KEY, t2)
                            pref.putString(Stuff.PREF_ACR_SECRET, tlast)
                            success = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            else -> withContext(Dispatchers.Main) {
                Stuff.toast(activity!!, "service not implemented")
            }
        }
        return if (success)
            null
        else
            ""
    }

    override fun onStart() {
        super.onStart()

        if (checksLogin) {
            Stuff.setTitle(activity, arguments?.getString(HEADING))
        }

    }

    companion object {
        const val HEADING = "head"
        const val INFO = "info"
        const val TEXTF1 = "t1"
        const val TEXTF2 = "t2"
        const val TEXTFL = "tl"
    }

}