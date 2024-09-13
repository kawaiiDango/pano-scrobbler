package com.arn.scrobble.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.LastfmUnscrobbler
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.databinding.ContentWebviewBinding
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.getSingle
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.startFadeLoop
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.transition.MaterialSharedAxis
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.launch


/**
 * Created by arn on 06/09/2017.
 */
class WebViewFragment : Fragment() {
    private var _binding: ContentWebviewBinding? = null
    private val binding
        get() = _binding!!
    private val url
        get() = arguments?.getString(Stuff.ARG_URL) ?: "about:blank"
    private val saveCookies
        get() = arguments?.getBoolean(Stuff.ARG_SAVE_COOKIES) ?: false
    private val viewModel by viewModels<WebViewVM>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.X)

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

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
        if (Stuff.isTv)
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

    private fun stringToCookies(cookieString: String, requestUrl: Url): List<Cookie> =
        cookieString.split(";").mapNotNull { cookie ->
            val (name, value) = cookie.trim().split("=", limit = 2)
            if (name.isNotBlank() && value.isNotBlank()) {
                Cookie(
                    name,
                    value,
                    CookieEncoding.RAW,
                    domain = requestUrl.host,
                    path = "/",
                    expires = GMTDate(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30 * 11),
                    //somewhat less than 1y, similar to what lastfm does
                    secure = true,
                )
            } else {
                null
            }
        }

    private fun handleCallbackUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        val path = uri.path ?: return false
        if (uri.scheme != Stuff.DEEPLINK_PROTOCOL_NAME)
            return false
        when (path) {
            "/lastfm" -> {
                val token = uri.getQueryParameter("token") ?: return false

                if (saveCookies) {
                    val httpUrlString = "https://www.last.fm/"
                    val httpUrl = Url(httpUrlString)
                    // intercept lastfm cookies for edit and delete to work
                    val cookieString =
                        CookieManager.getInstance().getCookie(httpUrlString) ?: ""
                    val cookies = stringToCookies(cookieString, httpUrl)

                    viewModel.viewModelScope.launch {
                        cookies.forEach {
                            LastfmUnscrobbler.cookieStorage.addCookie(httpUrl, it)
                        }
                    }
                }

                viewModel.doLastFmAuth(
                    requireArguments().getSingle<UserAccountTemp>()!!.copy(authKey = token)
                )
            }

            "/librefm" -> {
                val token = uri.getQueryParameter("token") ?: return false

                viewModel.doLastFmAuth(
                    requireArguments().getSingle<UserAccountTemp>()!!.copy(authKey = token)
                )
            }

            "/pleroma" -> {
                val token = uri.getQueryParameter("code") ?: return false

                viewModel.doPleromaAuth(
                    requireArguments().getSingle<UserAccountTemp>()!!.copy(authKey = token),
                    requireArguments().getSingle<PleromaOauthClientCreds>()!!
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
        override fun shouldOverrideUrlLoading(
            view: WebView,
            url: String?
        ): Boolean {
            val url = url ?: return false

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

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest,
            error: WebResourceError?
        ) {
            if (request.isForMainFrame && !BuildConfig.DEBUG)
                showErrorMessage(error?.description.toString())

            super.onReceivedError(view, request, error)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            (activity as? MainActivity)?.binding?.toolbar?.title = view.title
        }

        private fun showErrorMessage(text: String) {
            _binding ?: return

            val htmlData =
                "<html><body><div align=\"center\">$text</div></body></html>"
            binding.webview.loadUrl("about:blank")
            binding.webview.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)

        }
    }
}