package com.nexcode.ai

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways

/** Native Sora editor integrated with the Compose document and tab state. */
@Composable
internal fun VexCodeEditor(
    value: String,
    fileName: String,
    onValueChange: (String) -> Unit,
    onEditorReady: (CodeEditor?) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnEditorReady by rememberUpdatedState(onEditorReady)
    val languageKey = fileName.substringAfterLast('.', "").lowercase()

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            CodeEditor(viewContext).apply {
                setText(value)
                setTextSize(14f)
                setTypefaceText(Typeface.MONOSPACE)
                setLineNumberEnabled(true)
                setWordwrap(false)
                setUndoEnabled(true)
                setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()))
                setEditorLanguage(VexTextMate.languageFor(fileName))
                tag = languageKey

                subscribeAlways<ContentChangeEvent> {
                    latestOnValueChange(text.toString())
                }
                latestOnEditorReady(this)
            }
        },
        update = { editor ->
            if (editor.text.toString() != value) editor.setText(value)
            if (editor.tag != languageKey) {
                editor.setEditorLanguage(VexTextMate.languageFor(fileName))
                editor.tag = languageKey
            }
        },
        onRelease = { editor ->
            latestOnEditorReady(null)
            editor.release()
        }
    )
}
