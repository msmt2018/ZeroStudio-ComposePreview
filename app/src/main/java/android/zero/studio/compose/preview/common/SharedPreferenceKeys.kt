package android.zero.studio.compose.preview.common

/**
 * A singleton object holding constant keys for SharedPreferences.
 * Using constants helps avoid typos and centralizes key management.
 *
 * @author android_zero
 */
object SharedPreferenceKeys {
    /** Key for storing the application's theme setting. */
    const val APP_THEME: String = "app_theme"

    /** Key for storing the user-selected theme index. */
    const val SELECTED_THEME: String = "selected_theme"
}