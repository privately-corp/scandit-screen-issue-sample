package ch.privately.posintegration

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import ch.privately.posintegration.databinding.ActivityChooseVerificationBinding

class ChooseVerificationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChooseVerificationBinding
    private var isFullFlow: Int = 0
    private var timeoutFaceMs: Long = 5000L // Default 10 seconds
    private var timeoutIdMs: Long = 5000L // Default 15 seconds
    
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

        binding = ActivityChooseVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isFullFlow = intent.extras?.getInt("isFullFlow") ?: 1
        
        // Extract timeout parameters from intent
        val timeoutFaceFromIntent = intent.extras?.getLong("timeoutFace")
        if (timeoutFaceFromIntent != null && timeoutFaceFromIntent > 0) {
            timeoutFaceMs = timeoutFaceFromIntent
        }
        val timeoutIdFromIntent = intent.extras?.getLong("timeoutId")
        if (timeoutIdFromIntent != null && timeoutIdFromIntent > 0) {
            timeoutIdMs = timeoutIdFromIntent
        }
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // UI is already set up in the layout file
    }
    
    private fun setupClickListeners() {
        binding.btnIdScan.setOnClickListener {
            // Navigate to ID scanning
            val intent = Intent(this, IdCaptureActivity::class.java)
            val bundle = Bundle()
            bundle.putLong("timeoutId", timeoutIdMs) // Pass ID timeout to IdCaptureActivity
            intent.putExtras(bundle)
            startActivity(intent)
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
