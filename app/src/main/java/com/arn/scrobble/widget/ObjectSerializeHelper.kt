package com.arn.scrobble.widget

import java.io.*
import android.util.Base64

internal object ObjectSerializeHelper {
    fun convertToString(s: Serializable): String? {
        try {
            ByteArrayOutputStream().use { baos ->
                ObjectOutputStream(baos).use { oos ->
                    oos.writeObject(s)
                    return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun <T : Serializable?> convertFrom(objectAsString: String?): T? {
        val data = Base64.decode(objectAsString ?: return null, Base64.DEFAULT)
        try {
            ObjectInputStream(ByteArrayInputStream(data)).use { ois -> return ois.readObject() as T }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            return null
        }
    }
}