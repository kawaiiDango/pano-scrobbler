package com.arn.scrobble

import android.graphics.Bitmap
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


/**
 * Created by arn on 06/09/2017.
 */
class TestFragment : Fragment(R.layout.content_avd_test) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val avd = view.findViewById<ImageView>(R.id.test_avd).drawable as AnimatedVectorDrawable
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000)
            avd.start()
        }
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val file = File(activity!!.filesDir.path, "ic_launcher.png")
            FileOutputStream(file).use {
                ContextCompat.getDrawable(context!!, R.drawable.ic_launcher_for_export)!!
                    .toBitmap(width = 512, height = 512)
                    .compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            withContext(Dispatchers.Main) {
                Stuff.toast(context, "Saved to ${file.absolutePath}")
            }
        }
    }
}