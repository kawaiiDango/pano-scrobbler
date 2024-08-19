package com.arn.scrobble.search

import android.annotation.SuppressLint
import android.app.SearchManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.databinding.ContentSearchBinding
import com.arn.scrobble.main.App
import com.arn.scrobble.pref.HistoryPref
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.createSkeletonWithFade
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putData
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.hideKeyboard
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.arn.scrobble.utils.UiUtils.showKeyboard
import com.faltenreich.skeletonlayout.Skeleton
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch


class SearchFragment : Fragment() {
    private val viewModel by viewModels<SearchVM>()
    private val historyPref by lazy {
        HistoryPref(
            App.prefs.sharedPreferences,
            MainPrefs.PREF_ACTIVITY_SEARCH_HISTORY,
            20
        )
    }
    private val prefs = App.prefs
    private var _binding: ContentSearchBinding? = null
    private val binding
        get() = _binding!!
    private lateinit var skeleton: Skeleton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.Y)

        _binding = ContentSearchBinding.inflate(inflater, container, false)
        binding.searchResultsList.setupInsets()
        return binding.root
    }

    override fun onDestroyView() {
        hideKeyboard()
        _binding = null
        super.onDestroyView()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val queryFromIntent = arguments?.getString(SearchManager.QUERY)?.ifEmpty { null }

        if (!BuildConfig.DEBUG) {
            binding.searchType.visibility = View.GONE
        }

        if (queryFromIntent == null) {
            if (savedInstanceState == null) {
                binding.searchEdittext.requestFocus()
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(400)
                    showKeyboard(binding.searchTerm.editText!!)
                }
            }

            binding.searchEdittext.setOnClickListener { v ->
                if (Stuff.isTv)
                    showKeyboard(v)
            }
        }

        binding.searchTerm.editText!!.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)
            ) {
                viewModel.search(textView.text.toString(), prefs.searchType)
                hideKeyboard()
                textView.clearFocus()
            }
            true
        }

        binding.searchTerm.editText!!.doAfterTextChanged { editable ->
            val term = editable?.trim()?.toString()
            if (!term.isNullOrEmpty()) {
                viewModel.search(term, prefs.searchType)
            }
        }

        historyPref.load()
        /*

        val searchHistoryAdapter =
            object : ArrayAdapter<String>(requireContext(), R.layout.list_item_history) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val historyTextView = super.getView(position, convertView, parent)
                    if (convertView == null) {
                        historyTextView.setOnClickListener {
                            val term = getItem(position)
                            hideKeyboard()
                            binding.searchEdittext.setText(term, false)
                            binding.searchTerm.clearFocus()
                            viewModel.search(term, prefs.searchType)
                        }
                        historyTextView.setOnLongClickListener {
                            MaterialAlertDialogBuilder(context)
                                .setMessage(R.string.clear_history_specific)
                                .setPositiveButton(R.string.yes) { dialogInterface, i ->
                                    val item = getItem(position)
                                    historyPref.remove(item)
                                }
                                .setNegativeButton(R.string.no, null)
                                .setNeutralButton(R.string.clear_all_history) { dialogInterface, i ->
                                    historyPref.removeAll()
                                }
                                .show()
                            false
                        }
                    }
                    return historyTextView
                }

                override fun getItem(position: Int) = historyPref.history[position]

                override fun getCount() = historyPref.history.size
            }

        binding.searchEdittext.setAdapter(searchHistoryAdapter)

        */

        binding.searchEdittext.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                updateSearchHistory()
            }
        }

        val resultsItemClickListener = object : MusicEntryItemClickListener {
            override fun onItemClick(view: View, entry: MusicEntry) {
                val args = Bundle().putData(entry)
                findNavController().navigate(R.id.infoFragment, args)
            }
        }
        val resultsAdapter =
            SearchResultsAdapter(requireContext(), viewModel, resultsItemClickListener)

        binding.searchResultsList.adapter = resultsAdapter
        binding.searchResultsList.layoutManager = LinearLayoutManager(context)
        binding.searchResultsList.itemAnimator?.changeDuration = 0

        skeleton = binding.searchResultsList.createSkeletonWithFade(
            R.layout.list_item_recents_skeleton,
        )

        val checkedChip = when (prefs.searchType) {
            SearchResultsAdapter.SearchType.LOCAL -> binding.searchLibrary
            else -> binding.searchGlobal
        }
        checkedChip.isChecked = true

        binding.searchType.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedChipId = checkedIds.firstOrNull()
            when (checkedChipId) {
                R.id.search_library -> prefs.searchType =
                    SearchResultsAdapter.SearchType.LOCAL

                R.id.search_global -> prefs.searchType =
                    SearchResultsAdapter.SearchType.GLOBAL
            }

            if (prefs.searchType == SearchResultsAdapter.SearchType.LOCAL && prefs.lastMaxIndexTime == null) {
                findNavController().navigate(R.id.indexingDialogFragment)
                return@setOnCheckedStateChangeListener
            }

            val searchTerm = binding.searchTerm.editText?.text?.toString()
            if (checkedChipId != null && !searchTerm.isNullOrBlank()) {
                viewModel.search(searchTerm, prefs.searchType)
            }
        }

        collectLatestLifecycleFlow(viewModel.searchResults.filterNotNull()) {
            if (it.isEmpty) {
                binding.searchResultsList.isVisible = false
                binding.empty.isVisible = true
            } else {
                if (isResumed)
                    binding.searchResultsList.scheduleLayoutAnimation()

                binding.searchResultsList.isVisible = true
                binding.empty.isVisible = false

                resultsAdapter.populate(it)
            }
        }

        collectLatestLifecycleFlow(viewModel.hasLoaded) {
            if (binding.searchEdittext.text.isNullOrEmpty())
                return@collectLatestLifecycleFlow

            if (it) {
                skeleton.showOriginal()
            } else {
                binding.empty.isVisible = false
                skeleton.showSkeleton()
            }
        }

        if (queryFromIntent != null) {
            binding.searchTerm.editText!!.setText(queryFromIntent)
            viewModel.search(queryFromIntent, prefs.searchType)
        }
    }

    private fun updateSearchHistory() {
        val term = binding.searchEdittext.text.toString()
        if (term.isNotEmpty()) {
            historyPref.add(term)
        }
    }

    override fun onStop() {
        updateSearchHistory()
        historyPref.save()
        super.onStop()
    }
}