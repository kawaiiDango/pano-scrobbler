package com.arn.scrobble.search

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Build
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.databinding.ContentSearchBinding
import com.arn.scrobble.info.InfoFragment
import com.arn.scrobble.pref.HistoryPref
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.UiUtils.hideKeyboard
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.showKeyboard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialSharedAxis
import de.umass.lastfm.MusicEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SearchFragment : Fragment() {
    private val viewModel by viewModels<SearchVM>()
    private val historyPref by lazy {
        HistoryPref(
            MainPrefs(context!!).sharedPreferences,
            MainPrefs.PREF_ACTIVITY_SEARCH_HISTORY,
            20
        )
    }
    private val prefs by lazy { MainPrefs(context!!) }
    private var _binding: ContentSearchBinding? = null
    private val binding
        get() = _binding!!

    private val autoSubmitDelay = 1500L
    private var lastTimerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentSearchBinding.inflate(inflater, container, false)
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

        binding.searchTerm.editText!!.requestFocus()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(100)
            showKeyboard(binding.searchTerm.editText!!)
            delay(400)
            binding.searchEdittext.showDropDown()
        }

        binding.searchTerm.editText!!.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)
            ) {
//                loadSearches(textView.text.toString())
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
//                    binding.searchTerm.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
//                    bug https://github.com/material-components/material-components-android/issues/503
                    binding.searchTerm.endIconMode = TextInputLayout.END_ICON_CUSTOM
                    binding.searchTerm.setEndIconDrawable(R.drawable.vd_cancel)
                    binding.searchTerm.setEndIconOnClickListener { binding.searchTerm.editText?.text?.clear() }

                    lastTimerJob?.cancel()
                    lastTimerJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(autoSubmitDelay)
                        loadSearches(term)
                    }

                } else {
                    binding.searchTerm.endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
                }
            }

        })

        historyPref.load()

        val arrayAdapter = object : ArrayAdapter<String>(context!!, R.layout.list_item_history) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val historyTextView = super.getView(position, convertView, parent)
                if (convertView == null) {
                    historyTextView.setOnClickListener {
                        val item = getItem(position)!!
                        hideKeyboard()
                        binding.searchEdittext.setText(item, false)
                        binding.searchTerm.clearFocus()
//            val term = arrayAdapter.getItem(pos) as String
//            loadSearches(term)
                    }
                    historyTextView.setOnLongClickListener {
                        MaterialAlertDialogBuilder(context)
                            .setMessage(R.string.clear_history_specific)
                            .setPositiveButton(R.string.yes) { dialogInterface, i ->
                                val item = getItem(position)!!
                                historyPref.remove(item)
                                remove(item)
                                notifyDataSetChanged()
                            }
                            .setNegativeButton(R.string.no, null)
                            .setNeutralButton(R.string.clear_all_history) { dialogInterface, i ->
                                historyPref.removeAll()
                                clear()
                                notifyDataSetChanged()
                            }
                            .show()
                        false
                    }
                }
                return historyTextView
            }
        }
        arrayAdapter.addAll(historyPref.history)
        binding.searchEdittext.setAdapter(arrayAdapter)
//        binding.searchEdittext.setOnItemClickListener { adapterView, v, pos, l ->
//            hideKeyboard()
//            binding.searchTerm.clearFocus()
////            val term = arrayAdapter.getItem(pos) as String
////            loadSearches(term)
//        }

        val resultsItemClickListener = object : MusicEntryItemClickListener {
            override fun onItemClick(view: View, entry: MusicEntry) {
                val info = InfoFragment()
                info.arguments = entry.toBundle()
                info.show(activity!!.supportFragmentManager, null)
            }
        }
        val resultsAdapter =
            SearchResultsAdapter(context!!, viewModel, resultsItemClickListener)

        val touchListener = View.OnTouchListener { p0, p1 ->
            if (binding.searchTerm.editText!!.isFocused) {
                hideKeyboard()
                binding.searchTerm.clearFocus()
            }
            false
        }

        binding.searchResultsList.setOnTouchListener(touchListener)

        binding.searchResultsList.adapter = resultsAdapter
        binding.searchResultsList.layoutManager = LinearLayoutManager(context)
        binding.searchResultsList.itemAnimator?.changeDuration = 0

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
                loadSearches(searchTerm)
            }
        }

        viewModel.searchResults.observe(viewLifecycleOwner) {
            it ?: return@observe
            if (!it.isEmpty) {
                historyPref.add(it.term)
                arrayAdapter.remove(it.term)
                arrayAdapter.insert(it.term, 0)
                arrayAdapter.notifyDataSetChanged()
            }
            binding.searchProgress.isIndeterminate = true
            binding.searchProgress.hide()
            binding.searchResultsList.visibility = View.VISIBLE

            resultsAdapter.populate(it)

            binding.searchResultsList.scheduleLayoutAnimation()
        }

        viewModel.indexingProgress.observe(viewLifecycleOwner) {
            it ?: return@observe
            val progressInt = (binding.searchProgress.max * it).toInt()
            binding.searchResultsList.visibility = View.GONE
            binding.searchProgress.isIndeterminate = progressInt == 0
            if (progressInt == 0)
                binding.searchProgress.show()
            ObjectAnimator.ofInt(binding.searchProgress, "progress", progressInt).apply {
                duration = 150
                start()
            }
            if (progressInt == binding.searchProgress.max && !binding.searchEdittext.text.isNullOrBlank()) {
                viewModel.loadSearches(binding.searchEdittext.text.toString(), prefs.searchType)
                // prevent progress bar from showing
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setTitle(R.string.search)
    }

    override fun onStop() {
        historyPref.save()
        super.onStop()
    }

    private fun loadSearches(term: String) {
        binding.searchResultsList.visibility = View.GONE
        binding.searchProgress.show()
        if (prefs.searchType == SearchResultsAdapter.SearchType.LOCAL && prefs.lastMaxIndexTime == null)
            viewModel.fullIndex()
        else
            viewModel.loadSearches(term, prefs.searchType)
    }
}