package ru.skillbranch.skillarticles.markdown

import java.util.regex.Pattern

object MarkdownParser {

    private val LINE_SEPARATOR = System.lineSeparator()

    // group regex
    private const val UNORDERED_LIST_ITEM_GROUP = "(^[*+-] .+$)"
    private const val HEADER_GROUP = "(^#{1,6} .+?$)"
    private const val QUOTE_GROUP = "(^> .+?$)"
    private const val ITALIC_GROUP = "((?<!\\*)\\*[^*].*?[^*]?\\*(?!\\*)|(?<!_)_[^_].*?[^_]?_(?!_))"
    private const val BOLD_GROUP =
        "((?<!\\*)\\*{2}[^*].*?[^*]?\\*{2}(?!\\*)|(?<!_)_{2}[^_].*?[^_]?_{2}(?!_))"
    private const val STRIKE_GROUP = "((?<!~)~{2}[^~].*?[^~]?~{2}(?!~))"
    private const val RULE_GROUP = "(^[-_*]{3}$)"
    private const val INLINE_GROUP = "((?<!`)`[^`\\s].*?[^`\\s]?`(?!`))"

    //    private const val BLOCK_GROUP = "(^(?<!`)`{3}[^`\\s][\\s\\S]+?[^`]?`{3}(?!`)$)"
    private const val BLOCK_GROUP = "((?<!`)`{3}[^`\\s](?:.|\\n)*?[^`\\s]?`{3}(?!`))"

    //    private const val BLOCK_GROUP = "(^`{3}[\\s\\S]+?`{3}$)"
    private const val LINK_GROUP = "(\\[[^\\[\\]]*?]\\(.+?\\)|^\\[*?]\\(.*?\\))"
    private const val IMAGE_GROUP = "(!\\[[^\\[\\]]*?]\\(.+?\\)|^\\[*?]\\(.*?\\))"
    private const val ORDERED_LIST_ITEM_GROUP = "(^(\\d+)\\. .+$)"

    // result regex
    private const val MARKDOWN_GROUPS = "$UNORDERED_LIST_ITEM_GROUP|$HEADER_GROUP|$QUOTE_GROUP" +
            "|$ITALIC_GROUP|$BOLD_GROUP|$STRIKE_GROUP|$RULE_GROUP" +
            "|$INLINE_GROUP|$BLOCK_GROUP|$LINK_GROUP|$IMAGE_GROUP|$ORDERED_LIST_ITEM_GROUP"

    private val elementsPattern by lazy { Pattern.compile(MARKDOWN_GROUPS, Pattern.MULTILINE) }

    /**
     * parse markdown text to elements
     */
    fun parse(string: String?): MarkdownText {
        val elements = mutableListOf<Element>()
        if (!string.isNullOrEmpty())
            elements.addAll(findElements(string))
        return MarkdownText(elements)
    }

    /**
     * clear markdown text to string without markdown characters
     */
    fun clear(string: String?): String? = parse(string).elements.joinToString("") {
        if (it is Element.Text) it.text else it.text.replace(Regex("[_~*]"), "")
    }

    /**
     * finds markdown element in markdown text
     */
    private fun findElements(string: CharSequence): List<Element> {
        val parents = mutableListOf<Element>()
        val matcher = elementsPattern.matcher(string)
        var lastStartIndex = 0

        loop@ while (matcher.find(lastStartIndex)) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            // if something is found then everything before - TEXT
            if (lastStartIndex < startIndex) {
                parents.add(Element.Text(string.subSequence(lastStartIndex, startIndex)))
            }

            // found text
            var text: CharSequence

            // groups range for iterate by groups
            val groups = 1..12
            var group = -1
            for (gr in groups) {
                if (matcher.group(gr) != null) {
                    group = gr
                    break
                }
            }

            when (group) {
                // NOT FOUND -> break
                -1 -> break@loop

                // UNORDERED LIST
                1 -> {
                    // text without "*. "
                    text = string.subSequence(startIndex.plus(2), endIndex)

                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.UnorderedListItem(text, subs)
                    parents.add(element)

                    // next find start from position "endIndex" (last regex char)
                    lastStartIndex = endIndex

                }

                // HEADER
                2 -> {
                    val reg = "^#{1,6}".toRegex().find(string.subSequence(startIndex, endIndex))
                    val level = reg!!.value.length

                    // text without "{#} ", inc() for "\s"
                    text = string.subSequence(startIndex.plus(level.inc()), endIndex)
                    val element = Element.Header(level, text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                // QUOTE
                3 -> {
                    // text without "> "
                    text = string.subSequence(startIndex.plus(2), endIndex)
                    val subelements = findElements(text)
                    val element = Element.Quote(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                // ITALIC
                4 -> {
                    // text without "*{}*"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())
                    val subelements = findElements(text)
                    val element = Element.Italic(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                // BOLD
                5 -> {
                    // text without "**{}**"
                    text = string.subSequence(startIndex.plus(2), endIndex.minus(2))
                    val subelements = findElements(text)
                    val element = Element.Bold(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                // STRIKE
                6 -> {
                    // text without "~~{}~~"
                    text = string.subSequence(startIndex.plus(2), endIndex.minus(2))
                    val subelements = findElements(text)
                    val element = Element.Strike(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                // RULE
                7 -> {
                    // text without "***" insert empty char
                    val element = Element.Rule()
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                // INLINE CODE
                8 -> {
                    // text without "`{}`"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())
                    val element = Element.InlineCode(text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                // BLOCK CODE
                9 -> {
                    // text without "```{}```"
                    text = string.subSequence(startIndex.plus(3), endIndex.minus(3))
                    val subs = findElements(text)
                    val element = Element.BlockCode(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                // LINK
                10 -> {
                    // full text for regex
                    text = string.subSequence(startIndex, endIndex)
                    val (title: String, link: String) = "\\[(.*)]\\((.*)\\)".toRegex()
                        .find(text)!!.destructured

                    val element = Element.Link(link, title)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                // IMAGE
                11 -> {
                    // full text for regex
                    text = string.subSequence(startIndex, endIndex)
                    val (alt: String, url: String, title: String?) = "!\\[(.*)]\\((.*?)(\".*?\")?\\)".toRegex()
                        .find(text)!!.destructured

                    val element = Element.Image(
                        url = url.trim(),
                        alt = alt.takeIf { it.isNotEmpty() },
                        text = title.let {
                            if (it.isNotEmpty()) it.substring(
                                1,
                                it.length.dec()
                            ) else it
                        }
                    )
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                // ORDERED LIST
                12 -> {
                    // text without "*. "
                    val order = string.subSequence(startIndex, startIndex.inc()).toString()
                    text = string.subSequence(startIndex.plus(3), endIndex)

                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.OrderedListItem(order, text, subs)
                    parents.add(element)

                    // next find start from position "endIndex" (last regex char)
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

    data class OrderedListItem(
        val order: String,
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
        override val text: CharSequence = " ", //for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class InlineCode(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class BlockCode(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Link(
        val link: String,
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