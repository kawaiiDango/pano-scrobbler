package com.arn.scrobble.ui

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.ref.WeakReference

class EntryInfoHandler(private val loadImgWr: WeakReference<LoadImgInterface>) : Handler(Looper.getMainLooper()) {
    private var count = 0
    override fun handleMessage(m: Message) {
        if (count > 0)
            count --
        val pos = m.arg1
        loadImgWr.get()?.loadImg(pos)
    }
    fun sendMessage(what:Int, pos:Int){
        if (!hasMessages(what))
            count ++
        else
            removeMessages(what)
        val msg = obtainMessage(what, pos, 0)
        sendMessageDelayed(msg, count * 50L)
    }
    fun cancelAll(){
        removeCallbacksAndMessages(null)
        count = 0
    }
}