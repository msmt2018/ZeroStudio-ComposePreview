package android.zero.studio.compose.preview.language

import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration

/**
 * A base language implementation for the IDE that leverages the TextMate engine for syntax highlighting and language configuration.
 * This class serves as a wrapper around [TextMateLanguage] and explicitly maintains references to the underlying TextMate components.
 * @author android_zero
 */
open class IdeLanguage(
    grammar: IGrammar?,
    langConfiguration: LanguageConfiguration?,
    grammarRegistry: GrammarRegistry,
    themeRegistry: ThemeRegistry,
    createIdentifiers: Boolean = false
) : TextMateLanguage(grammar, langConfiguration, grammarRegistry, themeRegistry, createIdentifiers) {

    private val grammar: IGrammar?
    private val langConfiguration: LanguageConfiguration?
    private val grammarRegistry: GrammarRegistry
    private val themeRegistry: ThemeRegistry
    private val createIdentifiers: Boolean

    init {
        this.grammar = grammar
        this.langConfiguration = langConfiguration
        this.grammarRegistry = grammarRegistry
        this.themeRegistry = themeRegistry
        this.createIdentifiers = createIdentifiers
    }
}