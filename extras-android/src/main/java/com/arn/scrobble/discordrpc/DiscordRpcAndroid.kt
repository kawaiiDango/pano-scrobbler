package com.arn.scrobble.discordrpc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Process
import android.os.RemoteException
import co.touchlab.kermit.Logger
import com.discord.socialsdk.rpc.IDiscordRpcCallback
import com.discord.socialsdk.rpc.IDiscordRpcConnection
import com.discord.socialsdk.rpc.IDiscordRpcService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import java.util.UUID
import java.util.concurrent.Executors

/*
 * needs
 *   <queries>
 *     <intent><action android:name="com.discord.socialsdk.rpc.IDiscordRpcService" /></intent>
 *     <package android:name="com.discord" />
 *   </queries>
 *
 * Connects lazily: only setActivity()/start() (re)connect. clearActivity()/stop() never do.
 * Any drop (onClose, service/binding death, failed binder call) just resets state;
 * the next setActivity()/start() reconnects. Instance is reusable after stop().
 */
class DiscordRpcAndroid(
    context: Context,
    private val applicationId: Long,
    private val json: Json,
) {
    private val appContext = context.applicationContext

    // Blocking Binder calls live here. ALL mutable state below is touched only on this thread.
    private val worker = Executors.newSingleThreadExecutor { r ->
        Thread(r, "DiscordRpc").apply { isDaemon = true }
    }

    // ---- worker-thread-only state
    private var bound = false
    private var service: IDiscordRpcService? = null
    private var connection: IDiscordRpcConnection? = null
    private var currentCallback: IDiscordRpcCallback? = null
    private var ready = false

    @Volatile
    private var lastFrame: String? = null // re-sent once READY arrives

    private inner class Callback : IDiscordRpcCallback.Stub() {
        override fun onFrame(frame: String?) {
            if (frame == null) return
            worker.execute {
                if (currentCallback !== this) return@execute // stale connection
                Logger.withTag(TAG).d("<- $frame")
                when (val decoded = decodeFrame(frame)) {
                    is RpcFrame.Ready -> {
                        ready = true
                        lastFrame?.let { send(it) }
                    }

                    is RpcFrame.Error -> Logger.withTag(TAG)
                        .w("RPC error ${decoded.code}: ${decoded.message}")

                    else -> Unit
                }
            }
        }

        override fun onClose(code: Int, reason: String?) {
            // The SDK uses 1006 for "abnormal" and 4000 for "invalid client id".
            worker.execute {
                if (currentCallback !== this) return@execute
                Logger.withTag(TAG).w("closed: $code $reason")
                connection = null
                currentCallback = null
                ready = false
                // keep the binding; next ensureConnected() calls service.connect() again
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            worker.execute {
                if (!bound) return@execute // stop() raced us
                val svc = IDiscordRpcService.Stub.asInterface(binder)
                service = svc
                connectNow(svc)
            }
        }

        // Don't let the system auto-rebind; reconnect only on demand.
        override fun onServiceDisconnected(name: ComponentName) {
            worker.execute { teardown(disconnect = false) }
        }

        override fun onBindingDied(name: ComponentName) {
            worker.execute { teardown(disconnect = false) }
        }

        override fun onNullBinding(name: ComponentName) {
            worker.execute { teardown(disconnect = false) }
        }
    }

    fun setActivity(
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
    ) {
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

        val frame = json.encodeToString(SetActivityRequest(Process.myPid(), activity))
        lastFrame = frame
        worker.execute {
            ensureConnected()
            send(frame) // no-op until READY; READY replays lastFrame
        }
    }

    /* If there's no live connection, there's nothing to clear. */
    fun clearActivity(disconnect: Boolean) {
        lastFrame = null
        val frame = json.encodeToString(ClearActivityRequest(Process.myPid()))
        worker.execute { send(frame) }

        if (disconnect)
            worker.execute { teardown(disconnect = true) }
    }

    // ---------------------------------------------------------------- worker-only helpers

    private fun rpcIntent() = Intent(ACTION).setPackage(DISCORD_PACKAGE)

    private fun ensureConnected() {
        if (connection != null) return
        try {
            if (!bound) {
                if (appContext.packageManager.resolveService(rpcIntent(), 0) == null) return
                bound = appContext.bindService(
                    rpcIntent(), serviceConnection, Context.BIND_AUTO_CREATE
                )
                // onServiceConnected will connect
                if (!bound) Logger.withTag(TAG).w("bindService failed")
                return
            }
            // bound but no connection: either waiting for onServiceConnected (service == null)
            // or the RPC connection was closed.
            service?.let { connectNow(it) }
        } catch (e: Exception) { // SecurityException etc.
            Logger.withTag(TAG).e("ensureConnected failed", e)
            teardown(disconnect = false)
        }
    }

    private fun connectNow(svc: IDiscordRpcService) {
        val cb = Callback()
        currentCallback = cb
        ready = false
        try {
            val conn = svc.connect(applicationId, RPC_VERSION, cb)
            if (conn == null) {
                Logger.withTag(TAG).w("handshake rejected")
                currentCallback = null
                return
            }
            connection = conn
        } catch (e: RemoteException) {
            Logger.withTag(TAG).e("handshake failed", e)
            teardown(disconnect = false) // binder likely dead; rebind next time
        }
    }

    private fun teardown(disconnect: Boolean) {
        val conn = connection
        connection = null
        currentCallback = null
        service = null
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
            Logger.withTag(TAG).d("-> $frame")
            conn.sendFrame(frame)
        } catch (e: RemoteException) {
            Logger.withTag(TAG).e("sendFrame failed", e)
            teardown(disconnect = false) // next setActivity() reconnects
        }
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
        val env = json.decodeFromString(RpcEnvelope.serializer(), frame)
        val data = env.data?.takeIf { it !is JsonNull }
        when {
            env.evt == "READY" -> RpcFrame.Ready
            env.evt == "ERROR" -> {
                val err = data?.let { json.decodeFromJsonElement(RpcError.serializer(), it) }
                RpcFrame.Error(env.nonce, err?.code, err?.message)
            }

            env.cmd == "SET_ACTIVITY" -> RpcFrame.ActivitySet(
                env.nonce,
                data?.let { json.decodeFromJsonElement(ActivityResult.serializer(), it) },
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

        // https://discord.com/api/v9/oauth2/applications/$applicationId/assets
        // I don't know why strings fail on android
        val assetsMap = mapOf(
            "graphic_eq" to "1441021223982202943",
            "pause_circle" to "1441021224384724992",
        )
    }
}