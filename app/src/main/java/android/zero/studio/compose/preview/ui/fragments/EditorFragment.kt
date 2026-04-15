package android.zero.studio.compose.preview.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ThreadUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.zero.studio.compose.preview.ComposeApplication
import android.zero.studio.compose.preview.R
import android.zero.studio.compose.preview.databinding.FragmentEditorBinding
import android.zero.studio.compose.preview.editor.CodeEditorView
import android.zero.studio.compose.preview.editor.analyzers.DiagnosticAnalyzer
import android.zero.studio.compose.preview.editor.language.KotlinLanguage
import android.zero.studio.compose.preview.editor.symbol.SymbolInputView
import android.zero.studio.compose.preview.kotlin.parser.PreviewComposableFunctionParser
import android.zero.studio.compose.preview.ui.theme.AppTheme
import android.zero.studio.compose.preview.ui.viewmodel.EditorViewModel
import android.zero.studio.compose.preview.utils.DynamicPreviewLoader
import android.zero.studio.compose.preview.utils.FileUtil
import android.zero.studio.compose.preview.utils.MultipleDexClassLoader
import android.zero.studio.compose.preview.utils.className
import android.zero.studio.compose.preview.utils.getFileHash
import android.zero.studio.compose.preview.views.*
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import kotlinx.coroutines.*
import org.eclipse.tm4e.core.registry.IThemeSource
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.io.File

class EditorFragment : WorkspaceFragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    
    lateinit var codeEditor: CodeEditorView
        internal set

    private var contentHash: String? = null
    private var isSymbolsAdded: Boolean = false
    private val BOTTOM_EDITOR_SYMBOLS = arrayOf("->", "{", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/", "<", ">", "[", "]", ":")
    private val BOTTOM_EDITOR_SYMBOL_INSERT_TEXT = arrayOf("\t", "{}", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/", "<", ">", "[", "]", ":")

    private val editorViewModel: EditorViewModel by activityViewModels()

    private var autoSaveJob: Job? = null
    private lateinit var eventReceiver: SubscriptionReceipt<ContentChangeEvent>
    private var isPreviewLoaded: Boolean = false

    companion object {
        val TAG: String = EditorFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        this.codeEditor = binding.codeEditor as CodeEditorView
        return binding.getRoot()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViews()
        initiateFragmentsObservers()
        setEditorTypefaceText()
        setUpTextmate()
        ensureTextmateTheme()
        setContentChangeEvent()
        initText(isContentHash = true)
        setEditorLanguage()
        setupSymbols()
        
        savedInstanceState?.let {
            binding.topRightNavView.selectedItemId = it.getInt("selectedItemId")
        }
    }

    private fun setViews() {
        binding.toolBarEditor.title = FileUtil.playgroundCode.name
        bottomSheetBehavior = BottomSheetBehavior.from(binding.editorBottomSheet)
        bottomSheetBehavior.apply {
            isGestureInsetBottomIgnored = true
            isFitToContents = false
            state = BottomSheetBehavior.STATE_EXPANDED
            isDraggable = false
        }

        binding.topRightNavView.setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.code -> { showCode(); true }
                R.id.design -> {
                    showDesign()
                    if (!isPreviewLoaded) { loadPreview(); isPreviewLoaded = true }
                    true
                }
                R.id.split -> {
                    showSplit()
                    if (!isPreviewLoaded) { loadPreview(); isPreviewLoaded = true }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setContentChangeEvent() {
        codeEditor.subscribeAlways(ContentChangeEvent::class.java) { doAutoSave() }
    }
    
    private fun doAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = lifecycleScope.launch {
            delay(1000)
            save()
        }
    }

    private fun initText(isContentHash: Boolean = false) {
        try {
            val targetFile = file ?: return
            val content = targetFile.readText()
            codeEditor.setText(content)
            if (isContentHash) {
                contentHash = content.getFileHash()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun initiateFragmentsObservers() {
        editorViewModel.top_nav_position.observe(viewLifecycleOwner) {}
    }

    private fun loadComposePreview(name: String, clazz: String) {
        val dexFile = FileUtil.classesJarDex.apply { setReadOnly() }
        val classLoader = MultipleDexClassLoader(null, requireContext().classLoader).apply { loadDex(dexFile) }
        val dynamicLoader = DynamicPreviewLoader(classLoader, clazz, name)
        
        binding.composeView.apply {
            disposeComposition()
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

        val isDark = when (ComposeApplication.themeProvider.getTheme()) {
            1 -> false
            2 -> true
            else -> (resources.configuration.uiMode and 48) == 32
        }

        layoutLoading(false)
        ThreadUtils.runOnUiThread {
            binding.composeView.setContent {
                AppTheme(darkTheme = isDark) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        dynamicLoader.Render()
                    }
                }
            }
        }
    }

    fun loadPreview() {
        val previewLayouts = listOf(
            binding.linearLayoutNoPreview,
            binding.linearLayoutMultiplePreview,
            binding.linearLayoutComposePreview,
            binding.linearLayoutInitializing
        )

        lifecycleScope.launch {
            showOnly(previewLayouts, binding.linearLayoutInitializing, true)
            
            val functions = withContext(Dispatchers.IO) { parsePreviewFunctions() }

            when {
                functions.isNullOrEmpty() -> showOnly(previewLayouts, binding.linearLayoutNoPreview, true)
                functions.size > 1 -> showOnly(previewLayouts, binding.linearLayoutMultiplePreview, true)
                else -> {
                    showOnly(previewLayouts, binding.linearLayoutComposePreview, true)
                    val fn = functions[0]
                    loadComposePreview(fn.name!!, fn.className())
                }
            }
        }
    }
    
    private fun showOnly(layouts: List<LinearLayout>, target: View, animate: Boolean) {
        layouts.forEach { it.gone() }
        target.visible()
        if (animate) {
            requireContext().animate(target)
        }
    }

    private fun parsePreviewFunctions(): List<KtNamedFunction>? {
        val lang = codeEditor.editorLanguage as? KotlinLanguage ?: return null
        val cache = lang.kotlinEnvironment.cache
        val psiFile = cache.getOrUpdate(file!!.name, codeEditor.text.toString())
        return PreviewComposableFunctionParser.initialize(psiFile.ktFile).parse()
    }
    
    private fun showCode() {
        val set = ConstraintSet().apply { clone(binding.editorPreviewContainer) }
        set.constrainPercentWidth(binding.editorPreviewLinearLayout.id, 1.0f)
        set.constrainPercentWidth(binding.linearLayoutContainerPreview.id, 0.0f)
        set.applyTo(binding.editorPreviewContainer)
    }

    private fun showDesign() {
        hideWindows()
        val set = ConstraintSet().apply { clone(binding.editorPreviewContainer) }
        set.constrainPercentWidth(binding.editorPreviewLinearLayout.id, 0.0f)
        set.constrainPercentWidth(binding.linearLayoutContainerPreview.id, 1.0f)
        set.applyTo(binding.editorPreviewContainer)
    }

    private fun showSplit() {
        hideWindows()
        val set = ConstraintSet().apply { clone(binding.editorPreviewContainer) }
        set.constrainPercentWidth(binding.editorPreviewLinearLayout.id, 0.5f)
        set.constrainPercentWidth(binding.linearLayoutContainerPreview.id, 0.5f)
        set.applyTo(binding.editorPreviewContainer)
    }
    
    private fun setEditorLanguage() {
        eventReceiver = codeEditor.subscribeEvent(ContentChangeEvent::class.java, DiagnosticAnalyzer(codeEditor, file!!))
        
        if (file?.extension == "kt") {
            val lang = KotlinLanguage(codeEditor, file!!, binding.topRightNavView, this)
            lang.onAnalyzed = { success ->
                if (success) {
                    ThreadUtils.runOnUiThread {
                        binding.textButtonAnalyzing.gone()
                        binding.topRightLinearLayout.visible()
                        requireContext().animate(binding.topRightLinearLayout)
                    }
                }
            }
            codeEditor.setEditorLanguage(lang)
        } else {
            codeEditor.setEditorLanguage(EmptyLanguage())
        }
    }

    private fun setupSymbols() {
        if (!isSymbolsAdded) {
            val symbolInputView = binding.symbolInputView as SymbolInputView
            symbolInputView.bindEditor(codeEditor)
            symbolInputView.addEditorSymbols(BOTTOM_EDITOR_SYMBOLS, BOTTOM_EDITOR_SYMBOL_INSERT_TEXT)
            isSymbolsAdded = true
        }
    }

    fun layoutLoading(show: Boolean = true) {
        ThreadUtils.runOnUiThread {
            if (show) {
                binding.linearLayoutLoading.visible()
                requireContext().animate(binding.linearLayoutLoading)
            } else {
                binding.linearLayoutLoading.gone()
            }
        }
    }

    fun hideWindows() {
        codeEditor.hideEditorWindows()
        codeEditor.hideAutoCompleteWindow()
    }

    fun isContentModified(): Boolean {
        val currentText = codeEditor.text.toString()
        return contentHash != currentText.getFileHash()
    }

    fun save() {
        try {
            val content = codeEditor.text.toString()
            file?.writeText(content)
            contentHash = content.getFileHash()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        if (::eventReceiver.isInitialized) {
            eventReceiver.unsubscribe()
        }
        if (::codeEditor.isInitialized) {
            hideWindows()
            codeEditor.release()
        }
    }
    
    override fun onResume() {
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedItemId", binding.topRightNavView.selectedItemId)
    }

    private fun setEditorTypefaceText() {
        codeEditor.setTypefaceText(ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono_regular))
    }

    private fun ensureTextmateTheme() {
        if (codeEditor.colorScheme !is TextMateColorScheme) {
            codeEditor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        }
    }

    private fun loadDefaultThemes() {
        val registry = ThemeRegistry.getInstance()
        arrayOf("darcula", "quietlight").forEach { name ->
            val path = "textmate/$name.json"
            registry.loadTheme(ThemeModel(IThemeSource.fromInputStream(FileProviderRegistry.getInstance().tryGetInputStream(path), path, null), name))
        }

        val themeName = when (ComposeApplication.themeProvider.getTheme()) {
            1 -> "quietlight"
            2 -> "darcula"
            else -> if ((resources.configuration.uiMode and 48) != 32) "quietlight" else "darcula"
        }
        registry.setTheme(themeName)
    }
    
    private fun loadDefaultLanguages() {
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
    }

    private fun setUpTextmate() {
        FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(requireContext().assets))
        loadDefaultThemes()
        loadDefaultLanguages()
    }
}