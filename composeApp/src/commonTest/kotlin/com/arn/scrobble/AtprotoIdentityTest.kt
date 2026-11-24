package com.arn.scrobble

import com.arn.scrobble.api.atproto.resolveAtprotoHandle
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AtprotoIdentityTest {
    @Test
    fun resolvesPlcDirectoryDocument() = runBlocking {
        val did = "did:plc:abcdefghijklmnopqrstuvwx"
        val identity = resolveAtprotoHandle("alice.example") { url ->
            when (url) {
                "https://public.api.bsky.app/xrpc/com.atproto.identity.resolveHandle?handle=alice.example" ->
                    """{"did":"$did"}"""
                "https://plc.directory/$did" ->
                    """{"id":"$did","alsoKnownAs":["at://alice.example"],"service":[
                        {"id":"$did#atproto_pds","type":"AtprotoPersonalDataServer","serviceEndpoint":"https://pds.example"}]}"""
                else -> error("Unexpected request: $url")
            }
        }
        assertEquals(did, identity.did)
        assertEquals("https://pds.example", identity.pds)
    }

    private suspend fun resolve(claim: String, pds: String) = resolveAtprotoHandle("@Alice.Example") { url ->
        when (url) {
            "https://public.api.bsky.app/xrpc/com.atproto.identity.resolveHandle?handle=alice.example" ->
                """{"did":"did:web:id.example"}"""
            "https://id.example/.well-known/did.json" ->
                """{"id":"did:web:id.example","alsoKnownAs":["at://$claim"],"service":[
                    {"id":"#atproto_pds","type":"AtprotoPersonalDataServer","serviceEndpoint":"$pds"}]}"""
            else -> error("Unexpected request: $url")
        }
    }

    @Test
    fun resolvesIndependentPdsHost() = runBlocking {
        assertEquals("https://pds.example", resolve("alice.example", "https://pds.example/").pds)
    }

    @Test
    fun rejectsUnverifiedBindingAndInsecurePds() = runBlocking {
        assertFailsWith<IllegalArgumentException> { resolve("bob.example", "https://pds.example") }
        assertFailsWith<IllegalArgumentException> { resolve("alice.example", "http://pds.example") }
        Unit
    }
}
