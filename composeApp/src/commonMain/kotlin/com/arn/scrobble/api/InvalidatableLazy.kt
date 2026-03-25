package com.arn.scrobble.api

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A lazy-like delegate that re-initializes its value when [invalidate] is called,
 * but only on the *next* access — never while the value is in use.
 */
class InvalidatableLazy<T>(private val init: () -> T) : ReadOnlyProperty<Any?, T> {

    @Volatile
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        value ?: init().also { value = it }

    fun invalidate() {
        value = null
    }
}

fun <T> invalidatableLazy(init: () -> T) = InvalidatableLazy(init)