package com.bimantara.feature.mdreader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class ListItem(
        val ordered: Boolean,
        val number: Int,
        val text: String,
        val isTask: Boolean = false,
        val isChecked: Boolean = false
    ) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
}

object MarkdownParser {

    fun parse(rawMarkdown: String): List<MarkdownBlock> {
        val lines = rawMarkdown.lines()
        val blocks = mutableListOf<MarkdownBlock>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 1. Code block fence
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
                i++
                continue
            }

            // 2. Horizontal rule
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                blocks.add(MarkdownBlock.HorizontalRule)
                i++
                continue
            }

            // 3. Headings
            if (trimmed.startsWith("#")) {
                var level = 0
                while (level < trimmed.length && trimmed[level] == '#') {
                    level++
                }
                if (level in 1..6 && trimmed.length > level && trimmed[level] == ' ') {
                    val headingText = trimmed.substring(level).trim()
                    blocks.add(MarkdownBlock.Heading(level, headingText))
                    i++
                    continue
                }
            }

            // 4. Blockquote
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString("\n")))
                continue
            }

            // 5. Task List or Bullet list
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                val content = trimmed.substring(2).trim()
                if (content.startsWith("[ ] ") || content.startsWith("[x] ") || content.startsWith("[X] ")) {
                    val isChecked = content.startsWith("[x] ", ignoreCase = true)
                    val taskText = content.substring(4).trim()
                    blocks.add(MarkdownBlock.ListItem(ordered = false, number = 0, text = taskText, isTask = true, isChecked = isChecked))
                } else {
                    blocks.add(MarkdownBlock.ListItem(ordered = false, number = 0, text = content))
                }
                i++
                continue
            }

            // 6. Numbered list
            val numberedMatch = Regex("^(\\d+)\\.\\s+(.*)").find(trimmed)
            if (numberedMatch != null) {
                val num = numberedMatch.groupValues[1].toIntOrNull() ?: 1
                val text = numberedMatch.groupValues[2]
                blocks.add(MarkdownBlock.ListItem(ordered = true, number = num, text = text))
                i++
                continue
            }

            // 7. Markdown Table
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    tableLines.add(lines[i].trim())
                    i++
                }
                if (tableLines.size >= 2) {
                    val headers = tableLines[0].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    // Filter out divider line e.g. |---|---|
                    val rowLines = tableLines.drop(1).filter { lineStr ->
                        !lineStr.replace("|", "").replace("-", "").replace(":", "").trim().isEmpty()
                    }
                    val rows = rowLines.map { rowStr ->
                        rowStr.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    blocks.add(MarkdownBlock.Table(headers, rows))
                } else if (tableLines.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Paragraph(tableLines.joinToString("\n")))
                }
                continue
            }

            // 8. Empty line
            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // 9. Standard Paragraph (gather multi-line paragraphs)
            val paragraphLines = mutableListOf<String>()
            while (i < lines.size) {
                val nextTrimmed = lines[i].trim()
                if (nextTrimmed.isEmpty() ||
                    nextTrimmed.startsWith("#") ||
                    nextTrimmed.startsWith("```") ||
                    nextTrimmed.startsWith(">") ||
                    nextTrimmed.startsWith("- ") ||
                    nextTrimmed.startsWith("* ") ||
                    Regex("^(\\d+)\\.\\s+").containsMatchIn(nextTrimmed) ||
                    nextTrimmed == "---"
                ) {
                    break
                }
                paragraphLines.add(lines[i])
                i++
            }
            if (paragraphLines.isNotEmpty()) {
                blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString(" ")))
            }
        }

        return blocks
    }

    /**
     * Parses inline Markdown text styles (**bold**, *italic*, `code`, ~~strikethrough~~) into AnnotatedString.
     */
    fun parseInlineFormatting(
        text: String,
        textColor: Color = Color.Unspecified,
        codeBgColor: Color = Color(0x3394A3B8)
    ): AnnotatedString {
        return buildAnnotatedString {
            var index = 0
            val len = text.length

            while (index < len) {
                // Inline Code: `code`
                if (text[index] == '`') {
                    val endCode = text.indexOf('`', index + 1)
                    if (endCode != -1) {
                        val codeContent = text.substring(index + 1, endCode)
                        val startPos = length
                        append(codeContent)
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBgColor,
                                fontWeight = FontWeight.Medium
                            ),
                            startPos,
                            length
                        )
                        index = endCode + 1
                        continue
                    }
                }

                // Bold: **text**
                if (text.startsWith("**", index)) {
                    val endBold = text.indexOf("**", index + 2)
                    if (endBold != -1) {
                        val boldContent = text.substring(index + 2, endBold)
                        val startPos = length
                        append(boldContent)
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), startPos, length)
                        index = endBold + 2
                        continue
                    }
                }

                // Strikethrough: ~~text~~
                if (text.startsWith("~~", index)) {
                    val endStrike = text.indexOf("~~", index + 2)
                    if (endStrike != -1) {
                        val strikeContent = text.substring(index + 2, endStrike)
                        val startPos = length
                        append(strikeContent)
                        addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), startPos, length)
                        index = endStrike + 2
                        continue
                    }
                }

                // Italic: *text* (single star)
                if (text[index] == '*' && (index + 1 < len && text[index + 1] != '*')) {
                    val endItalic = text.indexOf('*', index + 1)
                    if (endItalic != -1 && (endItalic + 1 >= len || text[endItalic + 1] != '*')) {
                        val italicContent = text.substring(index + 1, endItalic)
                        val startPos = length
                        append(italicContent)
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), startPos, length)
                        index = endItalic + 1
                        continue
                    }
                }

                // Normal character
                append(text[index])
                index++
            }
        }
    }
}
