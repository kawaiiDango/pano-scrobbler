package com.arn.scrobble

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Bundle
import android.text.util.Linkify
import android.transition.Slide
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import androidx.fragment.app.DialogFragment
import com.arn.scrobble.databinding.ContentLoginBinding
import com.arn.scrobble.pref.MultiPreferences
import org.json.JSONObject


/**
 * Created by arn on 06/09/2017.
 */
open class LoginFragment: DialogFragment() {
    protected lateinit var pref: MultiPreferences
    protected var checksLogin = true
    protected var standalone = false
    private var asyncTask: AsyncTask<Int, Unit, String?>? = null
    private var _binding: ContentLoginBinding? = null
    protected val binding
        get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        returnTransition = Slide()
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
            binding.loginProgress.visibility = View.VISIBLE
            validate()
        }

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private val sessChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                NLService.iSESS_CHANGED -> success()
            }
        }
    }

    protected open fun success() {
        if (context == null || !isAdded)
            return
        binding.loginProgress.visibility = View.GONE
        binding.loginStatus.setImageResource(R.drawable.vd_check)
        binding.loginStatus.visibility = View.VISIBLE
        binding.loginProgress.postDelayed(
                {
                    if (showsDialog)
                        try {
                            dismiss()
                        } catch (e:IllegalStateException) {}
                    else {
                        if (standalone)
                            context?.sendBroadcast(Intent(NLService.iDISMISS_MAIN_NOTI))
                        activity?.onBackPressed()
                    }
                },
                500
        )
    }

    protected fun error(errMsg: String) {
        if (errMsg.isNotEmpty())
            Stuff.toast(context, errMsg)
        if (context == null || !isAdded)
            return
        binding.loginProgress.visibility = View.GONE
        binding.loginStatus.setImageResource(R.drawable.vd_ban)
        binding.loginStatus.visibility = View.VISIBLE
        binding.loginProgress.postDelayed(
                {
                    _binding ?: return@postDelayed
                    binding.loginStatus.visibility = View.GONE
                    binding.loginSubmit.visibility = View.VISIBLE
                    binding.loginProgress.visibility = View.GONE
                },
                1500
        )
    }

    private fun validate() {
        asyncTask = @SuppressLint("StaticFieldLeak")
        object : AsyncTask<Int, Unit, String?>(){
            override fun doInBackground(vararg p0: Int?) = validateAsync()

            override fun onPostExecute(errMsg: String?) {
                if (errMsg == null)
                    success()
                else
                    error(errMsg)
            }
        }
        asyncTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        hideKeyboard()
    }

    open fun validateAsync(): String? {
        val title = arguments?.getString(HEADING)
        val t1 = binding.loginTextfield1.editText!!.text.toString()
        var t2 = binding.loginTextfield2.editText!!.text.toString()
        val tlast = binding.loginTextfieldLast.editText!!.text.toString()

        var success = false

        when (title) {
            getString(R.string.listenbrainz) -> {
                if (t1.isNotBlank() && tlast.isNotBlank())
                    success = ListenBrainz(tlast).checkAuth(activity!!, pref, t1)
            }
            getString(R.string.custom_listenbrainz) -> {
                if (t1.isNotBlank() && t2.isNotBlank() && tlast.isNotBlank()) {
                    if (URLUtil.isValidUrl(t2)) {
                        if (!t2.endsWith('/'))
                            t2 += '/'
                        success = ListenBrainz(tlast)
                                .setApiRoot(t2)
                                .checkAuth(activity!!, pref, t1)
                    } else
                        activity?.runOnUiThread {
                            Stuff.toast(activity!!, getString(R.string.failed_encode_url))
                        }
                }
            }
            getString(R.string.add_acr_key) -> {
                if (t1.isNotBlank() && t2.isNotBlank() && tlast.isNotBlank()){
                    val i = IdentifyProtocolV1()
                    try {
                        if (!URLUtil.isValidUrl(t2)) {
                            activity?.runOnUiThread {
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
            else -> activity?.runOnUiThread {
                Stuff.toast(activity!!, "service not implemented")
            }
        }
        return if (success)
            null
        else
            ""
    }

    protected fun hideKeyboard(){
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onStart() {
        super.onStart()

        if (checksLogin) {
            Stuff.setTitle(activity, arguments?.getString(HEADING))

            val iF = IntentFilter()
            iF.addAction(NLService.iSESS_CHANGED)
            activity!!.registerReceiver(sessChangeReceiver, iF)
        }

    }

    override fun onStop() {
        if (checksLogin)
            activity!!.unregisterReceiver(sessChangeReceiver)
        asyncTask?.cancel(false)
        super.onStop()
    }

    companion object {
        const val HEADING = "head"
        const val INFO = "info"
        const val TEXTF1 = "t1"
        const val TEXTF2 = "t2"
        const val TEXTFL = "tl"
    }

}