package com.arn.scrobble.pref

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentAppListBinding
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.transition.MaterialSharedAxis


/**
 * Created by arn on 05/09/2017.
 */
class AppListFragment : Fragment() {
    private val mainNotifierViewModel by lazy { (activity as MainActivity).mainNotifierViewModel }
    private val prefs by lazy { MainPrefs(context!!) }
    private val viewModel by viewModels<AppListVM>()
    private var _binding: ContentAppListBinding? = null
    private val binding
        get() = _binding!!

    private val allowedPackagesArg
        get() = arguments?.getStringArray(Stuff.ARG_ALLOWED_PACKAGES)?.toSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
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
        mainNotifierViewModel.backButtonEnabled = true
        saveData()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.selectedPackages += allowedPackagesArg ?: prefs.allowedPackages

        if (!binding.appList.isInTouchMode)
            binding.appList.requestFocus()

        if (Stuff.isTv) {
            context!!.toast(R.string.press_back)
        }

        binding.appList.layoutManager = LinearLayoutManager(context)
        val adapter = AppListAdapter(activity!!, viewModel)
        binding.appList.adapter = adapter
        if (!Stuff.isTv) {
            binding.appList.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrollStateChanged(view: RecyclerView, scrollState: Int) {
                    if (!mainNotifierViewModel.backButtonEnabled)
                        return

                    if (scrollState == 0) { //scrolling stopped
                        binding.appListDone.show()
                    } else //scrolling
                        binding.appListDone.hide()
                }
            })

            binding.appListDone.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            if (allowedPackagesArg == null) {
                binding.appListDone.setOnLongClickListener {
                    prefs.blockedPackages = setOf()
                    context!!.toast(R.string.cleared_disabled_apps)
                    true
                }
            }
        }

        viewModel.data.observe(viewLifecycleOwner) {
            it ?: return@observe
            adapter.populate(it)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            it ?: return@observe
            mainNotifierViewModel.backButtonEnabled = !it

            if (!it) {
                if (!Stuff.isTv) {
                    binding.appListDone.show()
                }

                if (allowedPackagesArg == null && !prefs.appListWasRun) {
                    prefs.allowedPackages = viewModel.selectedPackages
                }
            }
        }

        if (viewModel.data.value == null)
            viewModel.load(checkDefaultApps = allowedPackagesArg == null && !prefs.appListWasRun)
    }

    override fun onStart() {
        super.onStart()
        setTitle(R.string.enabled_apps)
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
            setFragmentResult(Stuff.ARG_ALLOWED_PACKAGES, Bundle().apply {
                putStringArray(
                    Stuff.ARG_ALLOWED_PACKAGES,
                    viewModel.selectedPackages.toTypedArray()
                )
            })
        }
    }

}