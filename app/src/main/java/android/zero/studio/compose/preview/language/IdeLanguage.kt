package android.zero.studio.compose.preview.language

import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration

/**
 * A base language implementation for the IDE that leverages the TextMate engine for syntax highlighting and language configuration.
 * This class serves as a wrapper around [TextMateLanguage] and explicitly maintains references to the underlying TextMate components.
 * 
 * 工作流程线路图:
 * 1. 构造函数接收 Grammar, Configuration 和 Registries。
 * 2. 立即通过 super() 初始化父类 TextMateLanguage。
 * 3. 在 init 块中，将传入的参数 1:1 赋值给类内部定义的私有属性。
 * 
 * 父类关系:
 * - 继承自 io.github.rosemoe.sora.langs.textmate.TextMateLanguage
 * 
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