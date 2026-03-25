package com.arn.scrobble.onboarding

/*
 *  Copyright 2021 Matthew Nelson
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * */

import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor

// from https://gist.github.com/05nelsonm/8bc3fa33272fc596219a16b0eb2d217f

object WebViewProxyOverride {

    enum class ProxyOverrideState {
        Uninitialized,
        ProxySet,
        ProxyCleared,
        OverrideUnsupported,
        InvalidConfig,
    }

    private val _proxyOverrideStateFlow: MutableStateFlow<ProxyOverrideState> by lazy {
        MutableStateFlow(ProxyOverrideState.Uninitialized)
    }

    val proxyOverrideStateFlow
        get() = _proxyOverrideStateFlow.asStateFlow()

    private class SynchronousExecutor : Executor {
        override fun execute(command: Runnable?) {
            command?.run()
        }
    }

    /**
     * Helper method for discerning if the functionality for overriding the WebView's
     * system proxy is supported for the device.
     *
     * @see [WebViewFeature.isFeatureSupported]
     * */
    fun isWebViewProxyOverrideSupported(): Boolean =
        WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)

    fun setSocksProxy(
        socksHost: String,
        socksPort: Int,
        proxyConfigBuilder: ProxyConfig.Builder = ProxyConfig.Builder()
    ) {
        proxyConfigBuilder
            .addProxyRule("socks://${socksHost}:${socksPort}")
            .build().let { proxyConfig ->
                try {
                    ProxyController.getInstance().setProxyOverride(
                        proxyConfig,
                        SynchronousExecutor(),
                        {
                            _proxyOverrideStateFlow.value = ProxyOverrideState.ProxySet
                        }
                    )
                } catch (e: UnsupportedOperationException) {
                    _proxyOverrideStateFlow.value = ProxyOverrideState.OverrideUnsupported
                } catch (e: IllegalArgumentException) {
                    _proxyOverrideStateFlow.value = ProxyOverrideState.InvalidConfig
                }
            }
    }

    fun clearProxy() {
        if (
            _proxyOverrideStateFlow.value == ProxyOverrideState.ProxyCleared ||
            _proxyOverrideStateFlow.value == ProxyOverrideState.Uninitialized
        ) {
            return
        }

        try {
            ProxyController.getInstance().clearProxyOverride(
                SynchronousExecutor(),
                {
                    _proxyOverrideStateFlow.value = ProxyOverrideState.ProxyCleared
                }
            )
        } catch (e: UnsupportedOperationException) {
            _proxyOverrideStateFlow.value = ProxyOverrideState.OverrideUnsupported
        }
    }
}