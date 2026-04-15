package android.zero.studio.compose.preview.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Shared ViewModel for managing the editor state, including navigation and analysis status.
 * 
 * @author android_zero
 */
class EditorViewModel : ViewModel() {
    
    /** Current position of the top navigation view. */
    val top_nav_position: MutableLiveData<Int> = MutableLiveData(0)
    
    /** Status of the code analysis process. */
    val analyzed: MutableLiveData<Boolean> = MutableLiveData(false)

    /**
     * Updates the top navigation position if it differs from the current value.
     * 
     * @param pos The new position index.
     */
    fun setTopNavPosition(pos: Int) {
        val current = top_nav_position.value
        if (current == null || current != pos) {
            top_nav_position.value = pos
        }
    }
}