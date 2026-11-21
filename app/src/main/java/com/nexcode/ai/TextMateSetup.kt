package com.nexcode.ai

import android.content.Context
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource

/** Loads local grammars once. No network access is required for syntax highlighting. */
internal object VexTextMate {
    @Volatile
    private var initialized = false

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(appContext.assets))

        val themePath = "textmate/vex-night.json"
        val themeStream = FileProviderRegistry.getInstance().tryGetInputStream(themePath)
            ?: error("Vex theme asset is missing")
        ThemeRegistry.getInstance().loadTheme(
            ThemeModel(IThemeSource.fromInputStream(themeStream, themePath, null), "vex-night").apply {
                isDark = true
            }
        )
        ThemeRegistry.getInstance().setTheme("vex-night")
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
        initialized = true
    }

    fun isReady(): Boolean = initialized

    fun languageFor(fileName: String): Language {
        if (!initialized) return EmptyLanguage()
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val scope = when (extension) {
            "kt", "kts" -> "source.kotlin"
            "java" -> "source.java"
            "js", "jsx", "mjs", "cjs", "ts", "tsx", "json" -> "source.js"
            "py" -> "source.python"
            "html", "htm" -> "text.html.basic"
            "md", "markdown" -> "text.html.markdown"
            "xml", "svg" -> "text.xml"
            else -> return EmptyLanguage()
        }
        return TextMateLanguage.create(scope, true)
    }

    fun languageLabel(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "kt", "kts" -> "Kotlin"
            "java" -> "Java"
            "js", "jsx", "mjs", "cjs" -> "JavaScript"
            "ts", "tsx" -> "TypeScript (JS grammar)"
            "py" -> "Python"
            "html", "htm" -> "HTML"
            "md", "markdown" -> "Markdown"
            "xml", "svg" -> "XML"
            "json" -> "JSON (JS grammar)"
            else -> fileName.substringAfterLast('.', "Plain Text").uppercase()
        }
    }
}
