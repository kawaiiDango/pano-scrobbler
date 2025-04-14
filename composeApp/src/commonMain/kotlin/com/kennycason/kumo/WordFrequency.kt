package com.kennycason.kumo


/**
 * Created by kenny on 6/29/14.
 */
class WordFrequency(val word: String, val frequency: Int) : Comparable<WordFrequency> {
    override fun compareTo(other: WordFrequency): Int {
        return other.frequency - frequency
    }
}
