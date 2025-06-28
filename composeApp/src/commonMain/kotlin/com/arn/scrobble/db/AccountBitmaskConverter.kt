package com.arn.scrobble.db

import androidx.room.TypeConverter
import com.arn.scrobble.api.AccountType

object AccountBitmaskConverter {
    @TypeConverter
    fun accountTypesToBitMask(services: Set<AccountType>) =
        services.fold(0) { acc, service -> acc or (1 shl service.id) }

    @TypeConverter
    fun bitMaskToAccountTypes(bitMask: Int) =
        AccountType.entries
            .filter { (bitMask and (1 shl it.id)) != 0 }
            .toSet()

}
