package com.arn.scrobble.api.atproto

import androidx.datastore.core.DataStore
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.withScrobbleAccount
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.first

/** The AT login is private to MainPrefs. Teal and Rocksky entries hold only its DID and PDS. */
class AtprotoSessions(private val prefs: DataStore<MainPrefs> = PlatformStuff.mainPrefs) {
    /** Bind the selected service. Reauthentication renews the shared session for the same DID. */
    suspend fun connect(
        session: AtprotoAppPasswordSession,
        user: UserCached,
        type: AccountType,
        expectedDid: String? = null,
    ) {
        require(type.isAtproto)
        check(expectedDid == null || expectedDid == session.did)
        prefs.updateData { p ->
            check(p.atprotoAccount?.authKey == expectedDid)
            val accounts = p.scrobbleAccounts.map {
                if (it.type.isAtproto && it.authKey == session.did) {
                    it.copy(
                        user = it.user.copy(name = user.name, url = it.type.atprotoProfileUrl(user.name)),
                        apiRoot = session.pds,
                    )
                } else it
            }
            val existing = accounts.firstOrNull { it.type == type }
            val account = existing ?: UserAccountSerializable(
                type,
                user.copy(url = type.atprotoProfileUrl(user.name)),
                session.did,
                session.pds,
            )
            p.copy(
                atprotoSession = session,
                scrobbleAccounts = accounts.withScrobbleAccount(account),
                currentAccountType = if (existing == null || p.currentAccount == null) type else p.currentAccountType,
            )
        }
    }

    /** Add another service using the existing credential, without refreshing or duplicating it. */
    suspend fun connectSaved(did: String, type: AccountType) {
        require(type.isAtproto)
        prefs.updateData { p ->
            val owner = p.scrobbleAccounts.firstOrNull { it.type.isAtproto && it.authKey == did }
            check(owner != null && p.atprotoSession?.matches(owner) == true)
            if (p.scrobbleAccounts.any { it.type == type }) return@updateData p
            p.copy(
                scrobbleAccounts = p.scrobbleAccounts.withScrobbleAccount(
                    UserAccountSerializable(
                        type,
                        owner.user.copy(url = type.atprotoProfileUrl(owner.user.name)),
                        did,
                        owner.apiRoot,
                    )
                ),
                currentAccountType = type,
            )
        }
    }

    internal suspend fun restore(account: UserAccountSerializable) = prefs.data.first().sessionFor(account)

    private fun MainPrefs.sessionFor(account: UserAccountSerializable) = atprotoSession?.takeIf { session ->
        account.type.isAtproto && session.matches(account) && scrobbleAccounts.any {
            it.type == account.type && session.matches(it)
        }
    }
}

internal fun AccountType.atprotoProfileUrl(handle: String): String = when (this) {
    AccountType.TEAL -> "https://teal.fm/profile/$handle"
    AccountType.ROCKSKY -> "https://rocksky.app/profile/$handle"
    else -> error("Not an AT Protocol scrobble destination")
}

internal fun AtprotoAppPasswordSession.matches(account: UserAccountSerializable) =
    did == account.authKey && pds == account.apiRoot
