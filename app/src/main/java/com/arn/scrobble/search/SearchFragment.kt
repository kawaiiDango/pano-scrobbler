package com.arn.scrobble.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.hideKeyboard
import com.arn.scrobble.Stuff.showKeyboard
import com.arn.scrobble.VMFactory
import com.arn.scrobble.databinding.ContentSearchBinding
import com.arn.scrobble.info.InfoFragment
import com.arn.scrobble.pref.HistoryPref
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.ItemClickListener
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialSharedAxis
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SearchFragment : Fragment() {
    private val viewModel by lazy { VMFactory.getVM(this, SearchVM::class.java) }
    private val historyPref by lazy {
        HistoryPref(
            MainPrefs(context!!).sharedPreferences,
            MainPrefs.PREF_ACTIVITY_SEARCH_HISTORY,
            20
        )
    }
    private var _binding: ContentSearchBinding? = null
    private val binding
        get() = _binding!!

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                loadSearches(textView.text.toString())
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
                if (editable.isNotEmpty()) {
//                    binding.searchTerm.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
//                    bug https://github.com/material-components/material-components-android/issues/503
                    binding.searchTerm.endIconMode = TextInputLayout.END_ICON_CUSTOM
                    binding.searchTerm.setEndIconDrawable(R.drawable.vd_cancel)
                    binding.searchTerm.setEndIconOnClickListener { binding.searchTerm.editText?.text?.clear() }
                } else {
                    binding.searchTerm.endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
                }
            }

        })

        historyPref.load()

        val arrayAdapter = ArrayAdapter<String>(context!!, R.layout.list_item_history)
        arrayAdapter.addAll(historyPref.history)
        binding.searchEdittext.setAdapter(arrayAdapter)
        binding.searchEdittext.setOnItemClickListener { adapterView, v, pos, l ->
            hideKeyboard()
            binding.searchTerm.clearFocus()
            val term = arrayAdapter.getItem(pos) as String
            loadSearches(term)
        }

        val resultsAdapter = SearchResultsAdapter(binding)
        val resultsItemClickListener = object : ItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val item = resultsAdapter.getItem(position)
                if (item is Pair<*, *> && viewModel.searchResults.value != null) {
                    if (resultsAdapter.expandType != item.first as Int)
                        resultsAdapter.populate(
                            viewModel.searchResults.value!!,
                            item.first as Int,
                            true
                        )
                    else
                        resultsAdapter.populate(viewModel.searchResults.value!!, -1, true)
                } else {
                    when (item) {
                        is Artist -> {
                            val info = InfoFragment()
                            info.arguments = Bundle().apply {
                                putString(NLService.B_ARTIST, item.name)
                            }
                            info.show(activity!!.supportFragmentManager, null)
                        }
                        is Album -> {
                            val info = InfoFragment()
                            info.arguments = Bundle().apply {
                                putString(NLService.B_ARTIST, item.artist)
                                putString(NLService.B_ALBUM, item.name)
                            }
                            info.show(activity!!.supportFragmentManager, null)
                        }
                        is Track -> {
                            val info = InfoFragment()
                            info.arguments = Bundle().apply {
                                putString(NLService.B_ARTIST, item.artist)
                                putString(NLService.B_ALBUM, item.album)
                                putString(NLService.B_TRACK, item.name)
                            }
                            info.show(activity!!.supportFragmentManager, null)
                        }
                    }
                }
            }
        }

        val touchListener = View.OnTouchListener { p0, p1 ->
            if (binding.searchTerm.editText!!.isFocused) {
                hideKeyboard()
                binding.searchTerm.clearFocus()
            }
            false
        }

        binding.searchResultsList.setOnTouchListener(touchListener)

        resultsAdapter.clickListener = resultsItemClickListener
        binding.searchResultsList.adapter = resultsAdapter
        binding.searchResultsList.layoutManager = LinearLayoutManager(context)
        (binding.searchResultsList.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations =
            false

        viewModel.searchResults.observe(viewLifecycleOwner) {
            it ?: return@observe
            if (!(it.artists.isEmpty() && it.albums.isEmpty() && it.tracks.isEmpty())) {
                historyPref.add(it.term)
                arrayAdapter.remove(it.term)
                arrayAdapter.insert(it.term, 0)
                arrayAdapter.notifyDataSetChanged()
            }
            resultsAdapter.populate(it, -1, false)
        }

    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity!!, getString(R.string.search))
    }

    override fun onStop() {
        historyPref.save()
        super.onStop()
    }

    private fun loadSearches(term: String) {
        binding.searchResultsList.visibility = View.GONE
        binding.searchProgress.show()
        viewModel.loadSearches(term)
    }
}