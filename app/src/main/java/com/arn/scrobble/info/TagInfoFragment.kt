package com.arn.scrobble.info

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Layout
import android.text.Spanned
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.copyToClipboard
import com.arn.scrobble.databinding.ContentTagInfoBinding
import com.arn.scrobble.ui.UiUtils.expandIfNeeded
import com.arn.scrobble.ui.UiUtils.scheduleTransition
import com.arn.scrobble.ui.UiUtils.startFadeLoop
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.net.URLEncoder
import java.text.NumberFormat


class TagInfoFragment : BottomSheetDialogFragment() {

    private val viewModel by viewModels<TagInfoVM>()
    private var _binding: ContentTagInfoBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentTagInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tag = requireArguments().getString(Stuff.ARG_TAG)!!
        binding.tagInfoTitle.text = tag

        binding.tagInfoTitle.setOnLongClickListener {
            requireContext().copyToClipboard(binding.tagInfoTitle.text.toString())
            true
        }

        binding.tagInfoLink.setOnClickListener {
            Stuff.openInBrowser(
                "https://www.last.fm/tag/" + URLEncoder.encode(tag, "UTF-8")
            )
        }
        binding.root.startFadeLoop()

        viewModel.info.observe(viewLifecycleOwner) {
            it ?: return@observe
            val tagInfo = it.first ?: return@observe
            val similarTags = it.second

            binding.root.clearAnimation()
            scheduleTransition()

            binding.tagInfoContent.visibility = View.VISIBLE

            binding.tagInfoTaggers.text = NumberFormat.getInstance().format(tagInfo.reach)
            binding.tagInfoTaggings.text = NumberFormat.getInstance().format(tagInfo.count)

            var wikiText = tagInfo.wikiText ?: tagInfo.wikiSummary
            if (!wikiText.isNullOrBlank()) {
                var idx = wikiText.indexOf("<a href=\"http://www.last.fm")
                if (idx == -1)
                    idx = wikiText.indexOf("<a href=\"https://www.last.fm")
                if (idx != -1)
                    wikiText = wikiText.substring(0, idx).trim()
                if (wikiText.isNotBlank()) {
                    wikiText = wikiText.replace("\n", "<br>")
                    binding.tagInfoWikiContainer.visibility = View.VISIBLE
                    binding.tagInfoWiki.text = Html.fromHtml(wikiText)

                    val urls = (binding.tagInfoWiki.text as? Spanned)?.getSpans(
                        0,
                        binding.tagInfoWiki.text.length,
                        URLSpan::class.java
                    )
                    if (urls.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        binding.tagInfoWiki.justificationMode = Layout.JUSTIFICATION_MODE_INTER_WORD

                    binding.tagInfoWiki.post {
                        if (_binding == null || binding.tagInfoWiki.layout == null)
                            return@post
                        if (binding.tagInfoWiki.lineCount > 4 ||
                            binding.tagInfoWiki.layout.getEllipsisCount(binding.tagInfoWiki.lineCount - 1) > 0
                        ) {
                            val clickListener = { view: View ->
                                if (!(view is TextView && (view.selectionStart != -1 || view.selectionEnd != -1))) {

                                    scheduleTransition()

                                    if (binding.tagInfoWiki.maxLines == 4) {
                                        binding.tagInfoWiki.maxLines = 1000
                                        binding.tagInfoWikiExpand.rotation = 180f
                                    } else {
                                        binding.tagInfoWiki.maxLines = 4
                                        binding.tagInfoWikiExpand.rotation = 0f
                                    }
                                }
                            }
                            binding.tagInfoWiki.setOnClickListener(clickListener)
                            binding.tagInfoWikiExpand.setOnClickListener(clickListener)
                            binding.tagInfoWikiExpand.visibility = View.VISIBLE
                        }
                    }
                }
            }
/*
            if (!similarTags.isNullOrEmpty()) {
                binding.tagInfoSimilarTitle.visibility = View.VISIBLE
                binding.tagInfoTags.removeAllViews()
                similarTags.forEach {
                    val chip = Chip(requireContext())
                    chip.text = it.name
                    chip.setOnClickListener { _ ->
                        val tif = TagInfoFragment()
                        val b = Bundle()
                        b.putString(Stuff.ARG_TAG, it.name)
                        tif.arguments = b
                        tif.show(parentFragmentManager, null)
                    }
                    binding.tagInfoTags.addView(chip)
                }
            }

 */
        }

        if (viewModel.info.value == null)
            viewModel.loadInfo(tag)
    }

    override fun onStart() {
        super.onStart()
        expandIfNeeded()
    }
}