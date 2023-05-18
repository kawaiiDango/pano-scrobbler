package com.arn.scrobble.ui

import coil.intercept.Interceptor

class DemoInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain) =
        chain.proceed(chain.request)
}
