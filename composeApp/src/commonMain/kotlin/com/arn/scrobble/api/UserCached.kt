package com.arn.scrobble.api

import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.utils.Stuff
import kotlinx.serialization.Serializable

@Serializable
data class UserCached(
    val name: String,
    val url: String,
    val realname: String,
    val country: String,
    val registeredTime: Long,
    val largeImage: String = "",
    val order: Int = 0,
) {

    var lastUpdated = System.currentTimeMillis()
    val isSelf get() = Scrobblables.currentAccount.value?.user?.let { it.name == name && it.url == url } == true

    inline fun <reified T> key() =
        T::class.qualifiedName + "|" + "$name|" + Scrobblables.current?.userAccount?.type

    companion object {
        fun User.toUserCached() = UserCached(
            name = name,
            url = url,
            realname = realname ?: "",
            country = country ?: "None", // gnufm
            registeredTime = registered ?: Stuff.TIME_2002, // gnufm
            largeImage = webp300 ?: ""
        )
    }
}

@Serializable
data class UserAccountSerializable(
    val type: AccountType,
    val user: UserCached,
    val authKey: String,
    val apiRoot: String? = null,
)

@Serializable
data class UserAccountTemp(
    val type: AccountType,
    val authKey: String,
    val apiRoot: String? = null,
)