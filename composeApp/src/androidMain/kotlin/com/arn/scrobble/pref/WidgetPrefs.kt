package com.arn.scrobble.pref

import androidx.datastore.core.Serializer
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.widget.WidgetPeriod
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration.Companion.hours


@Serializable
data class WidgetPrefs(
    val widgets: Map<Int, SpecificWidgetPrefs> = emptyMap(),
    val chartsData: Map<ChartsDataKey, ChartsData> = emptyMap(),
    val version: Int = 0,
) {
    @Serializable
    data class SpecificWidgetPrefs(
        val tab: Int = Stuff.TYPE_ARTISTS,
        val bgAlpha: Float = 0.6f,
        val period: WidgetPeriod = WidgetPeriod.MONTH,
        val accountType: AccountType = AccountType.LASTFM,
        val images: Boolean = true,
        val shadow: Boolean = true,
    ) {
        val dataKey
            get() = ChartsDataKey(accountType, period, tab)
    }

    @Serializable
    data class ChartsWidgetListItem(
        val title: String,
        val subtitle: String? = null,
        val number: Int,
        val imageUrl: String? = null,
        // empty string if "looked up but no image found", null if not in cache
        val cachedImage: String? = null,
        val stonksDelta: Int? = null,
    )

    @Serializable
    data class ChartsData(
        val data: List<ChartsWidgetListItem>,
        val timePeriodString: String,
        val fetchTime: Long,
    ) {
        val canFetchAgain
            get() = (System.currentTimeMillis() - fetchTime >= Stuff.CHARTS_WIDGET_REFRESH_INTERVAL_HOURS.hours.inWholeMilliseconds / 2)
    }

    @JvmInline
    @Serializable
    value class ChartsDataKey(val str: String) {

        val accountType: AccountType?
            get() = str.split("|").getOrNull(0)
                ?.let { s -> AccountType.entries.find { it.name == s } }
        val period: WidgetPeriod?
            get() = str.split("|").getOrNull(1)
                ?.let { s -> WidgetPeriod.entries.find { it.name == s } }
        val tab: Int? get() = str.split("|").getOrNull(2)?.toIntOrNull()

        companion object {
            operator fun invoke(accountType: AccountType, period: WidgetPeriod, tab: Int) =
                ChartsDataKey("$accountType|$period|$tab")
        }
    }


    companion object {
        const val FILE_NAME = "widget-prefs.json"
    }
}

object WidgetPrefsSerializer : Serializer<WidgetPrefs> {
    override val defaultValue = WidgetPrefs()

    override suspend fun readFrom(input: InputStream) =
        try {
            Stuff.myJson.decodeFromStream<WidgetPrefs>(input)
        } catch (exception: SerializationException) {
            defaultValue
        }

    override suspend fun writeTo(
        t: WidgetPrefs,
        output: OutputStream
    ) = Stuff.myJson.encodeToStream(t, output)
}
