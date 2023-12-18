package com.arn.scrobble.friends

import android.os.Parcelable
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.utils.Stuff
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class UserCached(
    val name: String,
    val url: String,
    val realname: String,
    val country: String,
    val registeredTime: Long,
    val largeImage: String = "",
    @IgnoredOnParcel
    val order: Int = 0
) : Parcelable {

    @IgnoredOnParcel
    var lastUpdated = System.currentTimeMillis()
    val isSelf get() = Scrobblables.currentScrobblableUser?.name == name

    companion object {
        fun User.toUserCached() = UserCached(
            name = name,
            url = url,
            realname = realname ?: "",
            country = country ?: "None", // gnufm
            registeredTime = (registered ?: Stuff.TIME_2002) * 1000L, // gnufm
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

@Parcelize
data class UserAccountTemp(
    val type: AccountType,
    val authKey: String,
    val apiRoot: String? = null,
) : Parcelable