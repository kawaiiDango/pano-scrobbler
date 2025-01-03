package com.arn.scrobble.graphics

expect class PlatformCanvas(bitmap: PlatformBitmap) {
    fun drawBitmap(bitmap: PlatformBitmap, left: Int, top: Int)
    fun drawColor(color: Int)
    fun drawText(text: String, x: Int, y: Int)
    fun translate(x: Int, y: Int)
    fun measureText(text: String): Int
    fun setColor(color: Int)
    var textSize: Float
    val fontHeight: Int
    val descent: Int
    val leading: Int
}
