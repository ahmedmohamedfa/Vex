package com.nexcode.ai

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Lightweight lexical highlighting for common source languages.
 * It preserves every character and cursor offset; it is not a full parser/LSP.
 */
internal class CodeSyntaxTransformation : VisualTransformation {
    private val tokenPattern = Regex(
        """(?s)//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\b(?:fun|val|var|class|object|interface|if|else|when|for|while|do|return|break|continue|package|import|public|private|protected|internal|override|open|abstract|suspend|inline|data|sealed|enum|try|catch|finally|throw|new|const|let|function|async|await|def|self|from|as|in|is|true|false|null|None|True|False)\b|\b\d+(?:\.\d+)?\b"""
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        val result = AnnotatedString.Builder(source)
        tokenPattern.findAll(source).forEach { match ->
            val token = match.value
            val color = when {
                token.startsWith("//") || token.startsWith("/*") -> Color(0xFF7F91A8)
                token.startsWith('"') || token.startsWith('\'') -> Color(0xFFE6B879)
                token.firstOrNull()?.isDigit() == true -> Color(0xFFB9A0F5)
                else -> Color(0xFF79B8FF)
            }
            result.addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1)
        }
        return TransformedText(result.toAnnotatedString(), OffsetMapping.Identity)
    }
}
