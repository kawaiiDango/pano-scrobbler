package com.arn.scrobble.help

object MdParser {

    // ─── Block-level parser ───────────────────────────────────────
    private val listPrefixes = arrayOf("- ", "* ", "+ ")
    private val headingPrefixes = arrayOf("# ", "## ", "### ", "#### ", "##### ", "###### ")

    fun parseMarkdown(input: String): List<MdNode.Block> {
        val blocks = mutableListOf<MdNode.Block>()
        val lines = input.lines()
        var i = 0

        // Each entry is a raw line segment; will be merged with soft/hard logic
        val paragraphLines = mutableListOf<String>()

        // Each entry: (rawLines for this item)
        val listItems = mutableListOf<MutableList<String>>()

        fun joinLines(rawLines: List<String>): List<MdNode.InlineNode> {
            // Merge lines: trailing "  " = hard break, otherwise soft break (space)
            // But don't append trailing space/break after the last line
            val nodes = mutableListOf<MdNode.InlineNode>()
            rawLines.forEachIndexed { idx, line ->
                val isLast = idx == rawLines.lastIndex
                val hardBreak = line.endsWith("  ")
                val trimmed = if (hardBreak) line.trimEnd() else line
                nodes.addAll(parseInline(trimmed))
                if (!isLast) {
                    if (hardBreak) nodes.add(MdNode.InlineNode.HardBreak)
                    else nodes.add(MdNode.InlineNode.TextSpan(" "))
                }
            }
            return nodes
        }

        fun flushParagraph() {
            if (paragraphLines.isNotEmpty()) {
                blocks.add(MdNode.Block.Paragraph(joinLines(paragraphLines)))
                paragraphLines.clear()
            }
        }

        fun flushList() {
            if (listItems.isNotEmpty()) {
                blocks.add(MdNode.Block.UnorderedList(listItems.map { MdNode.ListItem(joinLines(it)) }))
                listItems.clear()
            }
        }

        while (i < lines.size) {
            val line = lines[i]

            // ── Fenced code block ──
            if (line.trimStart().startsWith("```")) {
                flushParagraph()
                flushList()
                val meta = line.trimStart().removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size) {
                    if (lines[i].trimStart().startsWith("```")) {
                        i++; break
                    }
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(
                    MdNode.Block.CodeBlock(
                        language = meta,
                        code = codeLines.joinToString("\n")
                    )
                )
                continue
            }

            // ── Heading ──
            val headingLevel =
                headingPrefixes.indexOfFirst { line.startsWith(it) }.takeIf { it >= 0 }?.plus(1)

            if (headingLevel != null) {
                flushParagraph()
                flushList()
                val content = line.removePrefix(headingPrefixes[headingLevel - 1]).trim()
                blocks.add(MdNode.Block.Heading(headingLevel, parseInline(content)))
                i++
                continue
            }

            // ── Unordered list item ──
            val listPrefix = listPrefixes.firstOrNull { line.startsWith(it) }
            if (listPrefix != null) {
                flushParagraph()
                val itemContent =
                    line.removePrefix(listPrefix) // preserve trailing spaces for hard break detection
                val itemLines = mutableListOf(itemContent)
                i++
                // Gather continuation lines (indented by at least 2 spaces, or a tab)
                while (i < lines.size) {
                    val next = lines[i]
                    if (next.startsWith("  ") || next.startsWith("\t")) {
                        itemLines.add(next.trimStart())
                        i++
                    } else {
                        // Non-indented, non-blank line after list item = paragraph continuation
                        // per CommonMark loose list rules; treat as end of this item
                        break
                    }
                }
                listItems.add(itemLines)
                continue
            }

            // ── Blank line ──
            if (line.isBlank()) {
                flushParagraph()
                flushList()
                i++
                continue
            }

            // ── Paragraph text ──
            // If we were accumulating a list and hit a non-blank non-list line,
            // flush the list first
            flushList()
            paragraphLines.add(line)
            i++
        }

        flushParagraph()
        flushList()
        return blocks
    }

    // ─── Inline parser ────────────────────────────────────────────

    private fun parseInline(input: String): List<MdNode.InlineNode> = InlineReader(input).parse()

    private class InlineReader(private val src: String) {
        private var pos = 0

        fun parse(stopBefore: String? = null): List<MdNode.InlineNode> {
            val nodes = mutableListOf<MdNode.InlineNode>()
            val textBuf = StringBuilder()

            fun flushText() {
                if (textBuf.isNotEmpty()) {
                    nodes.add(MdNode.InlineNode.TextSpan(textBuf.toString()))
                    textBuf.clear()
                }
            }

            while (pos < src.length) {
                if (stopBefore != null && src.startsWith(stopBefore, pos)) {
                    pos += stopBefore.length
                    flushText()
                    return nodes
                }

                val ch = src[pos]

                // ── Hard line break encoded as \n in the raw string ──
                if (ch == '\n') {
                    flushText()
                    nodes.add(MdNode.InlineNode.HardBreak)
                    pos++
                    continue
                }

                // ── Backslash escape or backslash hard break ──
                if (ch == '\\' && pos + 1 < src.length) {
                    val next = src[pos + 1]
                    if (next == '\n') {
                        // Backslash before newline = hard break
                        flushText()
                        nodes.add(MdNode.InlineNode.HardBreak)
                        pos += 2
                        continue
                    }
                    if (next in """\`*_[]()#""") {
                        textBuf.append(next)
                        pos += 2
                        continue
                    }
                }

                // ── Inline code ──
                if (ch == '`') {
                    flushText()
                    pos++
                    val start = pos
                    while (pos < src.length && src[pos] != '`') pos++
                    nodes.add(MdNode.InlineNode.InlineCode(src.substring(start, pos)))
                    if (pos < src.length) pos++
                    continue
                }

                // ── Link [text](url) ──
                if (ch == '[') {
                    val saved = pos
                    pos++
                    val linkChildren = parse(stopBefore = "]")
                    if (pos < src.length && src[pos] == '(') {
                        pos++
                        val urlStart = pos
                        var depth = 1
                        while (pos < src.length && depth > 0) {
                            if (src[pos] == '(') depth++
                            else if (src[pos] == ')') depth--
                            if (depth > 0) pos++
                        }
                        val url = src.substring(urlStart, pos)
                        if (pos < src.length) pos++
                        flushText()
                        nodes.add(MdNode.InlineNode.LinkSpan(linkChildren, url))
                    } else {
                        pos = saved
                        textBuf.append(src[pos])
                        pos++
                    }
                    continue
                }

                // ── Bold / Italic (* or _) ──
                if (ch == '*' || ch == '_') {
                    val runLen = countRun(ch)
                    if (runLen >= 3 && hasClosingRun(ch, 3)) {
                        flushText(); pos += 3
                        nodes.add(MdNode.InlineNode.BoldItalicSpan(parse(ch.toString().repeat(3))))
                        continue
                    }
                    if (runLen >= 2 && hasClosingRun(ch, 2)) {
                        flushText(); pos += 2
                        nodes.add(MdNode.InlineNode.BoldSpan(parse(ch.toString().repeat(2))))
                        continue
                    }
                    if (runLen >= 1 && hasClosingRun(ch, 1)) {
                        flushText(); pos += 1
                        nodes.add(MdNode.InlineNode.ItalicSpan(parse(ch.toString().repeat(1))))
                        continue
                    }
                    textBuf.append(ch); pos++
                    continue
                }

                textBuf.append(ch); pos++
            }

            flushText()
            return nodes
        }

        private fun countRun(ch: Char): Int {
            var c = 0; while (pos + c < src.length && src[pos + c] == ch) c++; return c
        }

        private fun hasClosingRun(delimiter: Char, count: Int): Boolean {
            val needle = delimiter.toString().repeat(count)
            var j = pos + count
            while (j <= src.length - count) {
                if (src[j] == '\\') {
                    j += 2; continue
                }
                if (src[j] == '`') {
                    j++; while (j < src.length && src[j] != '`') j++; j++; continue
                }
                if (src.startsWith(needle, j)) {
                    val after = if (j + count < src.length) src[j + count] else ' '
                    if (count == 1 && after == delimiter) {
                        j++; continue
                    }
                    if (count == 2 && after == delimiter) {
                        j++; continue
                    }
                    return true
                }
                j++
            }
            return false
        }
    }
}