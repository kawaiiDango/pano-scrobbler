package com.arn.scrobble.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.LastfmUnscrobbler
import com.arn.scrobble.databinding.ContentWebviewBinding
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.ui.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.startFadeLoop
import com.arn.scrobble.ui.UiUtils.toast
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.getSingle
import com.google.android.material.transition.MaterialSharedAxis
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl


/**
 * Created by arn on 06/09/2017.
 */
class WebViewFragment : Fragment() {
    private var _binding: ContentWebviewBinding? = null
    private val binding
        get() = _binding!!
    private val url
        get() = arguments?.getString(Stuff.ARG_URL) ?: "https://example.com"
    private val saveCookies
        get() = arguments?.getBoolean(Stuff.ARG_SAVE_COOKIES) ?: false
    private val viewModel by viewModels<WebViewVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAxisTransitions(MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.webview.webViewClient = MyWebViewClient()
        // In Android O and afterwards, this function does not have any effect, the form data will be saved to platform's autofill service if applicable.
        binding.webview.settings.saveFormData = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && resources.getBoolean(R.bool.is_dark)) {
            binding.webview.settings.forceDark = WebSettings.FORCE_DARK_ON
        }
//        webview.settings.javaScriptEnabled =true

        binding.webview.loadUrl(url)
        if (!binding.webview.isInTouchMode)
            binding.webview.requestFocus()

        collectLatestLifecycleFlow(viewModel.callbackProcessed) { done ->
            if (done) {
                binding.webview.clearAnimation()

                try {
                    findNavController().popBackStack()
                } catch (e: IllegalStateException) {
                    view.context.toast(R.string.press_back)
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.webview.clearCache(true)
        CookieManager.getInstance().removeAllCookies(null)
        _binding = null
        super.onDestroyView()
    }

    private fun handleCallbackUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        val path = uri.path ?: return false
        if (uri.scheme != Stuff.DEEPLINK_PROTOCOL_NAME)
            return false
        val token = uri.getQueryParameter("token") ?: return false
        when (path) {
            "/lastfm" -> {
                if (saveCookies) {
                    val httpUrl = "https://www.last.fm/".toHttpUrl()
                    val cookieString =
                        CookieManager.getInstance().getCookie(httpUrl.toString()) ?: ""
                    // intercept lastfm cookies for edit and delete to work
                    val cookies = mutableListOf<Cookie>()

                    cookieString.split("; ")
                        .forEach {
                            val nvpair = it.split("=", limit = 2)
                            if (nvpair[0] == LastfmUnscrobbler.COOKIE_CSRFTOKEN ||
                                nvpair[0] == LastfmUnscrobbler.COOKIE_SESSIONID
                            ) {
                                val okCookie = Cookie.Builder()
                                    .domain("last.fm")
                                    .expiresAt(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30 * 11)
                                    //somewhat less than 1y, similar to what lastfm does
                                    .name(nvpair[0])
                                    .value(nvpair[1])
                                    .path("/")
                                    .secure()
                                    //bad but this makes it https on SharedPrefsCookiePersistor
                                    .build()

                                cookies.add(okCookie)
                            }
                        }
                    val unscrobbler = LastfmUnscrobbler()
                    unscrobbler.putCookies(httpUrl, cookies)
                }

                viewModel.doAuth(
                    requireArguments().getSingle<UserAccountTemp>()!!.copy(authKey = token)
                )
            }

            "/librefm",
            "/gnufm" -> {
                viewModel.doAuth(
                    requireArguments().getSingle<UserAccountTemp>()!!.copy(authKey = token)
                )
            }

            else -> {
                return false
            }
        }

        return true
    }

    private fun disableWebviewClicks() {
        binding.touchBlockerOverlay.visibility = View.VISIBLE
    }

    inner class MyWebViewClient : WebViewClient() {
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {

            if (
                url.startsWith("https://www.last.fm/join") ||
                url.startsWith("https://secure.last.fm/settings/lostpassword")
            ) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view.context.startActivity(intent)
                try {
                    findNavController().popBackStack()
                } catch (e: IllegalStateException) {
                    view.context.toast(R.string.press_back)
                }
                return true
            }

            val callbackHandled = handleCallbackUrl(url)
            if (callbackHandled) {
                disableWebviewClicks()
                binding.webview.startFadeLoop()
            }

            return callbackHandled
        }

        @Deprecated("Deprecated in Java")
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            //deprecated but required for lollipop
            showErrorMessage(description ?: "null")
        }

        override fun onPageFinished(view: WebView, url: String?) {
            (activity as? MainActivity)?.binding?.toolbar?.title = view.title
        }

        private fun showErrorMessage(text: String) {
            _binding ?: return

            val htmlData =
                "<html><body><div align=\"center\" >" + getString(R.string.webview_error) + "<br>" +
                        text + "</div></body></html>"
            binding.webview.loadUrl("about:blank")
            binding.webview.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)

        }
    }
}