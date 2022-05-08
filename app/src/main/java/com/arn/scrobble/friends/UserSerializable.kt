package com.arn.scrobble.friends

import de.umass.lastfm.ImageHolder
import de.umass.lastfm.ImageSize
import de.umass.lastfm.User
import kotlinx.serialization.Serializable

@Serializable
data class UserSerializable(
    var name: String,
    var url: String,
    var realname: String,
    var country: String,
    var registeredTime: Long,
    var imgUrls: Map<ImageSize, String>,
) : ImageHolder() {
    var order: Int = 0
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
            country = country,
            registeredTime = registeredDate.time,
            imgUrls = imageUrls
        )
    }
}