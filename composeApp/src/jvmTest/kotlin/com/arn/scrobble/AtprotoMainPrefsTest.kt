package com.arn.scrobble

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.atproto.AtprotoAppPasswordSession
import com.arn.scrobble.api.atproto.AtprotoSessions
import com.arn.scrobble.api.atproto.atprotoProfileUrl
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.withScrobbleAccount
import com.arn.scrobble.api.withoutScrobbleAccount
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.onboarding.LoginDestinations
import com.arn.scrobble.pref.ImExporter
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.utils.DesktopStuff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AtprotoMainPrefsTest {
    @Test
    fun eachProviderOpensItsOwnSignInAndIsNotTreatedAsCustomServer() {
        for (type in destinations) {
            assertEquals(PanoRoute.LoginAtproto(type), LoginDestinations.route(type))
            assertTrue(type.isAtproto)
            assertFalse(type.isCustom)
        }
    }

    @Test
    fun addRockskyToTealWithoutSigningInAgain() = addSecondService(AccountType.TEAL, AccountType.ROCKSKY)

    @Test
    fun addTealToRockskyWithoutSigningInAgain() = addSecondService(AccountType.ROCKSKY, AccountType.TEAL)

    private fun addSecondService(first: AccountType, second: AccountType) = withPrefsFile { file ->
        file.open { prefs ->
            AtprotoSessions(prefs).connect(appSession(), user, first)
            val p = prefs.data.first()
            assertEquals(first, p.currentAccountType)
            assertEquals(first, p.scrobbleAccounts.single().type)
            prefs.updateData { it.copy(scrobbleAccounts = listOf(p.scrobbleAccounts.single().copy(canScrobble = false))) }
        }
        file.open { prefs ->
            val before = prefs.data.first()
            val sessions = AtprotoSessions(prefs)
            assertFailsWith<IllegalStateException> { sessions.connectSaved(OTHER_DID, second) }
            sessions.connectSaved(DID, second)
            sessions.connectSaved(DID, first) // Stale repeat action must not resume a paused service.
            val after = prefs.data.first()
            val previousCredential = assertNotNull(before.atprotoSession)
            val reusedCredential = assertNotNull(after.atprotoSession)
            assertEquals(previousCredential.did, reusedCredential.did)
            assertEquals(previousCredential.pds, reusedCredential.pds)
            assertEquals(previousCredential.accessJwt, reusedCredential.accessJwt)
            assertEquals(previousCredential.refreshJwt, reusedCredential.refreshJwt)
            assertEquals(before.scrobbleAccounts.single(), after.scrobbleAccounts.single { it.type == first })
            assertEquals(
                UserAccountSerializable(second, user.copy(url = second.atprotoProfileUrl(user.name)), DID, PDS),
                after.currentAccount,
            )
            assertEquals("https://teal.fm/profile/${user.name}", after.scrobbleAccounts.single { it.type == AccountType.TEAL }.user.url)
            assertEquals("https://rocksky.app/profile/${user.name}", after.scrobbleAccounts.single { it.type == AccountType.ROCKSKY }.user.url)
            assertEquals(setOf(second), after.scrobbleAccounts.filter { it.canScrobble }.map { it.type }.toSet())
        }
    }

    @Test
    fun appPasswordLoginReopensForBothDestinationsAndRotatesOnlyTheSession() = withPrefsFile { file ->
        file.writeText("""{"scrobblerEnabled":false}""")
        file.open { prefs ->
            assertFalse(prefs.data.first().scrobblerEnabled)
            assertNull(prefs.data.first().atprotoSession)
            AtprotoSessions(prefs).connectBoth(appSession())
        }
        val json = Json.parseToJsonElement(file.readText()).jsonObject
        val accounts = json.getValue("scrobbleAccounts").jsonArray.toString()
        assertFalse("synthetic-" in accounts)
        assertEquals(setOf("did", "pds", "accessJwt", "refreshJwt"), json.getValue("atprotoSession").jsonObject.keys)

        val before = file.open { prefs ->
            val accountsBefore = prefs.data.first().scrobbleAccounts
            for (account in accountsBefore) {
                assertEquals(DID, account.authKey)
                assertEquals(PDS, account.apiRoot)
                val restored = assertIs<AtprotoAppPasswordSession>(AtprotoSessions(prefs).restore(account))
                assertEquals("synthetic-access", restored.accessJwt)
                assertEquals("synthetic-refresh", restored.refreshJwt)
            }
            AtprotoSessions(prefs).connect(appSession(access = "synthetic-rotated"), user, AccountType.ROCKSKY, DID)
            accountsBefore
        }
        file.open { prefs ->
            assertEquals(before, prefs.data.first().scrobbleAccounts)
            val sessions = AtprotoSessions(prefs)
            for (account in before)
                assertEquals("synthetic-rotated", assertIs<AtprotoAppPasswordSession>(sessions.restore(account)).accessJwt)
            assertNull(sessions.restore(before.first().copy(authKey = OTHER_DID)))
            assertNull(sessions.restore(before.first().copy(apiRoot = "https://stale.example")))
            assertFailsWith<IllegalStateException> { sessions.connect(appSession(did = OTHER_DID), user, AccountType.ROCKSKY, DID) }
        }
    }

    @Test
    fun removingOneServiceKeepsTheSharedLoginAndRemovingTheLastClearsIt() = withPrefsFile { file ->
        val unrelated = UserAccountSerializable(AccountType.GNUFM, user, "existing-auth", "https://other.example")
        val old = file.open { prefs ->
            prefs.updateData { it.copy(scrobbleAccounts = listOf(unrelated)) }
            AtprotoSessions(prefs).connectBoth(appSession())
            prefs.data.first().scrobbleAccounts.single { it.type == AccountType.TEAL }
        }
        file.open { prefs ->
            val p = prefs.updateData { it.withoutScrobbleAccount(AccountType.TEAL) }
            assertEquals(AccountType.ROCKSKY, p.currentAccountType)
            assertNull(AtprotoSessions(prefs).restore(old))
        }
        file.open { prefs ->
            val sessions = AtprotoSessions(prefs)
            val rocksky = prefs.data.first().scrobbleAccounts.single { it.type.isAtproto }
            assertEquals(AccountType.ROCKSKY, rocksky.type)
            assertEquals(DID, sessions.restore(rocksky)?.did)
            prefs.updateData { it.withoutScrobbleAccount(AccountType.ROCKSKY) }
        }
        file.open { prefs ->
            val p = prefs.data.first()
            assertNull(p.atprotoSession)
            assertEquals(listOf(unrelated), p.scrobbleAccounts)
            assertEquals(unrelated, p.currentAccount)
            assertFailsWith<IllegalStateException> { AtprotoSessions(prefs).connectSaved(DID, AccountType.TEAL) }
        }
    }

    @Test
    fun reconnectWhileAddingSecondServiceKeepsFirstServicePaused() = withPrefsFile { file ->
        file.open { prefs ->
            val sessions = AtprotoSessions(prefs)
            sessions.connect(appSession(), user, AccountType.ROCKSKY)
            prefs.updateData { p ->
                p.copy(atprotoSession = null, scrobbleAccounts = listOf(p.scrobbleAccounts.single().copy(canScrobble = false)))
            }
            assertFailsWith<IllegalStateException> { sessions.connectSaved(DID, AccountType.TEAL) }
            sessions.connect(appSession(), user, AccountType.TEAL, DID)
            val p = prefs.data.first()
            assertEquals(setOf(AccountType.TEAL), p.scrobbleAccounts.filter { it.canScrobble }.map { it.type }.toSet())
            assertEquals(AccountType.TEAL, p.currentAccountType)
            assertEquals(destinations, p.scrobbleAccounts.map { it.type }.toSet())
        }
    }

    @Test
    fun duplicateRootIsExemptOnlyForTheAtPairAndFailureLeavesPrefsIntact() = withPrefsFile { file ->
        val otherService = UserAccountSerializable(AccountType.GNUFM, user, "other-key", PDS)
        file.open { prefs ->
            val sessions = AtprotoSessions(prefs)
            sessions.connectBoth(appSession())
            val entries = prefs.data.first().scrobbleAccounts
            assertFailsWith<ApiException> { entries.withScrobbleAccount(otherService) }
            assertFailsWith<ApiException> { listOf(otherService).withScrobbleAccount(otherService.copy(type = AccountType.PLEROMA)) }
            assertEquals(1, listOf(otherService).withScrobbleAccount(otherService.copy(canScrobble = false)).size)
            prefs.updateData { it.copy(scrobbleAccounts = entries + otherService.copy(apiRoot = "https://occupied.example")) }
            assertFailsWith<ApiException> {
                sessions.connect(appSession(pds = "https://occupied.example"), user, AccountType.TEAL, DID)
            }
        }
        file.open { prefs ->
            val p = prefs.data.first()
            assertEquals(DID, p.atprotoSession?.did)
            assertEquals(setOf(DID), p.scrobbleAccounts.filter { it.type.isAtproto }.map { it.authKey }.toSet())
        }
    }

    @Test
    fun publicExportOmitsLoginAndPublicImportCannotOverwriteIt() = withPrefsFile { file ->
        val session = appSession()
        file.open { prefs ->
            AtprotoSessions(prefs).connectBoth(session)
            // Loading PanoDb initializes its desktop singleton before Room builds the test database.
            DesktopStuff.parseCmdlineArgs(arrayOf("--data-dir", file.parent))
            val db = Room.inMemoryDatabaseBuilder<PanoDb>().setDriver(BundledSQLiteDriver()).build()
            try {
                val exporter = ImExporter(db, prefs)
                val output = ByteArrayOutputStream()
                assertTrue(exporter.export(output))
                val exported = output.toString(Charsets.UTF_8.name())
                for (secret in listOf("atprotoSession", "scrobbleAccounts", DID, PDS, session.accessJwt, session.refreshJwt))
                    assertFalse(secret in exported)

                val injected = """{"pano_version":1,"settings":{"scrobblerEnabled":false,"atprotoSession":null,"scrobbleAccounts":[]}}"""
                assertEquals(setOf(ImExporter.ImportTypes.settings), exporter.createImportTypes(injected.byteInputStream()))
                exporter.import(setOf(ImExporter.ImportTypes.settings), ImExporter.WriteMode.replace_all)
            } finally {
                db.close()
            }
        }
        file.open { prefs ->
            val imported = prefs.data.first()
            assertFalse(imported.scrobblerEnabled)
            assertEquals(DID, imported.atprotoSession?.did)
            assertEquals(destinations, imported.scrobbleAccounts.map { it.type }.toSet())
            val restored = assertIs<AtprotoAppPasswordSession>(AtprotoSessions(prefs).restore(imported.scrobbleAccounts.first()))
            assertEquals(session.accessJwt, restored.accessJwt)
            assertEquals(session.refreshJwt, restored.refreshJwt)
        }
    }

    private fun appSession(did: String = DID, access: String = "synthetic-access", pds: String = PDS) =
        AtprotoAppPasswordSession(did, pds, access, "synthetic-refresh")

    private suspend fun AtprotoSessions.connectBoth(session: AtprotoAppPasswordSession) {
        connect(session, user, AccountType.TEAL)
        connectSaved(session.did, AccountType.ROCKSKY)
    }

    private fun withPrefsFile(block: suspend (File) -> Unit) = runBlocking {
        val dir = createTempDirectory("atproto-prefs-test-").toFile()
        try {
            block(dir.resolve(MainPrefs.FILE_NAME))
        } finally {
            check(dir.deleteRecursively())
        }
    }

    private suspend fun <T> File.open(block: suspend (DataStore<MainPrefs>) -> T): T {
        val job = Job()
        val store = DataStoreFactory.create(MainPrefs.dataStoreSerializer, scope = CoroutineScope(Dispatchers.IO + job)) { this }
        return try {
            block(store)
        } finally {
            job.cancelAndJoin()
        }
    }

    private companion object {
        const val DID = "did:web:one.example"
        const val OTHER_DID = "did:web:two.example"
        const val PDS = "https://pds.example"
        val destinations = setOf(AccountType.TEAL, AccountType.ROCKSKY)
        val user = UserCached("one.example", "", "Example", "", 0)
    }
}
