package com.arn.scrobble

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arn.scrobble.charts.ChartsVM
import com.arn.scrobble.info.InfoVM


object VMFactory: ViewModelProvider.Factory {
    private var application: Application? = null

    fun <T : ViewModel>getVM(fr: Fragment, vmClass: Class<T>): T {
        application = fr.activity?.application
        return ViewModelProvider(fr, this).get(vmClass)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TracksVM::class.java) -> TracksVM(application!!) as T
            modelClass.isAssignableFrom(FriendsVM::class.java) -> FriendsVM(application!!) as T
            modelClass.isAssignableFrom(ChartsVM::class.java) -> ChartsVM(application!!) as T
            modelClass.isAssignableFrom(RandomVM::class.java) -> RandomVM(application!!) as T
            modelClass.isAssignableFrom(InfoVM::class.java) -> InfoVM(application!!) as T
            else -> throw RuntimeException("Unknown VM class")
        }
    }
}