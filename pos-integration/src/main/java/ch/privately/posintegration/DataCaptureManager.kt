package ch.privately.posintegration

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.scandit.datacapture.core.capture.DataCaptureContext
import com.scandit.datacapture.core.source.Camera
import com.scandit.datacapture.core.source.CameraPosition
import com.scandit.datacapture.core.source.VideoResolution
import com.scandit.datacapture.id.capture.DriverLicense
import com.scandit.datacapture.id.capture.FullDocumentScanner
import com.scandit.datacapture.id.capture.IdCapture
import com.scandit.datacapture.id.capture.IdCaptureDocument
import com.scandit.datacapture.id.capture.IdCaptureSettings
import com.scandit.datacapture.id.capture.IdCard
import com.scandit.datacapture.id.capture.Passport
import com.scandit.datacapture.id.capture.SingleSideScanner
import com.scandit.datacapture.id.data.IdCaptureRegion
import java.util.Arrays


/*
* Initializes DataCapture.
*/
class DataCaptureManager private constructor() {
    val dataCaptureContext: DataCaptureContext
    var camera: Camera? = null
        private set
    var idCapture: IdCapture? = null
        private set

    init {
        /*
         * Create DataCaptureContext using your license key.
         */
        dataCaptureContext = DataCaptureContext.forLicenseKey(SCANDIT_LICENSE_KEY)

        initCamera()
        initIdCapture()
    }

    private fun initCamera() {
        /*
         * Set the device's default camera as DataCaptureContext's FrameSource. DataCaptureContext
         * passes the frames from it's FrameSource to the added modes to perform capture or
         * tracking.
         *
         * Since we are going to perform TextCapture in this sample, we initiate the camera with
         * the recommended settings for this mode.
         */
        val cameraSettings = IdCapture.createRecommendedCameraSettings()
        cameraSettings.preferredResolution = VideoResolution.FULL_HD
        cameraSettings.shouldPreferSmoothAutoFocus = false
        cameraSettings.zoomFactor = 1.0f

        camera = Camera.getCamera(CameraPosition.USER_FACING, cameraSettings)

        checkNotNull(camera) {
            Log.e("MMOSADDD", "Failed to init camera!!")
            "Failed to init camera!"
        }

        dataCaptureContext.setFrameSource(camera)
    }

    private fun initIdCapture() {
        /*
         * Create a mode responsible for recognizing documents. This mode is automatically added
         * to the passed DataCaptureContext.
         */
        val settings = IdCaptureSettings()

        // Recognize national ID cards, driver's licenses and passports.
        settings.acceptedDocuments = ACCEPTED_DOCUMENTS
        // settings.scannerType = SingleSideScanner(visualInspectionZone = true)
        settings.scannerType = SingleSideScanner(machineReadableZone = true , visualInspectionZone = true)

        idCapture = IdCapture.forDataCaptureContext(dataCaptureContext, settings)
    }

    companion object {
        // Replace SCANDIT_LICENSE_KEY with your licence key
        @VisibleForTesting
        var SCANDIT_LICENSE_KEY: String = ""

        private var INSTANCE: DataCaptureManager? = null

        private val ACCEPTED_DOCUMENTS: List<IdCaptureDocument> = Arrays.asList(
            IdCard(IdCaptureRegion.ANY),
            DriverLicense(IdCaptureRegion.ANY),
            Passport(IdCaptureRegion.ANY)
        )

        val instance: DataCaptureManager?
            get() {
                if (INSTANCE == null) {
                    INSTANCE = DataCaptureManager()
                }

                return INSTANCE
            }
    }
}