package android.zero.studio.compose.preview.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity // 恢复使用 AppCompatActivity
import android.zero.studio.compose.preview.databinding.FragmentContainerViewBinding
import android.zero.studio.compose.preview.ui.fragments.EditorFragment
import android.zero.studio.compose.preview.ui.fragments.WorkspaceFragment
import android.zero.studio.compose.preview.utils.FileUtil

/**
 * Main activity for the code editor and preview.
 * 
 * @author android_zero
 */
class EditorActivity : AppCompatActivity() {

    private lateinit var binding: FragmentContainerViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = FragmentContainerViewBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        if (savedInstanceState == null && supportFragmentManager.findFragmentByTag(EditorFragment.TAG) == null) {
            val workspaceFragment = WorkspaceFragment.createEditorFragment(FileUtil.playgroundCode)
            
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainerView.id, workspaceFragment, EditorFragment.TAG)
                .commit()
        }
    }
}