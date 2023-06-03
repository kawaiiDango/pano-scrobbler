package com.arn.scrobble.pref

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
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
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentAppListBinding
import com.arn.scrobble.ui.FabData
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.transition.MaterialSharedAxis


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
    private val backPressedCallback by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)
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
        binding.appList.setupInsets()
//        binding.appListDone.setupInsets()

        viewModel.selectedPackages += allowedPackagesArg ?: prefs.allowedPackages

        if (!binding.appList.isInTouchMode)
            binding.appList.requestFocus()

        if (Stuff.isTv) {
            requireContext().toast(R.string.press_back)
        }

        binding.appList.layoutManager = LinearLayoutManager(context)
        val adapter = AppListAdapter(requireActivity(), viewModel)
        binding.appList.adapter = adapter
        if (!Stuff.isTv) {
            binding.appList.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrollStateChanged(view: RecyclerView, scrollState: Int) {
                    if (viewModel.isLoading.value == true)
                        return

//                    if (scrollState == 0) { //scrolling stopped
//                        binding.appListDone.show()
//                    } else //scrolling
//                        binding.appListDone.hide()
                }
            })
        }

        viewModel.data.observe(viewLifecycleOwner) {
            it ?: return@observe
            adapter.populate(it)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            it ?: return@observe

            if (!it) {
                backPressedCallback.isEnabled = false

                mainNotifierViewModel.fabData.value = FabData(
                    viewLifecycleOwner,
                    com.google.android.material.R.string.abc_action_mode_done,
                    R.drawable.vd_check_simple,
                    {
                        findNavController().popBackStack()
                    },
                    {
                        if (allowedPackagesArg == null) {
                            prefs.blockedPackages = setOf()
                            requireContext().toast(R.string.cleared_disabled_apps)
                        }
                        true
                    }
                )

                if (allowedPackagesArg == null && !prefs.appListWasRun) {
                    prefs.allowedPackages = viewModel.selectedPackages
                }
            }
        }

        if (viewModel.data.value == null)
            viewModel.load(checkDefaultApps = allowedPackagesArg == null && !prefs.appListWasRun)
    }

    override fun onStop() {
        if (allowedPackagesArg == null) {
            prefs.appListWasRun = true
        }

        super.onStop()
    }

    private fun saveData() {
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