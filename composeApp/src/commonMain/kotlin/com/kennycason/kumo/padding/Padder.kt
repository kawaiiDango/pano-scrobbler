package com.kennycason.kumo.padding

import com.kennycason.kumo.Word

/**
 * Created by kenny on 7/2/14.
 *
 * Add padding around words in the word cloud.
 */
interface Padder {
    fun pad(word: Word, padding: Int)
}
