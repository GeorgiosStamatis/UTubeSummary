package io.github.utubesummary.ui.component

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

private val headingRegex = Regex("^\\s{0,3}#{1,6}\\s+(.*)$")
private val bulletRegex = Regex("^(\\s*)[-*+]\\s+(.*)$")
private val ruleRegex = Regex("^\\s{0,3}([-*_])(\\s*\\1){2,}\\s*$")
private val inlineRegex =
    Regex("\\*\\*(.+?)\\*\\*|`([^`\\n]+)`|(?<![*\\w])\\*([^\\s*][^*\\n]*?)\\*(?![*\\w])")

private val headingStyle = SpanStyle(
    fontWeight = FontWeight.Bold,
    fontSize = TextUnit(1.1f, TextUnitType.Em),
)

/** True for the `|---|:---:|` row that separates a table header from its body. */
private fun isTableSeparator(line: String): Boolean =
    line.contains('|') && line.contains('-') && line.all { it in "|-: \t" }

/**
 * Converts the common Markdown found in LLM answers (headings, bold, italic, inline code, bullet
 * lists, rules and simple tables) into an [AnnotatedString]. Text without Markdown is unchanged.
 */
fun markdownToAnnotatedString(markdown: String): AnnotatedString = buildAnnotatedString {
    var inFence = false
    var firstLine = true
    for (line in markdown.lines()) {
        if (line.trimStart().startsWith("```")) {
            inFence = !inFence
            continue
        }
        if (!inFence && isTableSeparator(line)) continue

        if (!firstLine) append("\n")
        firstLine = false

        when {
            inFence -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(line) }

            headingRegex.matches(line) -> {
                val title = headingRegex.matchEntire(line)!!.groupValues[1].trim()
                withStyle(headingStyle) { appendInline(title) }
            }

            ruleRegex.matches(line) -> append("────────")

            line.trimStart().startsWith("|") -> {
                val cells = line.trim().trim('|').split('|').map { it.trim() }
                appendInline(cells.joinToString("  ·  "))
            }

            bulletRegex.matches(line) -> {
                val match = bulletRegex.matchEntire(line)!!
                append(match.groupValues[1])
                append("• ")
                appendInline(match.groupValues[2])
            }

            else -> appendInline(line)
        }
    }
}

private fun AnnotatedString.Builder.appendInline(text: String) {
    var last = 0
    for (match in inlineRegex.findAll(text)) {
        append(text.substring(last, match.range.first))
        val bold = match.groups[1]?.value
        val code = match.groups[2]?.value
        val italic = match.groups[3]?.value
        when {
            bold != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
            code != null -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(code) }
            italic != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
        }
        last = match.range.last + 1
    }
    append(text.substring(last))
}
