package com.arn.scrobble

import kotlin.test.Test


class RegexTest {

    @Test
    fun regexTest() {
        val fs = arrayOf(
            "%1\$s от %2\$s",
            "%1\$s esitajalt %2\$s",
            "%1\$s દ્વારા %2\$s",
            "%1\$s – %2\$s",
            "%1\$s par %2\$s",
            "%1\$s од %2\$s",
            "%1\$s de la %2\$s",
            "%1\$s, izvajalec: %2\$s",
            "%2\$s को %1\$s",
            "%1\$s od interpreta %2\$s",
            "%2\$s का %1\$s",
            "%2\$s ਵੱਲੋਂ %1\$s",
            "%1\$s eftir %2\$s",
            "%2\$s - %1\$s",
            "%1\$s của %2\$s",
            "%1\$s av %2\$s",
            "%1\$s by %2\$s",
            "%1\$s โดย %2\$s",
            "%2\$s-н %1\$s",
            "%2\$s चे %1\$s",
            "%2\$s-ի %1\$s երգը",
            "%1\$s（%2\$s）",
            "%1\$s በ%2\$s",
            "%2\$sගේ %1\$s",
            "\"%1\$s\", виконавець: %2\$s",
            "%1\$s از %2\$s",
            "%2\$s کا %1\$s",
            "%1\$s de: %2\$s",
            "%1\$s ដោយ %2\$s",
            "%2\$s的《%1\$s》",
            "„%1\$s“ von %2\$s",
            "%2\$sৰ %1\$s",
            "%1\$s ад %2\$s",
            "%2\$s ಅವರಿಂದ %1\$s",
            "%2\$s ద్వారా %1\$s",
            "%2\$s орындайтын %1\$s",
            "「%1\$s」，演出者：%2\$s",
            "%1\$s από %2\$s",
            "%1\$s van %2\$s",
            "%1\$s / %2\$s",
            "%1\$s af %2\$s",
            "%2\$s tərəfindən %1\$s",
            "%2\$s аткаруусундагы %1\$s деген ыр",
            "%1\$s na %2\$s",
            "%1\$s, შემსრულებელი: %2\$s",
            "%1\$s nga %2\$s",
            "%1\$s بصوت %2\$s",
            "%1\$s ngo-%2\$s",
            "%2\$s এর %1\$s",
            "%2\$s – %1\$s",
            "%1\$s deur %2\$s",
            "%1\$s ໂດຍ %2\$s",
            "%1\$s (izpildītājs: %2\$s)",
            "%1\$s ni %2\$s",
            "%1\$s သီဆိုသူ %2\$s",
            "%1\$s izvođača %2\$s",
            "%1\$s од извођача %2\$s",
            "\"%1\$s\", %2\$s",
            "%1\$s, ଗାୟକ %2\$s",
            "%1\$s de %2\$s",
            "%1\$s di %2\$s",
            "%2\$s: %1\$s",
            "%2\$s의 %1\$s",
            "%1\$s, %2\$s",
            "%2\$s இன் %1\$s",
            "%1\$s od izvođača %2\$s",
            "%1\$s של %2\$s",
            "%1\$s oleh %2\$s",
        )

        for (f in fs) {
            val titleStr = f.replace("%1\$s", "title")
                .replace("%2\$s", "artist")
            val arr = extractMeta(titleStr, f)
            assert(arr != null && arr[0] == "artist" && arr[1] == "title")
        }
    }

    private fun extractMeta(titleStr: String, formatStr: String): Array<String>? {
        val tpos = formatStr.indexOf("%1\$s")
        val apos = formatStr.indexOf("%2\$s")
        val regex = formatStr
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("%1\$s", "(.*)")
            .replace("%2\$s", "(.*)")
        return try {
            val m = regex.toRegex().find(titleStr)!!
            val g = m.groupValues
            if (g.size != 3)
                throw Exception("group size != 3")
            if (tpos > apos)
                arrayOf(g[1], g[2])
            else
                arrayOf(g[2], g[1])

        } catch (e: Exception) {
            print("$titleStr $formatStr")
            e.printStackTrace()
            null
        }
    }
}