package ch.privately.posintegration

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import ch.privately.posintegration.databinding.ActivityAgeVerificationFailureBinding

class AgeVerificationFailureActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAgeVerificationFailureBinding
    private val autoReturnHandler = Handler(Looper.getMainLooper())
    private var autoReturnRunnable: Runnable? = null
    private var isFullFlow: Boolean = false
    
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

        binding = ActivityAgeVerificationFailureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isFullFlow = (intent.extras?.getInt("isFullFlow") ?: 0) == 1
        setupUI()
        setupClickListeners()
        startAutoReturnTimer()
    }
    
    private fun setupUI() {
        binding.tvInstruction.visibility = if (isFullFlow) View.GONE else View.VISIBLE
        binding.btnIdScan.visibility = if (isFullFlow) View.VISIBLE else View.GONE
    }
    
    private fun setupClickListeners() {
        binding.btnIdScan.setOnClickListener {
            // Navigate to ID scanning
            cancelAutoReturnTimer()
            proceedToNextScreen()
        }
    }
    
    private fun startAutoReturnTimer() {
        autoReturnRunnable = Runnable {
            proceedToNextScreen()
        }
        autoReturnHandler.postDelayed(autoReturnRunnable!!, 2000) // 5 seconds
    }
    
    private fun cancelAutoReturnTimer() {
        autoReturnRunnable?.let { runnable ->
            autoReturnHandler.removeCallbacks(runnable)
            autoReturnRunnable = null
        }
    }
    
    private fun proceedToNextScreen() {
        // Navigate back to privacy notice screen and clear activity stack
        if (isFullFlow) {
            val timeoutId = intent.extras?.getLong("timeoutId")
            Log.e("MMOSADDD", "Timeout ID: " + timeoutId)
            val intent = Intent(this, IdCaptureActivity::class.java)
            if (timeoutId != null) {
                val bundle = Bundle()
                bundle.putLong("timeoutId", timeoutId)
                intent.putExtras(bundle)
            }
            startActivity(intent)
        } else {
            val intent = Intent(this, ChooseVerificationActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
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
