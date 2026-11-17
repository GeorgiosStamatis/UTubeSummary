package io.github.utubesummary.llm.tools

/**
 * Builds the text of a Word document from its XML events. Paragraphs become lines, tabs and line
 * breaks are kept, and a table becomes one line per row with the cells separated by tabs.
 */
internal class DocxTextBuilder {
    private val text = StringBuilder()
    private var cellDepth = 0

    fun startElement(name: String) {
        when (name) {
            "w:tab" -> text.append('\t')
            "w:br", "w:cr" -> text.append('\n')
            "w:tc" -> cellDepth++
        }
    }

    fun text(value: String) {
        text.append(value)
    }

    fun endElement(name: String) {
        when (name) {
            // Inside a cell a paragraph break would split the row, so it becomes a space.
            "w:p" -> text.append(if (cellDepth > 0) ' ' else '\n')

            "w:tc" -> {
                cellDepth = (cellDepth - 1).coerceAtLeast(0)
                trimTrailingSpaces()
                text.append('\t')
            }

            "w:tr" -> {
                if (text.lastOrNull() == '\t') text.setLength(text.length - 1)
                text.append('\n')
            }
        }
    }

    fun build(): String = text.toString().trim()

    private fun trimTrailingSpaces() {
        while (text.lastOrNull() == ' ') text.setLength(text.length - 1)
    }
}
