package android.zero.studio.compose.preview.views

import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.zero.studio.compose.preview.R

/**
 * A utility library providing extension functions for Android Views and Contexts to simplify
 * UI animations and visibility management.
 * @param view The target View to animate. Can be null.
 * @param animId The resource ID of the animation to load. Defaults to fade_in.
 */
fun Context.animate(view: View?, animId: Int = R.anim.fade_in) {
    val animation: Animation = AnimationUtils.loadAnimation(this, animId)
    if (view != null) {
        view.startAnimation(animation)
    }
}

/**
 * Sets the view's visibility to GONE (8), making it invisible and removing it from layout space.
 */
fun View.gone() {
    this.visibility = View.GONE
}

/**
 * Sets the view's visibility to INVISIBLE (4), hiding it but keeping its layout space.
 */
fun View.invisible() {
    this.visibility = View.INVISIBLE
}

/**
 * Checks whether the view's visibility is currently set to VISIBLE (0).
 * 
 * @return True if visible, false otherwise.
 */
fun View.isVisible(): Boolean {
    return this.visibility == View.VISIBLE
}

/**
 * Sets the view's visibility to VISIBLE (0).
 */
fun View.visible() {
    this.visibility = View.VISIBLE
}