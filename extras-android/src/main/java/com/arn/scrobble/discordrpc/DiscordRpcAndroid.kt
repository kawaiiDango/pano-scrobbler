package com.arn.scrobble.discordrpc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.os.SystemClock
import co.touchlab.kermit.Logger
import com.discord.socialsdk.rpc.IDiscordRpcCallback
import com.discord.socialsdk.rpc.IDiscordRpcConnection
import com.discord.socialsdk.rpc.IDiscordRpcService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/*
 * needs
 *   <queries>
 *     <intent><action android:name="com.discord.socialsdk.rpc.IDiscordRpcService" /></intent>
 *     <package android:name="com.discord" />
 *   </queries>
 *
 * Connects lazily: only setActivity() (re)connects. clearActivity() never does.
 *
 * RESULTS
 * setActivity() suspends until Discord acknowledges the SET_ACTIVITY frame (matched by nonce) or
 * something fails, so callers get a real answer even though every Binder call runs on the worker.
 *
 * BACKOFF
 * Every failure closes a gate: until it reopens, setActivity() returns immediately without touching
 * Discord. This matters because on Android a bind can *start* Discord's process, and setActivity()
 * is called on every metadata / seek / settings change.
 *
 *   persistent: not installed, handshake rejected / auth-ish codes (likely logged out), rate limited
 *               1 min, 2, 4, 8, 16, then 30 min max
 *   transient:  never READY, no ack, abnormal close, binder death, bind failure, SET_ACTIVITY error
 *               10 s, 20, 40, 80 ... then 10 min max
 *
 * The delay doubles per *consecutive* failure. The gate fully resets on the first successful ack,
 * on clearActivity(disconnect = true) (session over / feature disabled) and on resetBackoff().
 * A connection that dies while idle after having been healthy is NOT a failure: we just reconnect
 * on the next setActivity().
 *
 * Instance is reusable after clearActivity(disconnect = true).
 */
class DiscordRpcAndroid(
    context: Context,
    private val applicationId: Long,
    private val json: Json,
) {
    // ---------------------------------------------------------------- public result types

    private enum class FailureKind(
        val persistent: Boolean,
        val dropsConnection: Boolean = true,
    ) {
        /** Discord isn't installed, or doesn't expose the RPC service. */
        NOT_INSTALLED(persistent = true),

        /** Handshake rejected, or an auth-ish code: most likely logged out / bad client id / bad version. */
        REJECTED(persistent = true),
        RATE_LIMITED(persistent = true),
        BIND_FAILED(persistent = false),
        SERVICE_DIED(persistent = false),
        CLOSED(persistent = false),

        /** The connection never became READY in time. */
        TIMEOUT(persistent = false),

        /**
         * READY was received but this SET_ACTIVITY got no answer. The connection may well be fine
         * (e.g. Discord throttling updates), so don't tear it down; just back off.
         */
        NO_ACK(persistent = false, dropsConnection = false),

        /** Discord answered our SET_ACTIVITY with an error. The connection itself is fine. */
        REQUEST_ERROR(persistent = false, dropsConnection = false),
        INTERNAL(persistent = false),
    }

    private sealed interface Outcome {
        data object Success : Outcome

        /** [retryAtElapsedMs] is on the [SystemClock.elapsedRealtime] clock.
         * [backingOff] = true: nothing was attempted, the gate from an earlier failure is still closed. */
        data class Failure(
            val kind: FailureKind,
            val retryAtElapsedMs: Long,
            val backingOff: Boolean
        ) : Outcome
    }

    /** The failure that currently gates updates, or null if healthy. Handy for a UI hint. */
//    val lastFailure: StateFlow<Failure?>
//        get() = _lastFailure.asStateFlow()

    // ---------------------------------------------------------------- state

    private val appContext = context.applicationContext

    // Blocking Binder calls live here. ALL mutable state below is touched only on this thread.
    private val worker = Executors.newSingleThreadExecutor { r ->
        Thread(r, "DiscordRpc").apply { isDaemon = true }
    }

    // ---- worker-thread-only state
    private var bound = false
    private var connection: IDiscordRpcConnection? = null
    private var currentCallback: IDiscordRpcCallback? = null
    private var ready = false
    private var lastFrame: String? = null // re-sent once READY arrives
    private var pending: Pending? = null // the one setActivity() waiting for its ack
    private var consecutiveFailures = 0
    private val pid = 0 // official sdk sets it to 0. Android apps cant monitor pids of other apps

    private val _lastFailure = MutableStateFlow<Outcome.Failure?>(null)

    private class Pending(val nonce: String, val done: CompletableDeferred<Outcome>)

    /** Runs [block] on the worker. A stray exception must not crash the app from a background thread. */
    private fun post(block: () -> Unit) {
        worker.execute {
            try {
                block()
            } catch (e: Exception) {
                Logger.withTag(TAG).e(e) { "worker task failed" }
                try {
                    recordFailure(FailureKind.INTERNAL)
                } catch (_: Exception) {
                }
            }
        }
    }

    private inner class Callback : IDiscordRpcCallback.Stub() {
        override fun onFrame(frame: String?) {
            if (frame == null) return
            post {
                if (currentCallback !== this@Callback) return@post // stale connection
                Logger.withTag(TAG).d { "<- $frame" }
                when (val decoded = decodeFrame(frame)) {
                    is RpcFrame.Ready -> {
                        ready = true
                        lastFrame?.let { send(it) }
                    }

                    is RpcFrame.ActivitySet -> {
                        onHealthy()
                        if (decoded.nonce != null && decoded.nonce == pending?.nonce)
                            settlePending(Outcome.Success)
                    }

                    is RpcFrame.Error -> onRpcError(decoded.nonce, decoded.code, decoded.message)
                    is RpcFrame.Unknown -> Unit
                }
            }
        }

        override fun onClose(code: Int, reason: String?) {
            post {
                if (currentCallback !== this@Callback) return@post
                Logger.withTag(TAG).w { "closed: $code" }
                recordFailure(classifyClose(code))
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            post {
                if (!bound || connection != null) return@post // stop() raced us, or duplicate callback
                connectNow(IDiscordRpcService.Stub.asInterface(binder))
            }
        }

        // Every failure unbinds, so the system never auto-rebinds; we reconnect only on demand.
        override fun onServiceDisconnected(name: ComponentName) {
            post { if (bound) recordFailure(FailureKind.SERVICE_DIED) }
        }

        override fun onBindingDied(name: ComponentName) {
            post { if (bound) recordFailure(FailureKind.SERVICE_DIED) }
        }

        override fun onNullBinding(name: ComponentName) {
            post { if (bound) recordFailure(FailureKind.BIND_FAILED) }
        }
    }

    // ---------------------------------------------------------------- public api

    /**
     * Suspends until Discord acknowledges this activity, it fails, or [RESPONSE_TIMEOUT] passes.
     * Cancelling the caller is safe; the frame may still be delivered.
     */
    suspend fun setActivity(
        name: String,
        state: String,
        details: String,
        largeText: String,
        startTimeMillis: Long,
        endTimeMillis: Long?,
        artUrl: String,
        detailsUrl: String,
        isPlaying: Boolean,
        statusLine: Int,
        buttonText: String,
        buttonUrl: String,
    ): Boolean {
        val assets = RpcAssets(
            large_image = artUrl.ifEmpty { assetsMap["graphic_eq"] },
            large_text = largeText,
            small_image = if (isPlaying) null else assetsMap["pause_circle"],
            small_text = if (isPlaying) null else "Paused",
        )

        val buttons = if (buttonText.isNotEmpty() && buttonUrl.isNotEmpty())
            listOf(
                RpcButton(buttonText, buttonUrl)
            )
        else
            null

        val activity = RpcActivity(
            type = ActivityType.LISTENING.value,
            name = name.ifEmpty { null },
            state = state.ifEmpty { null },
            details = details.ifEmpty { null },
            details_url = detailsUrl.ifEmpty { null },
            status_display_type = when (statusLine) {
                2 -> StatusDisplayType.STATE.value
                1 -> StatusDisplayType.DETAILS.value
                else -> StatusDisplayType.NAME.value
            },
            timestamps = RpcTimestamps(
                start = startTimeMillis,
                end = endTimeMillis,
            ),
            assets = assets,
            buttons = buttons,
        )

        val request = SetActivityRequest(pid, activity)
        val frame = json.encodeToString(request)
        val done = CompletableDeferred<Outcome>()

        post { submitActivity(request.nonce, frame, done) }

        val outcome = withTimeoutOrNull(RESPONSE_TIMEOUT) { done.await() }
            ?: run {
                // Let the worker record it, so there is a single source of truth for the gate.
                post {
                    if (pending?.nonce == request.nonce) {
                        val kind = if (ready) FailureKind.NO_ACK else FailureKind.TIMEOUT
                        recordFailure(kind)
                    }
                }
                done.await()
            }

        return outcome is Outcome.Success
    }

    /**
     * Never connects: if there's no live connection, there's nothing to clear.
     * [disconnect] = true also ends the session and reopens the backoff gate.
     */
    fun clearActivity(disconnect: Boolean) {
        val frame = json.encodeToString(ClearActivityRequest(pid))
        post {
            lastFrame = null
            send(frame) // no-op unless READY
            if (disconnect) {
                resetFailureState()
                teardown(disconnect = true)
            }
        }
    }

    /** E.g. for a "reconnect" button in settings. */
//    fun resetBackoff() = post { resetFailureState() }

    // ---------------------------------------------------------------- worker-only helpers

    private fun rpcIntent() = Intent(ACTION).setPackage(DISCORD_PACKAGE)

    private fun submitActivity(nonce: String, frame: String, done: CompletableDeferred<Outcome>) {
        val gate = _lastFailure.value
            ?.takeIf { SystemClock.elapsedRealtime() < it.retryAtElapsedMs }
        if (gate != null) {
            done.complete(gate.copy(backingOff = true))
            return
        }

        lastFrame = frame
        pending = Pending(nonce, done)
        ensureConnected() // may fail synchronously, which settles `pending`
        send(frame) // no-op until READY; READY replays lastFrame
    }

    private fun ensureConnected() {
        if (bound) return // connecting, or connected
        try {
            if (appContext.packageManager.resolveService(rpcIntent(), 0) == null) {
                recordFailure(FailureKind.NOT_INSTALLED)
                return
            }
            bound = appContext.bindService(
                rpcIntent(), serviceConnection, Context.BIND_AUTO_CREATE
            )
            // on success, onServiceConnected will connect
            if (!bound) recordFailure(FailureKind.BIND_FAILED)
        } catch (e: Exception) { // SecurityException etc.
            recordFailure(FailureKind.BIND_FAILED)
        }
    }

    private fun connectNow(svc: IDiscordRpcService) {
        val cb = Callback()
        currentCallback = cb
        ready = false
        try {
            val conn = svc.connect(applicationId, RPC_VERSION, cb)
            if (conn == null) {
                recordFailure(FailureKind.REJECTED)
                return
            }
            connection = conn
        } catch (e: RemoteException) {
            recordFailure(FailureKind.SERVICE_DIED)
        }
    }

    private fun teardown(disconnect: Boolean) {
        val conn = connection
        connection = null
        currentCallback = null // makes late callbacks from the old connection stale
        ready = false
        if (disconnect) {
            try {
                conn?.disconnect()
            } catch (_: RemoteException) {
            }
        }
        if (bound) {
            try {
                appContext.unbindService(serviceConnection)
            } catch (_: IllegalArgumentException) {
            }
            bound = false
        }
    }

    private fun send(frame: String) {
        val conn = connection?.takeIf { ready } ?: return
        try {
            Logger.withTag(TAG).d { "-> $frame" }
            conn.sendFrame(frame)
        } catch (e: RemoteException) {
            recordFailure(FailureKind.SERVICE_DIED)
        }
    }

    private fun settlePending(outcome: Outcome) {
        pending?.done?.complete(outcome)
        pending = null
    }

    private fun onRpcError(nonce: String?, code: Int?, message: String?) {
        if (nonce != null && nonce != pending?.nonce) {
            // belongs to a superseded / cancelled / clear request; don't let it gate the current one
            Logger.withTag(TAG).w { "stale RPC error $code: $message" }
            return
        }
        Logger.withTag(TAG).w { "RPC error $code: $message" }
        recordFailure(classifyError(code))
    }

    // ---- failure policy

    private fun onHealthy() {
        if (consecutiveFailures > 0)
            Logger.withTag(TAG).i { "recovered after $consecutiveFailures failure(s)" }
        resetFailureState()
    }

    private fun resetFailureState() {
        consecutiveFailures = 0
        _lastFailure.value = null
    }

    private fun recordFailure(kind: FailureKind) {
        val idleDrop = pending == null && ready &&
                (kind == FailureKind.CLOSED || kind == FailureKind.SERVICE_DIED)
        if (idleDrop) {
            // Discord was killed / closed us while nobody was waiting. Not a failed update:
            // reconnect on the next setActivity().
            Logger.withTag(TAG).i { "dropped while idle: $kind" }
            teardown(disconnect = false)
            return
        }

        consecutiveFailures++
        val delayMs = backoffMillis(kind.persistent, consecutiveFailures)
        val failure =
            Outcome.Failure(kind, SystemClock.elapsedRealtime() + delayMs, backingOff = false)
        _lastFailure.value = failure
        Logger.withTag(TAG)
            .w { "failure #$consecutiveFailures: $kind; no attempts for ${delayMs / 1000}s" }

        if (kind.dropsConnection) teardown(disconnect = false)
        settlePending(failure)
    }

    private fun backoffMillis(persistent: Boolean, failures: Int): Long {
        val base = if (persistent) PERSISTENT_BASE else TRANSIENT_BASE
        val max = if (persistent) PERSISTENT_MAX else TRANSIENT_MAX
        val shift = (failures - 1).coerceIn(0, 16)
        return minOf(base.inWholeMilliseconds shl shift, max.inWholeMilliseconds)
    }

    // RPC close event codes:
    // https://discord.com/developers/docs/topics/opcodes-and-status-codes#rpc-rpc-close-event-codes
    // 1000 normal and 1006 abnormal fall through to CLOSED (transient).
    private fun classifyClose(code: Int) = when (code) {
        1003, // unsupported data
        4000, // invalid client id
        4001, // invalid origin
        4003, // token revoked
        4004, // invalid version
        4005, // invalid encoding
            -> FailureKind.REJECTED

        4002 -> FailureKind.RATE_LIMITED
        else -> FailureKind.CLOSED
    }

    // Codes in the data of an `evt: "ERROR"` frame. This is a different table from the close codes
    // (4000 here is "invalid payload", not "invalid client id"):
    // https://discord.com/developers/docs/topics/opcodes-and-status-codes#rpc-rpc-error-codes
    // The RPC docs say a client that isn't authenticated gets a 4006 on any command, so 4006 is the
    // prime suspect for "user is logged out".
    private fun classifyError(code: Int?) = when (code) {
        4002, // invalid command: resending the same frame can't help
        4006, // invalid permissions / not authenticated
        4007, // invalid client id
        4008, // invalid origin
        4009, // invalid token
        4010, // invalid user
        5000, // oauth2 error
            -> FailureKind.REJECTED

        // 1000 unknown, 4000 invalid payload, anything else: blame the request, not the connection
        else -> FailureKind.REQUEST_ERROR
    }

// ---------------------------------------------------------------- payload

    private enum class ActivityType(val value: Int) {
        PLAYING(0),
        LISTENING(2),
        WATCHING(3),
        COMPETING(5)
    }

    private enum class StatusDisplayType(val value: Int) {
        NAME(0),
        STATE(1),
        DETAILS(2)
    }

    @Serializable
    private data class RpcActivity(
        val type: Int? = null,
        val name: String? = null,
        val state: String? = null,
        val details: String? = null,
        val details_url: String? = null,
        val status_display_type: Int? = null,
        val timestamps: RpcTimestamps? = null,
        val assets: RpcAssets? = null,
        val buttons: List<RpcButton>? = null,
    )

    /* Milliseconds since the Unix epoch. */
    @Serializable
    private data class RpcTimestamps(
        val start: Long? = null,
        val end: Long? = null,
    )

    @Serializable
    private data class RpcAssets(
        val large_image: String? = null,
        val large_text: String? = null,
        val small_image: String? = null,
        val small_text: String? = null,
    )

    @Serializable
    private data class RpcButton(
        val label: String,
        val url: String,
    )

// ---------------------------------------------------------------- requests

    @Serializable
    private data class SetActivityRequest(val cmd: String, val args: Args, val nonce: String) {

        constructor(
            pid: Int,
            activity: RpcActivity,
        ) : this("SET_ACTIVITY", Args(pid, activity), UUID.randomUUID().toString())

        @Serializable
        data class Args(val pid: Int, val activity: RpcActivity)
    }

    /* Non-defaulted JsonElement so `"activity": null` is always encoded, even with explicitNulls = false. */
    @Serializable
    private data class ClearActivityRequest(val cmd: String, val args: Args, val nonce: String) {
        constructor(pid: Int) : this(
            "SET_ACTIVITY",
            Args(pid, JsonNull),
            UUID.randomUUID().toString()
        )

        @Serializable
        data class Args(val pid: Int, val activity: JsonElement)
    }

// ---------------------------------------------------------------- responses

    @Serializable
    private data class RpcEnvelope(
        val cmd: String? = null,
        val evt: String? = null,
        val nonce: String? = null,
        val data: JsonElement? = null,
    )

    @Serializable
    private data class ActivityResult(
        val type: Int? = null,
        val name: String? = null,
        val state: String? = null,
        val details: String? = null,
        val details_url: String? = null,
        val status_display_type: Int? = null,
        val application_id: String? = null,
        val platform: String? = null,
        val timestamps: RpcTimestamps? = null,
        val assets: RpcAssets? = null,
    )

    @Serializable
    private data class RpcError(val code: Int? = null, val message: String? = null)

    private sealed interface RpcFrame {
        object Ready : RpcFrame
        data class ActivitySet(val nonce: String?, val activity: ActivityResult?) : RpcFrame
        data class Error(val nonce: String?, val code: Int?, val message: String?) : RpcFrame
        data class Unknown(val raw: String) : RpcFrame
    }

    private fun decodeFrame(frame: String): RpcFrame = try {
        val env = json.decodeFromString<RpcEnvelope>(frame)
        val data = env.data?.takeIf { it !is JsonNull }
        when {
            env.evt == "READY" -> RpcFrame.Ready
            env.evt == "ERROR" -> {
                val err = data?.let { json.decodeFromJsonElement<RpcError>(it) }
                RpcFrame.Error(env.nonce, err?.code, err?.message)
            }

            env.cmd == "SET_ACTIVITY" -> RpcFrame.ActivitySet(
                env.nonce,
                data?.let { json.decodeFromJsonElement<ActivityResult>(it) },
            )

            else -> RpcFrame.Unknown(frame)
        }
    } catch (e: IllegalArgumentException) { // SerializationException extends this
        RpcFrame.Unknown(frame)
    }

    private companion object {
        const val TAG = "DiscordPresence"
        const val ACTION = "com.discord.socialsdk.rpc.IDiscordRpcService"
        const val DISCORD_PACKAGE = "com.discord"
        const val RPC_VERSION = "1"

        // Covers a cold start of Discord's process + handshake + the ack.
        val RESPONSE_TIMEOUT = 10.seconds

        val TRANSIENT_BASE = 10.seconds
        val TRANSIENT_MAX = 10.minutes
        val PERSISTENT_BASE = 1.minutes
        val PERSISTENT_MAX = 30.minutes

        // https://discord.com/api/v9/oauth2/applications/$applicationId/assets
        // I don't know why strings fail on android
        val assetsMap = mapOf(
            "graphic_eq" to "1441021223982202943",
            "pause_circle" to "1441021224384724992",
        )
    }
}
