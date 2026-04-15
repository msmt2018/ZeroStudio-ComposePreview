package android.zero.studio.compose.preview.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import java.io.File

/**
 * Base fragment for workspace-related views, providing common file access.
 * 
 * @author android_zero
 */
abstract class WorkspaceFragment : Fragment() {

    /** The file currently being edited in this fragment. */
    val file: File? by lazy {
        val serializable = arguments?.getSerializable("current_file")
        if (serializable is File) serializable else null
    }

    /**
     * Computes a hash code based on the file path.
     */
    fun getHashCode(): Long {
        return file?.hashCode()?.toLong() ?: 0L
    }

    companion object {
        /**
         * Creates a new instance of [EditorFragment] for the given file.
         * 
         * @param file The Kotlin file to open in the editor.
         * @return A configured [WorkspaceFragment].
         */
        fun createEditorFragment(file: File): WorkspaceFragment {
            return EditorFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("current_file", file)
                }
            }
        }
    }
}