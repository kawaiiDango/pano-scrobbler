/*
 * Copyright (C) 2016 Francisco José Montiel Navarro.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.franmontiel.persistentcookiejar.persistence

import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.util.date.GMTDate
import timber.log.Timber
import java.io.IOException
import java.io.ObjectInputStream
import java.io.Serial
import java.io.Serializable

class SerializableCookie : Serializable {
    @Transient
    private var cookie: Cookie? = null

    @Serial
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(ois: ObjectInputStream) {
        val name = ois.readObject() as String
        val value = ois.readObject() as String
        val expiresAt = ois.readLong().takeIf { it != NON_VALID_EXPIRES_AT }?.let { GMTDate(it) }
        var domain = ois.readObject() as String
        if (domain == "last.fm")
            domain = "www.last.fm"

        val path = ois.readObject() as String

        val cookieSerializable = Cookie(
            name = name,
            value = value,
            encoding = CookieEncoding.RAW,
            expires = expiresAt,
            domain = domain,
            path = path,
            secure = true,
        )

        cookie = cookieSerializable
    }

    companion object {
        private val TAG = SerializableCookie::class.java.getSimpleName()

        @Serial
        private val serialVersionUID = -8594045714036645534L

        private const val NON_VALID_EXPIRES_AT = -1L

        @OptIn(ExperimentalStdlibApi::class)
        fun decode(encodedCookie: String): Cookie? {
            val byteArrayInputStream = encodedCookie.hexToByteArray().inputStream()
            var cookie: Cookie? = null
            var objectInputStream: ObjectInputStream? = null
            try {
                objectInputStream = ObjectInputStream(byteArrayInputStream)
                cookie = (objectInputStream.readObject() as SerializableCookie).cookie
            } catch (e: IOException) {
                Timber.tag(TAG).d(e, "IOException in decodeCookie")
            } catch (e: ClassNotFoundException) {
                Timber.tag(TAG).d(e, "ClassNotFoundException in decodeCookie")
            } finally {
                if (objectInputStream != null) {
                    try {
                        objectInputStream.close()
                    } catch (e: IOException) {
                        Timber.tag(TAG).d(e, "Stream not closed in decodeCookie")
                    }
                }
            }
            return cookie
        }
    }
}