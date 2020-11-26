package com.arn.scrobble.info

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.VMFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.content_info.*
import kotlinx.android.synthetic.main.content_tag_info.*
import java.net.URLEncoder
import java.text.NumberFormat


class TagInfoFragment: BottomSheetDialogFragment() {

    val viewModel by lazy { VMFactory.getVM(this, TagInfoVM::class.java) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val width = resources.getDimension(R.dimen.bottom_sheet_width)
            if (width > 0)
                dialog.window!!.setLayout(width.toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_tag_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tag = arguments!!.getString(Stuff.ARG_TAG)!!
        tag_info_title.text = tag
        tag_info_link.setOnClickListener { Stuff.openInBrowser("https://www.last.fm/tag/" +
                URLEncoder.encode(tag, "UTF-8")
                , context!!) }

        viewModel.info.observe(viewLifecycleOwner) {
            it ?: return@observe
            val tagInfo = it.first
            val similarTags = it.second

            tag_info_progress.visibility = View.GONE
            tag_info_content.visibility = View.VISIBLE

            tag_info_taggers.text = NumberFormat.getInstance().format(tagInfo.reach)
            tag_info_taggings.text = NumberFormat.getInstance().format(tagInfo.count)

            var wikiText = tagInfo.wikiText ?: tagInfo.wikiSummary
            if (!wikiText.isNullOrBlank()) {
                var idx = wikiText.indexOf("<a href=\"http://www.last.fm")
                if (idx == -1)
                    idx = wikiText.indexOf("<a href=\"https://www.last.fm")
                if (idx != -1)
                    wikiText = wikiText.substring(0, idx).trim()
                if (!wikiText.isNullOrBlank()) {
                    wikiText = wikiText.replace("\n", "<br>")
                    tag_info_wiki.visibility = View.VISIBLE
                    tag_info_wiki.text = Html.fromHtml(wikiText)
                    tag_info_wiki.post{
                        if (tag_info_wiki == null || tag_info_wiki.layout == null)
                            return@post
                        if (tag_info_wiki.lineCount > 4 ||
                                tag_info_wiki.layout.getEllipsisCount(tag_info_wiki.lineCount - 1) > 0) {
                            val clickListener = { view: View ->
                                if (!(view is TextView && (view.selectionStart != -1 || view.selectionEnd != -1))) {
                                    if (tag_info_wiki.maxLines == 4) {
                                        tag_info_wiki.maxLines = 1000
                                        tag_info_wiki_expand.rotation = 180f
                                    } else {
                                        tag_info_wiki.maxLines = 4
                                        tag_info_wiki_expand.rotation = 0f
                                    }
                                }
                            }
                            tag_info_wiki.setOnClickListener(clickListener)
                            tag_info_wiki_expand.setOnClickListener(clickListener)
                            tag_info_wiki_expand.visibility = View.VISIBLE
                        }
                    }
                }
            }

            if (!similarTags.isNullOrEmpty()) {
                tag_info_similar_title.visibility = View.VISIBLE
                tag_info_tags.removeAllViews()
                similarTags.forEach {
                    val chip = Chip(context!!)
                    chip.text = it.name
                    chip.setOnClickListener { _ ->
                        val tif = TagInfoFragment()
                        val b = Bundle()
                        b.putString(Stuff.ARG_TAG, it.name)
                        tif.arguments = b
                        tif.show(parentFragmentManager, null)
                    }
                    tag_info_tags.addView(chip)
                }
            }
        }

        if (viewModel.info.value == null)
            viewModel.loadInfo(tag)
    }

    override fun onStart() {
        super.onStart()
        if (view?.isInTouchMode == false) {
            val bottomSheetView = dialog!!.window!!.decorView.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheetView).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
}