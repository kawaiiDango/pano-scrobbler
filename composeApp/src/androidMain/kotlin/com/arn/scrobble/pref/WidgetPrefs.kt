package com.arn.scrobble.pref

import androidx.datastore.core.Serializer
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.widget.ChartsWidgetListItem
import com.arn.scrobble.widget.WidgetPeriods
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class SpecificWidgetPrefs(
    val tab: Int = Stuff.TYPE_ARTISTS,
    val bgAlpha: Float = 0.6f,
    val period: WidgetPeriods = WidgetPeriods.MONTH,
    val shadow: Boolean = true,
)

@Serializable
data class WidgetPrefs(
    val widgets: Map<Int, SpecificWidgetPrefs> = emptyMap(),
    val charts: Map<WidgetPeriods, ChartsData> = emptyMap(),
    val lastFetched: Long = -1,
    val version: Int = 0,
) {
    @Serializable
    data class ChartsData(
        val artists: List<ChartsWidgetListItem>,
        val albums: List<ChartsWidgetListItem>,
        val tracks: List<ChartsWidgetListItem>,
        val timePeriodString: String,
    )

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
