package ch.privately.posintegration

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import ch.privately.posintegration.databinding.ActivityAgeVerificationSuccessBinding

class AgeVerificationSuccessActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAgeVerificationSuccessBinding
    private val autoReturnHandler = Handler(Looper.getMainLooper())
    private var autoReturnRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Keep screen on while this activity is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Set maximum brightness
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 1.0f // Full brightness (0.0f to 1.0f)
        window.attributes = layoutParams
        
        // Enable full-screen immersive mode
        enableFullScreen()

        binding = ActivityAgeVerificationSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupClickListeners()
        startAutoReturnTimer()
    }
    
    private fun setupUI() {
        binding.btnContinue.visibility = if (FeatureFlags.SHOW_BUTTONS) View.VISIBLE else View.GONE
    }
    
    private fun setupClickListeners() {
        binding.btnContinue.setOnClickListener {
            cancelAutoReturnTimer()
            returnToPrivacyScreen()
        }
    }
    
    private fun startAutoReturnTimer() {
        autoReturnRunnable = Runnable {
            returnToPrivacyScreen()
        }
        autoReturnHandler.postDelayed(autoReturnRunnable!!, 5000) // 5 seconds
    }
    
    private fun cancelAutoReturnTimer() {
        autoReturnRunnable?.let { runnable ->
            autoReturnHandler.removeCallbacks(runnable)
            autoReturnRunnable = null
        }
    }
    
    private fun returnToPrivacyScreen() {
        // Navigate back to privacy notice screen and clear activity stack
        val intent = Intent(this, ChooseVerificationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cancelAutoReturnTimer()
    }
    
    override fun onPause() {
        super.onPause()
        // Cancel timer if user navigates away (e.g., home button)
        cancelAutoReturnTimer()
    }
    
    override fun onResume() {
        super.onResume()
        // Restart timer if activity comes back into focus
        if (autoReturnRunnable == null) {
            startAutoReturnTimer()
        }
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
