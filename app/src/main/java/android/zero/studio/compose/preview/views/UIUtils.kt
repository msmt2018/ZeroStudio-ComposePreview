package android.zero.studio.compose.preview.views

import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.zero.studio.compose.preview.R

/**
 * A utility library providing extension functions for Android Views and Contexts to simplify
 * UI animations and visibility management.
 * 
 * 工作流程线路图:
 * 1. [animate] -> 加载 XML 动画资源 -> 应用于指定的 View 实例。
 * 2. [visible]/[invisible]/[gone] -> 直接修改 View 的 visibility 属性。
 * 3. [isVisible] -> 检查并返回当前 View 的可见性状态。
 * 
 * 上下文关系:
 * - 用于 [android.zero.studio.compose.preview.ui.fragments.EditorFragment] 中的布局切换和加载动画。
 * - 依赖于 resources 模块中的动画资源定义。
 * 
 * @author android_zero
 */

/**
 * Animates a given view using the specified animation resource.
 * 
 * 逻辑还原自 animate$default:
 * - 接收目标 View 和可选的动画 ID。
 * - 如果未提供 animId，则默认使用 R.anim.fade_in。
 * - 通过 AnimationUtils 加载并启动动画。
 *
 * @param view The target View to animate. Can be null.
 * @param animId The resource ID of the animation to load. Defaults to fade_in.
 */
fun Context.animate(view: View?, animId: Int = R.anim.fade_in) {
    // 1:1 Restoration of animation loading logic
    val animation: Animation = AnimationUtils.loadAnimation(this, animId)
    if (view != null) {
        view.startAnimation(animation)
    }
}

/**
 * Sets the view's visibility to GONE (8), making it invisible and removing it from layout space.
 */
fun View.gone() {
    // 1:1 Restoration of setVisibility(8)
    this.visibility = View.GONE
}

/**
 * Sets the view's visibility to INVISIBLE (4), hiding it but keeping its layout space.
 */
fun View.invisible() {
    // 1:1 Restoration of setVisibility(4)
    this.visibility = View.INVISIBLE
}

/**
 * Checks whether the view's visibility is currently set to VISIBLE (0).
 * 
 * @return True if visible, false otherwise.
 */
fun View.isVisible(): Boolean {
    // 1:1 Restoration of getVisibility() == 0 check
    return this.visibility == View.VISIBLE
}

/**
 * Sets the view's visibility to VISIBLE (0).
 */
fun View.visible() {
    // 1:1 Restoration of setVisibility(0)
    this.visibility = View.VISIBLE
}