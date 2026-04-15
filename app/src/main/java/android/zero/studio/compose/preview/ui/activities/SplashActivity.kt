package android.zero.studio.compose.preview.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * Entry activity that handles the splash screen and navigates to the main EditorActivity.
 * 
 *
 * @author android_zero
 */
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen 必须在 super.onCreate 之前调用
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
    
        splashScreen.setKeepOnScreenCondition {
            // Restoration of SplashActivity$$ExternalSyntheticLambda0
            true
        }
        
        // 启动主编辑页面
        val intent = Intent(this, EditorActivity::class.java)
        startActivity(intent)
        
        // 立即关闭闪屏页
        finish()
    }
}