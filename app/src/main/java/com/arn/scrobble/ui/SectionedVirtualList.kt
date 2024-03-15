package com.arn.scrobble.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.arn.scrobble.main.App
import com.arn.scrobble.R
import kotlin.math.min

// TODO: test for race conditions

class SectionedVirtualList : AbstractList<Any>() {

    private val sections = mutableMapOf<Enum<*>, SectionWithHeader>()

    override val size: Int
        get() = sections.values.sumOf { it.size }

    @Synchronized
    override fun get(index: Int): Any {
        var _index = index
        for ((_, section) in sections) {
            if (_index < section.size) {
                return section[_index]
            }
            _index -= section.size
        }
        throw IndexOutOfBoundsException("index: $index, size: $size")
    }

    fun clear() {
        sections.clear()
    }

    @Synchronized
    fun getItemType(index: Int): Int {
        var _index = index
        for ((_, section) in sections) {
            if (_index < section.size) {
                return section.getItemType(_index)
            }
            _index -= section.size
        }
        throw IndexOutOfBoundsException("index: $index, size: $size")
    }

    @Synchronized
    fun addSection(section: SectionWithHeader) {
        section.header?.section = section
        sections[section.sectionId] = section
    }

    @Synchronized
    fun removeSection(sectionId: Enum<*>) {
        sections.remove(sectionId)
    }

    operator fun get(sectionId: Enum<*>) = sections[sectionId]

    fun toggleOne(sectionId: Enum<*>) {
        val header = sections[sectionId]!!.header as ExpandableHeader

        header.toggle()

        sections.forEach { (id, section) ->
            if (section.header !is ExpandableHeader || id == sectionId) {
                return@forEach
            }
            section.header.isExpanded = false
        }
    }

    companion object {
        const val TYPE_HEADER_DEFAULT = -11
        const val TYPE_ITEM_DEFAULT = -10
    }

    fun copy(deep: Boolean = false): SectionedVirtualList {
        val copy = SectionedVirtualList()
        sections.forEach { (id, section) ->
            copy.addSection(section.copy(deep))
        }
        return copy
    }
}

class SectionWithHeader(
    val sectionId: Enum<*>,
    val items: List<Any>,
    val itemType: Int = SectionedVirtualList.TYPE_ITEM_DEFAULT,
    val header: ExpandableHeader? = null,
    val headerType: Int = SectionedVirtualList.TYPE_HEADER_DEFAULT,
    var showHeaderWhenEmpty: Boolean = false,
) : AbstractList<Any>() {

    private val isHeaderShown get() = header != null && (showHeaderWhenEmpty || items.isNotEmpty())

    fun getItemType(index: Int): Int {
        return if (index == 0 && isHeaderShown) {
            headerType
        } else {
            itemType
        }
    }

    override val size: Int
        get() {
            val expanded = header?.isExpanded ?: false
            val maxCollapsedItems =
                header?.maxCollapsedItems ?: Int.MAX_VALUE

            var s = 0
            if (isHeaderShown) s++
            s += if (expanded) items.size
            else
                min(maxCollapsedItems, items.size)
            return s
        }

    val listSize get() = items.size

    @Synchronized
    override fun get(index: Int): Any {
        var _index = index
        if (isHeaderShown) {
            if (index == 0) return header!!
            _index--
        }
        return items[_index]
    }

    fun copy(deep: Boolean = false): SectionWithHeader {
        val _items = if (deep) items.toList() else items
        return SectionWithHeader(
            sectionId,
            _items,
            itemType,
            header?.copy(),
            headerType,
            showHeaderWhenEmpty
        )
    }
}


data class ExpandableHeader(
    @DrawableRes
    val iconRes: Int,
    var title: String,
    var expandText: String = "",
    var collapseText: String = "",
    var isExpanded: Boolean = false,
    var maxCollapsedItems: Int = 3,
) {
    constructor(
        @DrawableRes
        iconRes: Int,
        @StringRes
        titleRes: Int,
        @StringRes
        expandTextRes: Int = R.string.show_all,
        @StringRes
        collapseTextRes: Int = R.string.collapse,
        isExpanded: Boolean = false,
        maxCollapsedItems: Int = 3,
    ) : this(
        iconRes,
        App.context.getString(titleRes),
        App.context.getString(expandTextRes),
        App.context.getString(collapseTextRes),
        isExpanded,
        maxCollapsedItems
    )

    val actionText: String
        get() = if (isExpanded) collapseText else expandText

    lateinit var section: SectionWithHeader

    fun toggle() {
        isExpanded = !isExpanded
    }
}