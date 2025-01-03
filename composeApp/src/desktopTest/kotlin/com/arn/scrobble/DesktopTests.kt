package com.arn.scrobble


import com.arn.scrobble.utils.PanoTimeFormatter
import kotlin.test.Test

class DesktopTests {

    @Test
    fun dateFormatter() {
        val millis = 1734346631_000L
        val millisLastYear = 1702724231_000L
        val millisLastMonth = 1731754631_000L
        val relative1 = PanoTimeFormatter.relative(millisLastMonth, true)
        val relative2 = PanoTimeFormatter.relative(millisLastYear, true)
        val relative3 = PanoTimeFormatter.relative(System.currentTimeMillis(), true)
        val relative4 = PanoTimeFormatter.relative(System.currentTimeMillis() - 2 * 60_000L, true)
        val full = PanoTimeFormatter.full(millis)
        val day = PanoTimeFormatter.day(millis)
        val month = PanoTimeFormatter.month(millis)
        val monthRange = PanoTimeFormatter.monthRange(millis, millisLastYear)
        val monthRange2 = PanoTimeFormatter.monthRange(millis, millisLastMonth)
        val dateRange = PanoTimeFormatter.dateRange(millis, millisLastYear)
        val dateRange2 = PanoTimeFormatter.dateRange(millis, millisLastMonth)
        val year = PanoTimeFormatter.year(millis)

        println("relative1: $relative1")
        println("relative2: $relative2")
        println("relative3: $relative3")
        println("relative4: $relative4")
        println("full: $full")
        println("day: $day")
        println("month: $month")
        println("monthRange: $monthRange")
        println("monthRange2: $monthRange2")
        println("dateRange: $dateRange")
        println("dateRange2: $dateRange2")
        println("year: $year")
    }

}