package android.zero.studio.compose.preview.provider.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import android.zero.studio.compose.preview.R
import android.zero.studio.compose.preview.common.SharedPreferenceKeys

/**
 * Provides theme-related functionalities, such as retrieving the current theme
 * setting from SharedPreferences.
 *
 * @param context The application context used to access resources and preferences.
 * @author android_zero
 */
class ThemeProvider(private val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * Gets the currently selected theme mode from SharedPreferences.
     *
     * @return The theme mode, corresponding to [AppCompatDelegate.MODE_NIGHT_*] constants.
     */
    fun getTheme(): Int {
        val themeEntries = context.resources.getStringArray(R.array.themes_entries)
        val selectedThemeIndex = sharedPreferences.getInt(SharedPreferenceKeys.SELECTED_THEME, 2) // Default to "System Default"
        
        // Ensure index is within bounds
        val safeIndex = selectedThemeIndex.coerceIn(themeEntries.indices)
        val selectedThemeName = themeEntries[safeIndex]
        
        return getTheme(selectedThemeName)
    }

    /**
     * Converts a theme name string to its corresponding [AppCompatDelegate.MODE_NIGHT_*] constant.
     *
     * @param selectedTheme The name of the theme (e.g., "Light", "Dark").
     * @return The corresponding theme mode constant.
     */
    fun getTheme(selectedTheme: String): Int {
        return when (selectedTheme) {
            context.getString(R.string.light) -> AppCompatDelegate.MODE_NIGHT_NO
            context.getString(R.string.dark) -> AppCompatDelegate.MODE_NIGHT_YES
            context.getString(R.string.system_default) -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}