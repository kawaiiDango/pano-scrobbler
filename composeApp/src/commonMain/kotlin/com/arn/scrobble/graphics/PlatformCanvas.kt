package com.arn.scrobble.graphics

expect class PlatformCanvas(bitmap: PlatformBitmap) {
    fun drawBitmap(bitmap: PlatformBitmap, left: Int, top: Int)
    fun drawColor(color: Int)
    fun drawText(text: String, x: Float, y: Float)
    fun translate(x: Float, y: Float)
    fun measureText(text: String): Float
    fun setColor(color: Int)
    var textSize: Float
    val fontHeight: Float
    val descent: Float
    val leading: Float
}
