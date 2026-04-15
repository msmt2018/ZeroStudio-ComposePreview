package android.zero.studio.compose.preview.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Represents a family of related colors used in the theme, typically consisting
 * of a main color, a color for content on top of it, a container color, and a
 * color for content on top of the container.
 *
 * @property color The main color role.
 * @property onColor The color for content that appears "on" [color].
 * @property colorContainer The color for a container that holds elements with the [color] role.
 * @property onColorContainer The color for content that appears "on" [colorContainer].
 *
 * @author android_zero
 */
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)