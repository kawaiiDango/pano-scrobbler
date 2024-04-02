package com.arn.scrobble.onboarding

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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.arn.scrobble.main.App
import com.arn.scrobble.R
import com.arn.scrobble.Tokens
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.lastfm.GnuFm
import com.arn.scrobble.api.lastfm.LastfmUnscrobbler
import com.arn.scrobble.api.lastfm.ScrobbleIgnoredException
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.api.maloja.Maloja
import com.arn.scrobble.databinding.ContentLoginBinding
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.recents.PopupMenuUtils
import com.arn.scrobble.utils.UiUtils.hideKeyboard
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.arn.scrobble.utils.UiUtils.toast
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putSingle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder


/**
 * Created by arn on 06/09/2017.
 */
open class LoginFragment : DialogFragment() {
    protected val prefs = App.prefs
    protected open val checksLogin = true
    private var _binding: ContentLoginBinding? = null
    protected val binding
        get() = _binding!!
    private val args by navArgs<LoginFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setupAxisTransitions(MaterialSharedAxis.X)

        showsDialog = false
        _binding = ContentLoginBinding.inflate(inflater, container, false)
        args.infoText?.let {
            binding.loginInfo.autoLinkMask = Linkify.WEB_URLS
            binding.loginInfo.text = it
            binding.loginInfo.visibility = View.VISIBLE
        }
        args.textField1?.let {
            binding.loginTextfield1.hint = it
            if (!binding.root.isInTouchMode)
                binding.loginTextfield1.requestFocus()
            binding.loginTextfield1.visibility = View.VISIBLE
        }
        args.textField2?.let {
            binding.loginTextfield2.hint = it
            binding.loginTextfield2.visibility = View.VISIBLE
        }
        args.textFieldLast.let {
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

        binding.loginSubmit.setOnClickListener {
            it.visibility = View.GONE
            binding.loginProgress.show()
            validate()
        }
        binding.root.setupInsets()

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
            else if (args.loginTitle != getString(R.string.pleroma))
                findNavController().popBackStack()
        }
    }

    protected fun error(exception: Throwable) {
        if (context == null || isStateSaved)
            return

        binding.loginProgress.hide()
        binding.loginStatus.setImageResource(R.drawable.vd_ban)
        binding.loginStatus.visibility = View.VISIBLE

        lifecycleScope.launch {

            when (exception) {
                is LastfmUnscrobbler.CookiesInvalidatedException -> {
                    PopupMenuUtils.showReauthenticatePrompt(findNavController())
                }

                is ScrobbleIgnoredException -> {
                    if (System.currentTimeMillis() - exception.scrobbleTime < Stuff.LASTFM_MAX_PAST_SCROBBLE)
                        requireContext().toast(getString(R.string.lastfm) + ": " + getString(R.string.scrobble_ignored))
                    else {
                        withContext(Dispatchers.Main) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setMessage(R.string.scrobble_ignored_save_edit)
                                .setPositiveButton(R.string.yes) { _, _ ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        exception.altAction()
                                    }
                                    dismiss()
                                }
                                .setNegativeButton(R.string.no, null)
                                .show()
                        }
                    }
                }

                else -> {
                    if (!exception.message.isNullOrEmpty())
                        requireContext().toast(exception.message!!)
                    else
                        requireContext().toast(R.string.network_error)
                }
            }

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
            val result = withContext(Dispatchers.IO) {
                validateAsync()
            }

            lifecycleScope.launch {
                result.onSuccess {
                    success()
                }.onFailure {
                    error(it)
                }
            }
        }
        hideKeyboard()
    }

    protected open suspend fun validateAsync(): Result<Unit> {
        val t1 = binding.loginTextfield1.editText!!.text.toString()
        val t2 = binding.loginTextfield2.editText!!.text.toString()
        val tlast = binding.loginTextfieldLast.editText!!.text.toString()

        return when (args.loginTitle) {
            getString(R.string.listenbrainz) -> {
                if (tlast.isNotBlank()) {
                    val userAccount = UserAccountTemp(
                        AccountType.LISTENBRAINZ,
                        tlast,
                        Stuff.LISTENBRAINZ_API_ROOT,
                    )
                    ListenBrainz.authAndGetSession(userAccount)
                        .map { Unit }
                        .onFailure { it.printStackTrace() }
                } else {
                    Result.failure(IllegalArgumentException(getString(R.string.required_fields_empty)))
                }
            }

            getString(R.string.custom_listenbrainz) -> {
                if (t1.isNotBlank() && tlast.isNotBlank()) {
                    if (URLUtil.isValidUrl(t1)) {
                        val url = if (!t1.endsWith('/'))
                            "$t1/"
                        else
                            t1

                        val userAccount = UserAccountTemp(
                            AccountType.CUSTOM_LISTENBRAINZ,
                            tlast,
                            url,
                        )
                        ListenBrainz.authAndGetSession(userAccount)
                            .map { Unit }
                    } else {
                        Result.failure(IllegalArgumentException(getString(R.string.failed_encode_url)))
                    }
                } else {
                    Result.failure(IllegalArgumentException(getString(R.string.required_fields_empty)))
                }
            }

            getString(R.string.gnufm) -> {
                var url = t1
                val username = t2
                val password = tlast

                if (url.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                    if (URLUtil.isValidUrl(url)) {
                        if (!url.endsWith('/'))
                            url += '/'
                        if (!url.endsWith("2.0/"))
                            url += "2.0/"

                        GnuFm.authAndGetSession(url, username, password)
                            .map { }
                    } else {
                        Result.failure(IllegalArgumentException(getString(R.string.failed_encode_url)))
                    }
                } else {
                    Result.failure(IllegalArgumentException(getString(R.string.required_fields_empty)))
                }
            }

            getString(R.string.pleroma) -> {
                var url = tlast

                if (url.isNotBlank()) {
                    if (URLUtil.isValidUrl(url)) {
                        if (!url.endsWith('/'))
                            url += '/'

                        val arguments = Bundle().apply {
                            putString(
                                Stuff.ARG_URL,
                                "${url}oauth/authorize?client_id=${Tokens.PLEROMA_CLIENT_ID}&redirect_uri=${
                                    URLEncoder.encode(Stuff.DEEPLINK_PROTOCOL_NAME + "://auth/pleroma")
                                }&response_type=code&scope=${URLEncoder.encode("read write")}"
                            )
                            putSingle(UserAccountTemp(AccountType.PLEROMA, "", url))
                        }
                        withContext(Dispatchers.Main) {
                            findNavController().popBackStack()
                            findNavController().navigate(R.id.webViewFragment, arguments)
                        }
                        Result.success(Unit)
                    } else {
                        Result.failure(IllegalArgumentException(getString(R.string.failed_encode_url)))
                    }
                } else {
                    Result.failure(IllegalArgumentException(getString(R.string.required_fields_empty)))
                }
            }

            getString(R.string.maloja) -> {
                var url = t1
                val token = tlast

                if (url.isNotBlank() && token.isNotBlank()) {
                    if (URLUtil.isValidUrl(url)) {
                        if (!url.endsWith('/'))
                            url += '/'

                        val userAccount = UserAccountTemp(
                            AccountType.MALOJA,
                            tlast,
                            url,
                        )
                        Maloja.authAndGetSession(userAccount)
                            .map { }
                    } else {
                        Result.failure(IllegalArgumentException(getString(R.string.failed_encode_url)))
                    }
                } else {
                    Result.failure(IllegalArgumentException(getString(R.string.required_fields_empty)))
                }
            }

            getString(R.string.add_acr_key) -> {
                if (t1.isNotBlank() && t2.isNotBlank() && tlast.isNotBlank()) {

//                    val i = IdentifyProtocolV1()
//                    var url = t1
//                    if (!url.startsWith("http"))
//                        url = "https://$url"
//                    if (!URLUtil.isValidUrl(url)) {
//                        withContext(Dispatchers.Main) {
//                            requireActivity().toast(R.string.failed_encode_url)
//                        }
//                        throw IllegalArgumentException(getString(R.string.failed_encode_url))
//                    }
//                    val res = i.recognize(t1, t2, tlast, null, "audio", 10000)
//                    val j = JSONObject(res)
//                    val statusCode = j.getJSONObject("status").getInt("code")
//
//                    if (statusCode == 2004) {
                    // {"status":{"msg":"Can't generate fingerprint","version":"1.0","code":2004}}
                    prefs.acrcloudHost = t1
                    prefs.acrcloudKey = t2
                    prefs.acrcloudSecret = tlast
                    Result.success(Unit)
//                    }
                } else {
                    Result.failure(IllegalArgumentException(getString(R.string.required_fields_empty)))
                }
            }

            else -> withContext(Dispatchers.Main) {
                throw NotImplementedError("service not implemented")
            }
        }
    }
}