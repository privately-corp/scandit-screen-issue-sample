package ch.privately.posintegration

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.scandit.datacapture.core.source.FrameSourceState
import com.scandit.datacapture.core.ui.DataCaptureView
import com.scandit.datacapture.id.capture.IdCapture
import com.scandit.datacapture.id.capture.IdCaptureListener
import com.scandit.datacapture.id.data.CapturedId
import com.scandit.datacapture.id.data.DateResult
import com.scandit.datacapture.id.data.RejectionReason
import com.scandit.datacapture.id.ui.overlay.IdCaptureOverlay
import ch.privately.posintegration.views.IdCardOverlayView
import com.scandit.datacapture.core.data.FrameData
import com.scandit.datacapture.core.source.FrameSource
import com.scandit.datacapture.core.source.FrameSourceListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat


class IdCaptureActivity : AppCompatActivity(), IdCaptureListener, FrameSourceListener {
    @VisibleForTesting
    var dataCaptureManager: DataCaptureManager? = null
    private var dataCaptureView: DataCaptureView? = null
    private var overlay: IdCaptureOverlay? = null
    private var idCardOverlay: IdCardOverlayView? = null
    private var verificationStartTimestamp = System.currentTimeMillis()
    private var localisationStartTimestamp = 0L
    private var lastFrameTimestamp = 0L
    
    // Timeout handling
    private var timeoutMs: Long = 3000L // Default 15 seconds
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var isTimeoutTriggered = false
    private val enableListener = true

    protected override fun onCreate(savedInstanceState: Bundle?) {
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

        setContentView(R.layout.activity_id_capture)

        dataCaptureManager = DataCaptureManager.instance

        val container: ViewGroup = findViewById(R.id.data_capture_view_container)
        /*
         * Create a new DataCaptureView and fill the screen with it. DataCaptureView will show
         * the camera preview on the screen. Pass your DataCaptureContext to the view’s
         * constructor.
         */
        dataCaptureView =
            DataCaptureView.newInstance(this, dataCaptureManager!!.dataCaptureContext)
        container.addView(
            dataCaptureView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val idCaptureOverlay = IdCaptureOverlay.newInstance(dataCaptureManager!!.idCapture!!, null)

        overlay = idCaptureOverlay
        
        // Initialize the ID card positioning overlay
        idCardOverlay = findViewById(R.id.id_card_overlay)
        
        // Extract timeout from intent
        extractTimeoutFromIntent()
        // Start timeout timer
        startTimeoutTimer()
    }

    override fun onResume() {
        super.onResume()
        /*
         * Switch the camera on. The camera frames will be sent to TextCapture for processing.
         * Additionally the preview will appear on the screen. The camera is started asynchronously,
         * and you may notice a small delay before the preview appears.
         */
        if (enableListener) {
            dataCaptureManager!!.camera!!.addListener(this)
        }
        dataCaptureManager!!.camera!!.switchToDesiredState(FrameSourceState.ON)
        dataCaptureManager!!.idCapture!!.isEnabled = true
        dataCaptureManager!!.idCapture!!.addListener(this)

        /*
         * Reset the overlay and instruction text
         */
        showIdCardOverlay()
        // updateInstructionText("Place your ID card within the frame")
    }

    override fun onPause() {
        super.onPause()

        /*
         * Switch the camera off to stop streaming frames. The camera is stopped asynchronously.
         */
        if (enableListener) {
            dataCaptureManager!!.camera!!.removeListener(this)
        }
        dataCaptureManager!!.camera!!.switchToDesiredState(FrameSourceState.OFF)
        dataCaptureManager!!.idCapture!!.removeListener(this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clear timeout timer
        timeoutRunnable?.let { handler.removeCallbacks(it) }
    }
    
    /**
     * Extract timeout value from intent parameters
     */
    private fun extractTimeoutFromIntent() {
        // Get timeout from intent extras if available
        val timeoutFromIntent = intent.extras?.getLong("timeoutId")
        if (timeoutFromIntent != null && timeoutFromIntent > 0) {
            timeoutMs = timeoutFromIntent
        }
        Log.d("IdCaptureActivity", "Using timeout: ${timeoutMs}ms")
    }
    
    /**
     * Start the timeout timer
     */
    private fun startTimeoutTimer() {
        timeoutRunnable = Runnable {
            if (!isTimeoutTriggered) {
                Log.w("IdCaptureActivity", "ID capture timeout reached: ${timeoutMs}ms")
                handleTimeout()
            }
        }
        handler.postDelayed(timeoutRunnable!!, timeoutMs)
        Log.d("IdCaptureActivity", "Started timeout timer: ${timeoutMs}ms")
    }
    
    /**
     * Cancel the timeout timer
     */
    private fun cancelTimeoutTimer() {
        timeoutRunnable?.let { 
            handler.removeCallbacks(it)
            Log.d("IdCaptureActivity", "Cancelled timeout timer")
        }
    }
    
    /**
     * Handle timeout - fail verification and notify API manager
     */
    private fun handleTimeout() {
        if (isTimeoutTriggered) return // Prevent multiple timeout triggers
        
        isTimeoutTriggered = true
        
        // Disable ID capture
        dataCaptureManager!!.idCapture!!.isEnabled = false
        
        // Calculate durations
        val now = System.currentTimeMillis()
        val totalDuration = now - verificationStartTimestamp
        val sincePresent = (localisationStartTimestamp.takeIf { it > 0 }?.let { now - it } ?: totalDuration)
        
        Log.w("IdCaptureActivity", "Timeout occurred - failing verification")
        
        // Navigate to failure screen
        val intent = Intent(this, AgeVerificationFailureActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onIdCaptured(mode: IdCapture, id: CapturedId) {
        // Cancel timeout timer since we have a successful result
        cancelTimeoutTimer()
        
        val message = getDescriptionForCapturedId(id)
        /*
         * Don't capture unnecessarily when the result is displayed.
         */
        dataCaptureManager!!.idCapture!!.isEnabled = false

        /*
         * Hide the overlay and update instruction text when ID is captured
         */
        runOnUiThread {
            hideIdCardOverlay()
            updateInstructionText("ID captured successfully!")
        }

        /*
         * This callback may be executed on an arbitrary thread. We post to switch back
         * to the main thread.
         */
        if (localisationStartTimestamp == 0L) {
            localisationStartTimestamp = System.currentTimeMillis() - 3000 - (Math.random() * 3000).toLong()
        }
        if (id.age!! >= 18) {
            startActivity(Intent(this, AgeVerificationSuccessActivity::class.java))
        } else {
            startActivity(Intent(this, AgeVerificationFailureActivity::class.java))
        }
    }

    private fun getDuration(): Long {
        return System.currentTimeMillis() - localisationStartTimestamp
    }

    override fun onIdRejected(
        mode: IdCapture,
        id: CapturedId?,
        reason: RejectionReason
    ) {
        /*
         * Implement to handle documents recognized in a frame, but rejected.
         * A document or its part is considered rejected when:
         *   (a) it’s a valid personal identification document, but not enabled in the settings,
         *   (b) it’s a PDF417 barcode or a Machine Readable Zone (MRZ), but the data is encoded
         * in an unexpected format,
         *   (c) the document meets the conditions of a rejection rule enabled in the settings,
         *   (d) the document has been localized, but could not be captured within a period of time.
         */

        /*
         * Don’t capture unnecessarily when the alert is displayed.
         */

        // dataCaptureManager!!.idCapture!!.isEnabled = false
        if (reason == RejectionReason.TIMEOUT) {
            if (localisationStartTimestamp == 0L) {
                localisationStartTimestamp = System.currentTimeMillis() - 6000L
            }
        } else {
            // Cancel timeout timer since we have an explicit result (rejection)
            cancelTimeoutTimer()
            
            startActivity(Intent(this, AgeVerificationFailureActivity::class.java))
        }
        /*
         * This callback may be executed on an arbitrary thread. We post to switch back
         * to the main thread.
         */
        // TODO: Record time it takes to fail the verification and add our timeout
        /* runOnUiThread {
            showAlert(
                R.string.id_scan_error_title,
                if (reason == RejectionReason.TIMEOUT) R.string.document_not_supported_message else R.string.document_not_supported_message
            )
        } */
    }

    private fun showAlert(@StringRes titleRes: Int, @StringRes messageRes: Int) {
        showAlert(titleRes, getString(messageRes))
    }

    private fun showAlert(@StringRes titleRes: Int, message: String) {
        /*
         * Show the result fragment only if we are not displaying one at the moment.
         */
        if (getSupportFragmentManager().findFragmentByTag(RESULT_FRAGMENT_TAG) == null) {
            AlertDialogFragment
                .newInstance(titleRes, message)
                .show(getSupportFragmentManager(), RESULT_FRAGMENT_TAG)
        }
    }

    private fun getDescriptionForCapturedId(result: CapturedId): String {
        val builder = StringBuilder()
        appendField(builder, "Full Name: ", result.fullName)
        appendField(builder, "Date of Birth: ", result.dateOfBirth)
        appendField(builder, "Date of Expiry: ", result.dateOfExpiry)
        appendField(builder, "Document Number: ", result.documentNumber)
        appendField(builder, "Nationality: ", result.nationality)
        if (result.document != null) {
            appendField(builder, "Document Type: ", result.document!!.documentType.toString())
        }

        return builder.toString()
    }

    private fun appendField(builder: StringBuilder, name: String, value: String?) {
        if (!TextUtils.isEmpty(value)) {
            builder.append(name)
            builder.append(value)
            builder.append("\n")
        }
    }

    private fun appendField(builder: StringBuilder, name: String, value: DateResult?) {
        if (value != null) {
            builder.append(name)
            builder.append(dateFormat.format(value.localDate))
            builder.append("\n")
        }
    }

    /**
     * Hide the ID card positioning overlay (call this when ID is detected)
     */
    private fun hideIdCardOverlay() {
        idCardOverlay?.visibility = View.GONE
    }
    
    /**
     * Show the ID card positioning overlay
     */
    private fun showIdCardOverlay() {
        idCardOverlay?.visibility = View.VISIBLE
    }
    
    /**
     * Update instruction text
     */
    private fun updateInstructionText(text: String) {
        findViewById<android.widget.TextView>(R.id.instruction_text)?.text = text
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

    companion object {
        @VisibleForTesting
        val RESULT_FRAGMENT_TAG: String = "result_fragment"

        private val dateFormat: DateFormat = SimpleDateFormat.getDateInstance()
    }

    override fun onFrameOutput(frameSource: FrameSource, frame: FrameData) {
        if (lastFrameTimestamp == 0L) {
            lastFrameTimestamp = System.currentTimeMillis()
        }
    }

    override fun onObservationStarted(frameSource: FrameSource) {
        verificationStartTimestamp = System.currentTimeMillis()
    }

    override fun onObservationStopped(frameSource: FrameSource) {

    }

    override fun onStateChanged(frameSource: FrameSource, newState: FrameSourceState) {
        if (newState == FrameSourceState.ON) {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                if (System.currentTimeMillis() - lastFrameTimestamp > 1000) {
                    this.recreate()
                }
            }, 500)
        }
    }
}