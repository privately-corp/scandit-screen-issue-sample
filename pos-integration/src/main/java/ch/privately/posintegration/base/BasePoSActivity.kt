package ch.privately.posintegration.base

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * Base activity that provides full kiosk experience for all PoS integration activities
 * Features: Screen always on, maximum brightness, full-screen immersive mode
 * Extend this instead of AppCompatActivity for automatic kiosk behavior
 */
abstract class BasePoSActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide action bar for kiosk-like experience
        supportActionBar?.hide()
        
        // Keep screen on for all PoS activities
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Set maximum brightness
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 1.0f // Full brightness (0.0f to 1.0f)
        window.attributes = layoutParams
        
        // Enable full-screen immersive mode
        enableFullScreen()
    }
    
    override fun onResume() {
        super.onResume()
        // Ensure all flags are still set when activity resumes
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableFullScreen()
    }
    
    /**
     * Enable full-screen immersive mode for kiosk experience
     */
    private fun enableFullScreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-enable full-screen when window regains focus
            enableFullScreen()
        }
    }
}
