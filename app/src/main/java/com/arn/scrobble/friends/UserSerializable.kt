package com.arn.scrobble.friends

import android.os.Parcelable
import com.arn.scrobble.Stuff
import com.arn.scrobble.scrobbleable.AccountType
import de.umass.lastfm.ImageHolder
import de.umass.lastfm.ImageSize
import de.umass.lastfm.User
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class UserSerializable(
    var name: String,
    var url: String,
    var realname: String,
    var country: String,
    var registeredTime: Long,
    var imgUrls: Map<ImageSize, String>,
) : ImageHolder(), Parcelable {

    @IgnoredOnParcel
    var order: Int = 0

    @IgnoredOnParcel
    var lastUpdated = System.currentTimeMillis()

    init {
        super.imageUrls = imgUrls
    }

    fun updateFromUser(user: User) {
        name = user.name
        url = user.url
        realname = user.realname
        country = user.country
        registeredTime = user.registeredDate.time
        imgUrls = user.imageUrls
        super.imageUrls = user.imageUrls
        lastUpdated = System.currentTimeMillis()
    }

    fun toUser() = User(
        name,
        url,
        realname,
        country,
        registeredTime,
        imgUrls
    )

    companion object {
        fun User.toUserSerializable() = UserSerializable(
            name = name,
            url = url,
            realname = realname,
            country = country ?: "", // gnufm
            registeredTime = registeredDate?.time ?: Stuff.TIME_2002, // gnufm
            imgUrls = imageUrls
        )
    }
}

@Serializable
data class UserAccountSerializable(
    val type: AccountType,
    val user: UserSerializable,
    val authKey: String,
    val apiRoot: String? = null,
    val tlsNoVerify: Boolean = false,
)

@Parcelize
data class UserAccountTemp(
    val type: AccountType,
    var authKey: String,
    val apiRoot: String? = null,
    val tlsNoVerify: Boolean = false,
) : Parcelable