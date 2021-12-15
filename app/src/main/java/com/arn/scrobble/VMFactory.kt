package com.arn.scrobble

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.charts.ChartsVM
import com.arn.scrobble.edits.BlockedMetadataVM
import com.arn.scrobble.edits.RegexEditsVM
import com.arn.scrobble.edits.SimpleEditsVM
import com.arn.scrobble.friends.FriendsVM
import com.arn.scrobble.info.InfoVM
import com.arn.scrobble.info.TagInfoVM
import com.arn.scrobble.info.UserTagsVM
import com.arn.scrobble.recents.TracksVM
import com.arn.scrobble.search.SearchVM


object VMFactory : ViewModelProvider.Factory {
    private var application: Application? = null

    fun <T : ViewModel> getVM(fr: Fragment, vmClass: Class<T>): T {
        application = fr.activity?.application
        return ViewModelProvider(fr, this)[vmClass]
    }

    fun <T : ViewModel> getVM(act: AppCompatActivity, vmClass: Class<T>): T {
        application = act.application
        return ViewModelProvider(act, this)[vmClass]
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TracksVM::class.java) -> TracksVM(application!!) as T
            modelClass.isAssignableFrom(FriendsVM::class.java) -> FriendsVM(application!!) as T
            modelClass.isAssignableFrom(ChartsVM::class.java) -> ChartsVM(application!!) as T
            modelClass.isAssignableFrom(RandomVM::class.java) -> RandomVM(application!!) as T
            modelClass.isAssignableFrom(InfoVM::class.java) -> InfoVM(application!!) as T
            modelClass.isAssignableFrom(SearchVM::class.java) -> SearchVM(application!!) as T
            modelClass.isAssignableFrom(TagInfoVM::class.java) -> TagInfoVM(application!!) as T
            modelClass.isAssignableFrom(UserTagsVM::class.java) -> UserTagsVM(application!!) as T
            modelClass.isAssignableFrom(BillingViewModel::class.java) -> BillingViewModel(
                application!!
            ) as T
            modelClass.isAssignableFrom(MainNotifierViewModel::class.java) -> MainNotifierViewModel(
                application!!
            ) as T
            modelClass.isAssignableFrom(SimpleEditsVM::class.java) -> SimpleEditsVM(application!!) as T
            modelClass.isAssignableFrom(RegexEditsVM::class.java) -> RegexEditsVM(application!!) as T
            modelClass.isAssignableFrom(BlockedMetadataVM::class.java) -> BlockedMetadataVM(
                application!!
            ) as T
            else -> throw RuntimeException("Unknown VM class")
        }
    }
}