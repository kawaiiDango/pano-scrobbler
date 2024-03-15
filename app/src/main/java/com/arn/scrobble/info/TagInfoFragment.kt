package com.arn.scrobble.info

import android.app.Dialog
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
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.databinding.ContentTagInfoBinding
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.expandIfNeeded
import com.arn.scrobble.utils.UiUtils.scheduleTransition
import com.arn.scrobble.utils.UiUtils.startFadeLoop
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.copyToClipboard
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.getData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.filterNotNull
import java.net.URLEncoder


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

        val tag = requireArguments().getData<Tag>()!!
        binding.tagInfoTitle.text = tag.name

        binding.tagInfoTitle.setOnLongClickListener {
            requireContext().copyToClipboard(binding.tagInfoTitle.text.toString())
            true
        }

        binding.tagInfoLink.setOnClickListener {
            Stuff.openInBrowser(
                "https://www.last.fm/tag/" + URLEncoder.encode(tag.name, "UTF-8")
            )
        }
        binding.root.startFadeLoop()

        collectLatestLifecycleFlow(viewModel.info.filterNotNull()) {
            binding.root.clearAnimation()
            scheduleTransition()

            binding.tagInfoContent.visibility = View.VISIBLE

            binding.tagInfoTaggers.text = it.reach?.format()
            binding.tagInfoTaggings.text = it.count?.format()

            var wikiText = it.wiki?.content
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
        }

        viewModel.loadInfoIfNeeded(tag)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            expandIfNeeded(it)
        }
    }
}