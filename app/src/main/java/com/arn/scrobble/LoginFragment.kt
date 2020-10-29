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
import com.arn.scrobble.pref.MultiPreferences
import kotlinx.android.synthetic.main.content_login.*
import kotlinx.android.synthetic.main.content_login.view.*
import org.json.JSONObject


/**
 * Created by arn on 06/09/2017.
 */
open class LoginFragment: DialogFragment() {
    protected lateinit var pref: MultiPreferences
    protected var checksLogin = true
    protected var standalone = false
    private var asyncTask: AsyncTask<Int, Unit, String?>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        returnTransition = Slide()
        showsDialog = false
        val view = inflater.inflate(R.layout.content_login, container, false)
        pref = MultiPreferences(context!!)
        val args = arguments
        args?.getString(INFO)?.let {
            view.login_info.autoLinkMask = Linkify.WEB_URLS
            view.login_info.text = it
            view.login_info.visibility = View.VISIBLE
        }
        args?.getString(TEXTF1)?.let {
            view.login_textfield1.hint = it
            if (!view.isInTouchMode)
                view.login_textfield1.requestFocus()
            view.login_textfield1.visibility = View.VISIBLE
        }
        args?.getString(TEXTF2)?.let {
            view.login_textfield2.hint = it
            view.login_textfield2.visibility = View.VISIBLE
        }
        args?.getString(TEXTFL)?.let {
            view.login_textfield_last.hint = it
            view.login_textfield_last.editText?.setOnEditorActionListener { textView, actionId, keyEvent ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)) {
                    view.login_submit.callOnClick()
                    true
                } else
                    false
            }
        }

        view.login_submit.setOnClickListener {
            it.visibility = View.GONE
            view.login_progress.visibility = View.VISIBLE
            validate()
        }

        return view
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
        login_progress.visibility = View.GONE
        login_status.setImageResource(R.drawable.vd_check)
        login_status.visibility = View.VISIBLE
        login_progress.postDelayed(
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
        login_progress.visibility = View.GONE
        login_status.setImageResource(R.drawable.vd_ban)
        login_status.visibility = View.VISIBLE
        login_progress.postDelayed(
                {
                    login_status ?: return@postDelayed
                    login_status.visibility = View.GONE
                    login_submit.visibility = View.VISIBLE
                    login_progress.visibility = View.GONE
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
        val t1 = login_textfield1.editText!!.text.toString()
        var t2 = login_textfield2.editText!!.text.toString()
        val tlast = login_textfield_last.editText!!.text.toString()

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