package com.arn.scrobble

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.charts.ChartsVM
import com.arn.scrobble.databinding.ContentSearchBinding
import com.arn.scrobble.info.InfoFragment
import com.arn.scrobble.pref.MultiPreferences
import com.arn.scrobble.ui.ItemClickListener
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track


class SearchFragment: Fragment() {
    private val pref by lazy { MultiPreferences(context!!) }
    private val viewModel by lazy { VMFactory.getVM(this, SearchVM::class.java) }
    private val chartsVM by lazy { VMFactory.getVM(this, ChartsVM::class.java) }
    private var _binding: ContentSearchBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        binding.searchTerm.editText!!.requestFocus()
        binding.searchTerm.postDelayed({
            imm?.showSoftInput(binding.searchTerm.editText, 0)
        }, 100)
        binding.searchTerm.editText!!.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadSearches(textView.text.toString())
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                textView.clearFocus()
                true
            } else
                false
        }

        binding.searchTerm.editText!!.setOnClickListener {
            binding.searchResultsList.visibility = View.GONE
            binding.searchHistoryList.visibility = View.VISIBLE
        }

        loadHistory()

        val historyAdapter = SearchHistoryAdapter(binding)
        historyAdapter.viewModel = viewModel
        val historyItemClickListener = object : ItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                binding.searchTerm.clearFocus()
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                val term = viewModel.history[viewModel.history.size - position - 1]
                binding.searchTerm.editText?.setText(term)
                binding.searchTerm.editText?.setSelection(binding.searchTerm.editText!!.text.toString().length)
                loadSearches(term)
            }
        }
        historyAdapter.clickListener = historyItemClickListener
        binding.searchHistoryList.adapter = historyAdapter
        binding.searchHistoryList.layoutManager = LinearLayoutManager(context)
        historyAdapter.populate()

        val resultsAdapter = SearchResultsAdapter(binding)
        resultsAdapter.chartsVM = chartsVM
        val resultsItemClickListener = object : ItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val item = resultsAdapter.getItem(position)
                if (item is Pair<*, *> && viewModel.searchResults.value != null) {
                    if (resultsAdapter.expandType != item.first as Int)
                        resultsAdapter.populate(viewModel.searchResults.value!!, item.first as Int, true)
                    else
                        resultsAdapter.populate(viewModel.searchResults.value!!, -1, true)
                } else {
                    when (item) {
                        is Artist -> {
                            val info = InfoFragment()
                            val b = Bundle()
                            b.putString(NLService.B_ARTIST, item.name)
                            info.arguments = b
                            info.show(activity!!.supportFragmentManager, null)
                        }
                        is Album -> {
                            val info = InfoFragment()
                            val b = Bundle()
                            b.putString(NLService.B_ARTIST, item.artist)
                            b.putString(NLService.B_ALBUM, item.name)
                            info.arguments = b
                            info.show(activity!!.supportFragmentManager, null)
                        }
                        is Track -> {
                            val info = InfoFragment()
                            val b = Bundle()
                            b.putString(NLService.B_ARTIST, item.artist)
                            b.putString(NLService.B_ALBUM, item.album)
                            b.putString(NLService.B_TITLE, item.name)
                            info.arguments = b
                            info.show(activity!!.supportFragmentManager, null)
                        }
                    }
                }
            }
        }

        val touchListener = View.OnTouchListener { p0, p1 ->
            if (binding.searchTerm.editText!!.isFocused) {
                binding.searchTerm.clearFocus()
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
            }
            false
        }

        binding.searchResultsList.setOnTouchListener(touchListener)
        binding.searchHistoryList.setOnTouchListener(touchListener)

        resultsAdapter.clickListener = resultsItemClickListener
        binding.searchResultsList.adapter = resultsAdapter
        binding.searchResultsList.layoutManager = LinearLayoutManager(context)
        (binding.searchResultsList.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations = false

        viewModel.searchResults.observe(viewLifecycleOwner) {
            it ?: return@observe
            if (!(it.artists.isEmpty() && it.albums.isEmpty() && it.tracks.isEmpty())) {
                viewModel.history.remove(it.term)
                viewModel.history += it.term
                historyAdapter.notifyDataSetChanged()
            }
            resultsAdapter.populate(it, -1, false)
        }

        chartsVM.info.observe(viewLifecycleOwner, {
            it ?: return@observe
            val imgUrl = when (val entry = it.second) {
                is Artist -> entry.getImageURL(ImageSize.EXTRALARGE) ?: ""
                is Album -> entry.getWebpImageURL(ImageSize.LARGE) ?: ""
                is Track -> entry.getWebpImageURL(ImageSize.LARGE) ?: ""
                else -> ""
            }
            resultsAdapter.setImg(it.first, imgUrl)
            chartsVM.removeInfoTask(it.first)
        })
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, getString(R.string.search))
    }

    override fun onStop() {
        saveHistory()
        super.onStop()
    }

    private fun loadSearches(term: String) {
        binding.searchHistoryList.visibility = View.GONE
        binding.searchResultsList.visibility = View.GONE
        binding.searchProgress.visibility = View.VISIBLE
        viewModel.loadSearches(term)
        chartsVM.imgMap.clear()
    }

    private fun loadHistory() {
        val historySet = pref.getStringSet(Stuff.PREF_ACTIVITY_SEARCH_HISTORY, setOf())
        val historyList = mutableListOf<Pair<Int,String>>()
        historySet.forEach {
            val parts = it.split('\n')
            historyList += parts[0].toInt() to parts[1]
        }
        historyList.sortBy { it.first }
        viewModel.history.clear()
        historyList.forEach {
            viewModel.history += it.second
        }
    }

    private fun saveHistory() {
        val historyPrefsSet = mutableSetOf<String>()
        viewModel.history.takeLast(7).forEachIndexed { i, it ->
            historyPrefsSet += "" + i + "\n" + it.replace(',', ' ')
        }
        pref.putStringSet(Stuff.PREF_ACTIVITY_SEARCH_HISTORY, historyPrefsSet)
    }
}