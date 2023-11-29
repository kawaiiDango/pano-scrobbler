package com.arn.scrobble.pref

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.App
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ContentAppListBinding
import com.arn.scrobble.ui.FabData
import com.arn.scrobble.ui.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.UiUtils.toast
import com.arn.scrobble.utils.Stuff
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.flow.filter


/**
 * Created by arn on 05/09/2017.
 */
class AppListFragment : Fragment() {
    private val prefs = App.prefs
    private val viewModel by viewModels<AppListVM>()
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()
    private var _binding: ContentAppListBinding? = null
    private val binding
        get() = _binding!!

    private val allowedPackagesArg
        get() = arguments?.getStringArray(Stuff.ARG_ALLOWED_PACKAGES)?.toSet()

    private val singleChoiceArg
        get() = arguments?.getBoolean(Stuff.ARG_SINGLE_CHOICE, false) ?: false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupAxisTransitions(MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        saveData()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        postponeEnterTransition()

        binding.appList.setupInsets()
        binding.progress.show()

        viewModel.selectedPackages += allowedPackagesArg ?: prefs.allowedPackages

        if (!binding.appList.isInTouchMode)
            binding.appList.requestFocus()

        if (Stuff.isTv) {
            requireContext().toast(R.string.press_back)
        }

        if (singleChoiceArg)
            setTitle(R.string.choose_an_app)

        binding.appList.layoutManager = LinearLayoutManager(context)
        val adapter = AppListAdapter(requireActivity(), viewModel, singleChoiceArg)
        binding.appList.adapter = adapter
        if (!Stuff.isTv) {
            binding.appList.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrollStateChanged(view: RecyclerView, scrollState: Int) {
                    if (!viewModel.hasLoaded.value)
                        return

//                    if (scrollState == 0) { //scrolling stopped
//                        binding.appListDone.show()
//                    } else //scrolling
//                        binding.appListDone.hide()
                }
            })
        }

        collectLatestLifecycleFlow(viewModel.appList.filter { it.isNotEmpty() }) {
            adapter.submitList(it)

//            (view.parent as? ViewGroup)?.doOnPreDraw {
//                startPostponedEnterTransition()
//            }
        }

        collectLatestLifecycleFlow(viewModel.hasLoaded) {
            if (!it) return@collectLatestLifecycleFlow
            val fabData = FabData(
                viewLifecycleOwner,
                com.google.android.material.R.string.abc_action_mode_done,
                R.drawable.vd_check_simple,
                {
                    findNavController().navigateUp()
                },
                {
                    if (allowedPackagesArg == null) {
                        prefs.blockedPackages = setOf()
                        requireContext().toast(R.string.cleared_disabled_apps)
                    }
                    true
                }
            )

            mainNotifierViewModel.setFabData(fabData)

            if (allowedPackagesArg == null && !prefs.appListWasRun) {
                prefs.allowedPackages = viewModel.selectedPackages
            }

            binding.progress.hide()
        }

//        if (viewModel.appList.value == null)
//            viewModel.load(checkDefaultApps = allowedPackagesArg == null && !prefs.appListWasRun)
    }

    override fun onStop() {
        if (allowedPackagesArg == null) {
            prefs.appListWasRun = true
        }

        super.onStop()
    }

    private fun saveData() {
        if (!viewModel.hasLoaded.value) return

        if (allowedPackagesArg == null) {
            prefs.allowedPackages = viewModel.selectedPackages
            //BL = old WL - new WL
            prefs.blockedPackages =
                prefs.blockedPackages + prefs.allowedPackages - viewModel.selectedPackages
        } else {
            setFragmentResult(
                Stuff.ARG_ALLOWED_PACKAGES,
                bundleOf(Stuff.ARG_ALLOWED_PACKAGES to viewModel.selectedPackages.toTypedArray())
            )
        }
    }

}