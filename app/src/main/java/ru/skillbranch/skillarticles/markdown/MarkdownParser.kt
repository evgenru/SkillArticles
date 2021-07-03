package ru.skillbranch.skillarticles.markdown

import java.util.regex.Pattern

object MarkdownParser {

    private val LINE_SEPARATOR = System.getProperty("line.separator") ?: "\n"

    enum class GROUPS(val regex: String) {
        UNORDERED_LIST_ITEM_GROUP("(^[*+-] .+?$)"),
        ORDERED_LIST_ITEM_GROUP("(^\\d\\. .+?$)"),
        HEADER_GROUP("(^#{1,6} .+?$)"),
        QUOTE_GROUP("(^> .+?$)"),
        ITALIC_GROUP("((?<!\\*)\\*[^*].*?[^*]?\\*(?!\\*)|(?<!_)_[^_].*?[^_]?_(?!_))"),
        BOLD_GROUP("((?<!\\*)\\*{2}[^*].*?[^*]?\\*{2}(?!\\*)|(?<!_)_{2}[^_].*?[^_]?_{2}(?!_))"),
        STRIKE_GROUP("((?<!~)~{2}[^~].*?[^~]?~{2}(?!~))"),
        RILE_GROUP("(^[-*_]{3}$)"),
        INLINE_CODE_GROUP("((?<!`)`[^`\\s].*?[^`\\s]?`(?!`))"),
        BLOCK_CODE_GROUP("((?<!`)`{3}[^`\\s](?:.|\\n)*?[^`\\s]?`{3}(?!`))"),
        LINK_GROUP("(\\[[^\\]]*?]\\(.+?\\)|^\\[*?]\\(.*?\\))"),
        IMAGE_GROUP("(!\\[[^\\]]*?]\\(.+?[ ]?(?:\".+\")?\\))")
    }

    private val MARKDOWN_GROUPS = GROUPS.values().joinToString("|") { it.regex }

    private val elementsPattern by lazy { Pattern.compile(MARKDOWN_GROUPS, Pattern.MULTILINE) }

    fun parse(string: String): MarkdownText {
        val elements = mutableListOf<Element>()
        elements.addAll(findElements(string))
        return MarkdownText(elements)
    }

    fun clear(string: String): String? {
        return parse(string).clearText
    }

    private fun findElements(string: CharSequence): List<Element> {
        val parents = mutableListOf<Element>()
        val matcher = elementsPattern.matcher(string)
        var lastStartIndex = 0

        loop@ while (matcher.find(lastStartIndex)) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            if (lastStartIndex < startIndex) {
                parents.add(Element.Text(string.subSequence(lastStartIndex, startIndex)))
            }

            var text: CharSequence

            val groups = 1..GROUPS.values().size
            var group = -1
            for (gr in groups) {
                if (matcher.group(gr) != null) {
                    group = gr
                    break
                }
            }
            if (group == -1) break@loop
            when (GROUPS.values()[group - 1]) {

                GROUPS.UNORDERED_LIST_ITEM_GROUP -> {
                    // "* "
                    text = string.subSequence(startIndex.plus(2), endIndex)

                    val subs = findElements(text)
                    val element = Element.UnorderedListItem(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                GROUPS.HEADER_GROUP -> {
                    // "### "
                    val regHeader =
                        "#{1,6}".toRegex().find(string.subSequence(startIndex, endIndex))
                    val level = regHeader!!.value.length
                    text = string.subSequence(startIndex.plus(level.inc()), endIndex)

                    val subs = findElements(text)
                    val element = Element.Header(level, text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                GROUPS.QUOTE_GROUP -> {
                    // "> "
                    text = string.subSequence(startIndex.plus(2), endIndex)

                    val subs = findElements(text)
                    val element = Element.Quote(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                GROUPS.ITALIC_GROUP -> {
                    text = string.subSequence(startIndex.plus(1), endIndex.minus(1))

                    val subs = findElements(text)
                    val element = Element.Italic(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                GROUPS.BOLD_GROUP -> {
                    text = string.subSequence(startIndex.plus(2), endIndex.minus(2))

                    val subs = findElements(text)
                    val element = Element.Bold(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                GROUPS.STRIKE_GROUP -> {
                    text = string.subSequence(startIndex.plus(2), endIndex.minus(2))

                    val subs = findElements(text)
                    val element = Element.Strike(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                GROUPS.RILE_GROUP -> {
                    val element = Element.Rule()
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                GROUPS.INLINE_CODE_GROUP -> {
                    text = string.subSequence(startIndex.plus(1), endIndex.minus(1))

                    val subs = findElements(text)
                    val element = Element.InlineCode(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                GROUPS.LINK_GROUP -> {
                    text = string.subSequence(startIndex, endIndex)
                    val (title: String, link: String) = "\\[(.*)]\\((.*)\\)".toRegex()
                        .find(text)!!.destructured
                    val element = Element.Link(link, title)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                GROUPS.ORDERED_LIST_ITEM_GROUP -> {
                    val (order: String, title) = "^(\\d\\.) (.*?)$".toRegex()
                        .find(string.subSequence(startIndex, endIndex))!!.destructured
                    val subs = findElements(title)
                    val element = Element.OrderedListItem(order, title, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                GROUPS.BLOCK_CODE_GROUP -> {
                    text = string.subSequence(startIndex.plus(3), endIndex.minus(3))

                    val subs = findElements(text)
                    val element = Element.BlockCode(text = text, elements = subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                GROUPS.IMAGE_GROUP -> {
                    val (alt: String, link: String, title: String) = "!\\[([^\\]]*?)]\\((.+?)[ ]?(?:\"(.+)\")?\\)".toRegex()
                        .find(string.subSequence(startIndex, endIndex))!!.destructured
                    val element = Element.Image(link, alt.ifEmpty { null }, title)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
            }
        }

        if (lastStartIndex < string.length) {
            val text = string.subSequence(lastStartIndex, string.length)
            parents.add(Element.Text(text))
        }

        return parents
    }

}

data class MarkdownText(val elements: List<Element>)

val MarkdownText.clearText: String
    get() = this.elements.joinToString("") { it.clearText }


val Element.clearText: String
    get() = if (elements.isEmpty()) {
        text.toString()
    } else {
        elements.joinToString("") { it.clearText }
    }


sealed class Element() {
    abstract val text: CharSequence
    abstract val elements: List<Element>

    data class Text(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class UnorderedListItem(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Header(
        val level: Int = 1,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Quote(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Italic(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Bold(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Strike(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Rule(
        override val text: CharSequence = " ",
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class InlineCode(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Link(
        val link: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class BlockCode(
        val type: Type = Type.MIDDLE,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element() {
        enum class Type { START, END, MIDDLE, SINGLE }
    }

    data class OrderedListItem(
        val order: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Image(
        val url: String,
        val alt: String?,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

}