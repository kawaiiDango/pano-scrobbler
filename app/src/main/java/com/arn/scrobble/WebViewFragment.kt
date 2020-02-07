package com.arn.scrobble

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.arn.scrobble.pref.MultiPreferences
import kotlinx.android.synthetic.main.content_webview.*
import okhttp3.Cookie
import okhttp3.HttpUrl

/**
 * Created by arn on 06/09/2017.
 */
class WebViewFragment: Fragment() {
    private lateinit var pref: MultiPreferences
    private var saveCookies = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.content_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        webview.webViewClient = WebViewClient()
        webview.settings.saveFormData = false
        // In Android O and afterwards, this function does not have any effect, the form data will be saved to platform's autofill service if applicable.
//        webview.settings.javaScriptEnabled =true

        val url = arguments?.getString(Stuff.ARG_URL) ?: "https://example.com"
        saveCookies = arguments?.getBoolean(Stuff.ARG_SAVE_COOKIES) ?: false
        webview.loadUrl(url)
        webview.webViewClient = MyWebViewClient()
        if (!webview.isInTouchMode)
            webview.requestFocus()
        super.onViewCreated(view, savedInstanceState)
    }
/*
    fun onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    */
    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.loading)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webview.clearCache(true)
        CookieManager.getInstance().removeAllCookies(null)
    }

    inner class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            val uri = Uri.parse(url)
            val host = uri.host
            if (host != null) {
                if (host.endsWith("last.fm") || host.endsWith("libre.fm") || host == "telegra.ph")
                    return false
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view.context.startActivity(intent)
                if (url.startsWith("pscrobble://auth/lastfm?")){
                    if (saveCookies) {
                        val httpUrl = HttpUrl.parse("https://www.last.fm/")!!
                        val cookieString: String = CookieManager.getInstance().getCookie(httpUrl.toString())
                        // intercept lastfm cookies for edit and delete to work
                        val cookies = mutableListOf<Cookie>()

                        cookieString.split("; ")
                                .forEach {
                                    val nvpair = it.split("=", limit=2)
                                    if (nvpair[0] == LastfmUnscrobbler.COOKIE_CSRFTOKEN ||
                                            nvpair[0] == LastfmUnscrobbler.COOKIE_SESSIONID) {
                                        val okCookie = Cookie.Builder()
                                                .domain("last.fm")
                                                .expiresAt(System.currentTimeMillis() + 1000L*60*60*24*30*11)
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
                        val unscrobbler = LastfmUnscrobbler(context)
                        unscrobbler.putCookies(httpUrl, cookies)
                    }
                    parentFragmentManager.popBackStack()
                }
            }
            return true
        }

        override fun onPageFinished(view: WebView, url: String?) {
            activity?.findViewById<View>(R.id.ctl)?.findViewById<Toolbar>(R.id.toolbar)?.title = view.title
        }
    }
}