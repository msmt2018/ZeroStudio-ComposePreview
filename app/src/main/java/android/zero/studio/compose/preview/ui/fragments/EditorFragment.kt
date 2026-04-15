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
 * 
 * 工作流程线路图:
 * 1. 视图绑定与初始化 -> 初始化 Sora Editor、TextMate 语法高亮。
 * 2. 绑定 KotlinLanguage -> 解析 *.kt 文件并建立 Kotlin 编译环境。
 * 3. 并发事件监听 -> 经由 DiagnosticAnalyzer 监听代码输入，执行带防抖的后台增量编译。
 * 4. DEX 热重载 (loadPreview) -> 利用带时间戳的隔离 Dex 文件绕过虚拟机缓存，将产物交给 DynamicPreviewLoader 渲染。
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
        this.codeEditor = binding.codeEditor
        return binding.root
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
                    if (!isPreviewLoaded) {
                        loadPreview()
                        isPreviewLoaded = true
                    }
                    true
                }
                R.id.split -> {
                    showSplit()
                    if (!isPreviewLoaded) {
                        loadPreview()
                        isPreviewLoaded = true
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setContentChangeEvent() {
        codeEditor.subscribeAlways(ContentChangeEvent::class.java) { 
            doAutoSave() 
        }
    }
    
    private fun doAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = lifecycleScope.launch {
            delay(1000)
            if (isContentModified()) {
                save()
            }
        }
    }

    /**
     * 判断当前编辑器内容是否相较于文件原始状态发生了改变。
     * 
     * 工作流程: 获取编辑器最新文本 -> 计算 Hash -> 与初始读取文件时的 contentHash 进行比对。
     */
    fun isContentModified(): Boolean {
        val currentContent = codeEditor.text.toString()
        val currentContentHash = currentContent.getFileHash()
        return this.contentHash != currentContentHash
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
     * @param name Compose 函数的短名称
     * @param clazz Compose 函数所在的完全限定类名
     */
    private fun loadComposePreview(name: String, clazz: String) {
        val compiledDexFile = FileUtil.classesJarDex
        if (!compiledDexFile.exists()) {
            layoutLoading(show = false)
            return
        }

        // 核心修复：准备具备时间戳签名的独立 Dex 文件，强制突破 ClassLoader 缓存
        val loadableDexFile = prepareLoadableDexFile(compiledDexFile)

        val classLoader = MultipleDexClassLoader(null, requireContext().classLoader)
        classLoader.loadDex(loadableDexFile)

        val dynamicLoader = DynamicPreviewLoader(classLoader, clazz, name)
        
        ThreadUtils.runOnUiThread {
            // 清理旧状态防内存泄漏
            binding.composeView.disposeComposition()
            binding.composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            val isDark = when (ComposeApplication.themeProvider.getTheme()) {
                1 -> false
                2 -> true
                else -> (resources.configuration.uiMode and 48) == 32
            }

            layoutLoading(show = false) // 关闭加载动画
            
            // 挂载动态渲染树
            binding.composeView.setContent {
                AppTheme(darkTheme = isDark, dynamicColor = false) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        dynamicLoader.Render()
                    }
                }
            }
        }
    }

    /**
     * 准备并切换预览布局。
     */
    fun loadPreview() {
        lifecycleScope.launch {
            val functions = withContext(Dispatchers.IO) { parsePreviewFunctions() }

            withContext(Dispatchers.Main) {
                when {
                    functions.isNullOrEmpty() -> {
                        layoutLoading(show = false)
                        showOnly(listOf(binding.linearLayoutInitializing, binding.linearLayoutComposePreview, binding.linearLayoutMultiplePreview), binding.linearLayoutNoPreview, true)
                    }
                    functions.size > 1 -> {
                        layoutLoading(show = false)
                        showOnly(listOf(binding.linearLayoutInitializing, binding.linearLayoutComposePreview, binding.linearLayoutNoPreview), binding.linearLayoutMultiplePreview, true)
                    }
                    else -> {
                        // 识别到唯一预览目标，执行动态挂载
                        showOnly(listOf(binding.linearLayoutInitializing, binding.linearLayoutNoPreview, binding.linearLayoutMultiplePreview), binding.linearLayoutComposePreview, true)
                        val fn = functions[0]
                        loadComposePreview(fn.name!!, fn.className())
                    }
                }
            }
        }
    }
    
    /**
     * Android 14+ 限制并且 ClassLoader 默认会缓存 Dex，因此每次覆盖编译后，
     * 我们必须通过创建带时间戳的副本，欺骗 ClassLoader 加载全新代码。
     */
    private fun prepareLoadableDexFile(sourceDex: File): File {
        val cacheDir = File(requireContext().codeCacheDir, "preview-dex")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        // 防溢出：清理过期的 dex
        cacheDir.listFiles()?.forEach { it.delete() }

        // 使用毫秒时间戳保证文件绝对唯一
        val targetDex = File(cacheDir, "classes_${System.currentTimeMillis()}.dex.zip")
        sourceDex.copyTo(targetDex, overwrite = true)
        
        targetDex.setReadable(true, false)
        targetDex.setWritable(false, false) // 适应 API 34+ 动态加载只读安全规范
        
        return targetDex
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
        eventReceiver = codeEditor.subscribeEvent(ContentChangeEvent::class.java, DiagnosticAnalyzer(codeEditor, file!!, this))
        
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
     * 更严谨的 UI 加载状态指示。
     * @param show 是否显示加载。
     * @param isHotReload 标志当前是初始化加载，还是用户在键入引发的热重载增量编译。
     */
    fun layoutLoading(show: Boolean = true, isHotReload: Boolean = false) {
        ThreadUtils.runOnUiThread {
            if (show) {
                if (isHotReload) {
                    // 热编译时，只在顶部出现 Loading 小提示，不遮挡画板
                    binding.linearLayoutLoading.visible()
                } else {
                    // 初次冷启动，遮蔽一切，全屏展示 Initializing...
                    val previewLayouts = listOf(
                        binding.linearLayoutNoPreview,
                        binding.linearLayoutMultiplePreview,
                        binding.linearLayoutComposePreview
                    )
                    previewLayouts.forEach { it.gone() }
                    binding.linearLayoutInitializing.visible()
                    requireContext().animate(binding.linearLayoutInitializing)
                }
            } else {
                binding.linearLayoutInitializing.gone()
                binding.linearLayoutLoading.gone()
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