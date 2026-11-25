package com.arn.scrobble.api.teal

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountSerializable
import java.io.IOException

/** Updates only the matching login, preserving concurrent settings changes and logout. */
internal class TealAccountSessionStore(
    private val sessionId: String,
    private val read: suspend () -> List<UserAccountSerializable>,
    private val update: suspend ((List<UserAccountSerializable>) -> List<UserAccountSerializable>) -> Unit,
) : TealSessionStore {
    private fun current(accounts: List<UserAccountSerializable>): UserAccountSerializable {
        val account = accounts.firstOrNull { it.type == AccountType.TEALFM }
            ?: throw IOException("teal.fm account was disconnected.")
        check(TealApiClient.json.decodeFromString<TealSession>(account.authKey).id == sessionId) {
            "teal.fm account changed. Retry with the current account."
        }
        return account
    }

    override suspend fun load() = TealApiClient.json.decodeFromString<TealSession>(current(read()).authKey)

    override suspend fun save(session: TealSession) {
        check(session.id == sessionId)
        update { accounts ->
            val account = current(accounts)
            accounts.map { if (it == account) it.copy(authKey = TealApiClient.json.encodeToString(session)) else it }
        }
    }
}
