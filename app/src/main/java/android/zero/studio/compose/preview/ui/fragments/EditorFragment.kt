package android.zero.studio.compose.preview.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

/**
 * 核心代码编辑器 Fragment。
 * 该类作为 IDE 的中心枢纽，协调代码编辑 (Sora Editor)、静态分析 (Kotlin Compiler)、
 * 字节码转换 (D8) 以及动态渲染 (Compose Runtime)。
 *
 * 工作流程线路图:
 * 1. 初始化编辑器 -> 加载 TextMate 语法与主题。
 * 2. 绑定 KotlinLanguage -> 启动编译器后端环境。
 * 3. 监听文本变更 -> 触发 DiagnosticAnalyzer 进行后台增量编译 (*.kt -> *.class -> *.dex)。
 * 4. 编译成功后 -> 收到分析完成信号 -> 重载 ClassLoader -> 渲染 Compose 画布。
 * 
 * @author android_zero
 */
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
        release()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViews()
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
        binding.toolBarEditor.title = file?.name ?: "Editor"
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
                    loadPreview()
                    true
                }
                R.id.split -> {
                    showSplit()
                    loadPreview()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setContentChangeEvent() {
        // 自动保存逻辑：当文本变化 1 秒后且无新输入时执行保存，减少 IO 频率
        codeEditor.subscribeAlways(ContentChangeEvent::class.java) { 
            doAutoSave() 
        }
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

    /**
     * 动态加载并渲染预览。
     * 使用隔离的 MultipleDexClassLoader 加载 classes.dex.zip。
     */
    private fun loadComposePreview(name: String, clazz: String) {
        val dexFile = FileUtil.classesJarDex
        if (!dexFile.exists()) {
            layoutLoading(false)
            return
        }

        // ★ 核心重构点：为了支持热重载（Live-Reload），必须销毁旧的 ClassLoader。
        // 每次预览都实例化一个新的 MultipleDexClassLoader 实例，Parent 设置为系统的 ClassLoader。
        val classLoader = MultipleDexClassLoader(null, requireContext().classLoader)
        classLoader.loadDex(dexFile)
        
        val dynamicLoader = DynamicPreviewLoader(classLoader, clazz, name)
        
        ThreadUtils.runOnUiThread {
            // 彻底清理 ComposeView 的上一次组合状态，释放内存
            binding.composeView.disposeComposition()
            binding.composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            val isDark = when (ComposeApplication.themeProvider.getTheme()) {
                1 -> false
                2 -> true
                else -> (resources.configuration.uiMode and 48) == 32
            }

            layoutLoading(false)
            // 在画布上渲染动态加载的 @Composable 函数
            binding.composeView.setContent {
                AppTheme(darkTheme = isDark) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        dynamicLoader.Render()
                    }
                }
            }
            isPreviewLoaded = true
        }
    }

      fun loadPreview() {
        lifecycleScope.launch {
            // 解析 PSI 获取所有被 @Preview 标记的函数
            val functions = withContext(Dispatchers.IO) { parsePreviewFunctions() }

            when {
                functions.isNullOrEmpty() -> {
                    layoutLoading(false)
                    showOnly(listOf(binding.linearLayoutInitializing, binding.linearLayoutComposePreview, binding.linearLayoutMultiplePreview), binding.linearLayoutNoPreview, true)
                }
                functions.size > 1 -> {
                    layoutLoading(false)
                    showOnly(listOf(binding.linearLayoutInitializing, binding.linearLayoutComposePreview, binding.linearLayoutNoPreview), binding.linearLayoutMultiplePreview, true)
                }
                else -> {
                    // 找到了合法的 Preview，准备渲染。注意此时可能还在编译，Initialization 会由 KotlinLanguage 触发。
                    val fn = functions[0]
                    loadComposePreview(fn.name!!, fn.className())
                }
            }
        }
    }
    
    /**
     * 控制预览占位布局的切换。
     */
    private fun showOnly(layouts: List<LinearLayout>, target: View, animate: Boolean) {
        layouts.forEach { it.gone() }
        target.visible()
        if (animate) {
            requireContext().animate(target)
        }
    }

    /**
     * 通过分析当前的编辑器文本解析出预览函数列表。
     */
    private fun parsePreviewFunctions(): List<KtNamedFunction>? {
        val lang = codeEditor.editorLanguage as? KotlinLanguage ?: return null
        val cache = lang.kotlinEnvironment.cache
        // 确保使用最新的编辑器文本进行解析
        val psiFile = cache.getOrUpdate(file!!.name, codeEditor.text.toString())
        return PreviewComposableFunctionParser.initialize(psiFile.ktFile).parse()
    }
    
    // --- 布局模式切换逻辑 ---

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
        // 订阅 DiagnosticAnalyzer。每当内容改变时，它都会调用 KotlinLanguage.analyze() 触发增量编译。
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

     /**
     * 更新布局加载状态。
     * @param show 为 true 时显示 Initialization 状态。
     */
    fun layoutLoading(show: Boolean = true) {
        ThreadUtils.runOnUiThread {
            val previewLayouts = listOf(
                binding.linearLayoutNoPreview,
                binding.linearLayoutMultiplePreview,
                binding.linearLayoutComposePreview
            )
            
            if (show) {
                // 隐藏所有预览层，显示初始化层
                previewLayouts.forEach { it.gone() }
                binding.linearLayoutInitializing.visible()
                requireContext().animate(binding.linearLayoutInitializing)
            } else {
                // 隐藏初始化层
                binding.linearLayoutInitializing.gone()
            }
        }
    }

    fun hideWindows() {
        codeEditor.hideEditorWindows()
        codeEditor.hideAutoCompleteWindow()
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

    /**
     * 释放所有持有资源，防止内存泄露。
     */
    fun release() {
        if (::eventReceiver.isInitialized) {
            eventReceiver.unsubscribe()
        }
        if (::codeEditor.isInitialized) {
            hideWindows()
            codeEditor.release()
        }
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