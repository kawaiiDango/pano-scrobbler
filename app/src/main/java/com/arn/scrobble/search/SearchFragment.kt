package com.arn.scrobble.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.databinding.ContentSearchBinding
import com.arn.scrobble.pref.HistoryPref
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.ui.UiUtils.hideKeyboard
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.UiUtils.showKeyboard
import com.arn.scrobble.utils.Stuff.putData
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private var skeletonJob: Job? = null
    private lateinit var skeleton: Skeleton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAxisTransitions(MaterialSharedAxis.Y)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        if (!BuildConfig.DEBUG) {
            binding.searchType.visibility = View.GONE
        }

        if (savedInstanceState == null) {
            binding.searchEdittext.requestFocus()
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                showKeyboard(binding.searchTerm.editText!!)
                delay(400)
                binding.searchEdittext.showDropDown()
            }
        }

        binding.searchTerm.editText!!.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)
            ) {
                doSearch(textView.text.toString())
                hideKeyboard()
                textView.clearFocus()
                true
            } else
                false
        }

        binding.searchTerm.editText!!.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun afterTextChanged(editable: Editable) {
                val term = editable.trim().toString()
                if (term.isNotEmpty()) {
                    binding.searchTerm.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                    binding.searchTerm.setEndIconDrawable(R.drawable.vd_cancel)

                    viewModel.search(term, prefs.searchType)
                } else {
                    binding.searchTerm.endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
                }
            }

        })

        historyPref.load()

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
                            doSearch(term)
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

        skeleton = binding.searchResultsList.applySkeleton(
            R.layout.list_item_recents_skeleton,
            10,
            UiUtils.mySkeletonConfig(requireContext())
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

            val searchTerm = binding.searchTerm.editText?.text?.toString()
            if (checkedChipId != null && !searchTerm.isNullOrBlank()) {
                doSearch(searchTerm)
            }
        }

        collectLatestLifecycleFlow(viewModel.searchResults) {
            skeletonJob?.cancel()
            if (skeleton.isSkeleton())
                skeleton.showOriginal()

            resultsAdapter.populate(it)

            binding.searchResultsList.scheduleLayoutAnimation()
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

    private fun doSearch(term: String) {

        if (prefs.searchType == SearchResultsAdapter.SearchType.LOCAL && prefs.lastMaxIndexTime == null)
            findNavController().navigate(R.id.indexingDialogFragment)
        else {
            skeletonJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                skeleton.showSkeleton()
            }
            viewModel.search(term, prefs.searchType)
        }
    }
}