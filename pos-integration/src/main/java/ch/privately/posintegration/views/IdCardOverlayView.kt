package ch.privately.posintegration.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import ch.privately.posintegration.R

/**
 * Custom overlay view that guides users to position their ID card correctly
 * Shows rounded corners, grayed out background, and an ID card icon in the center
 */
class IdCardOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private val overlayColor = Color.parseColor("#FF000000") // Semi-transparent black
    private val cornerColor = Color.WHITE
    private val iconColor = Color.WHITE
    
    private val cornerLength = 60f // Length of corner lines in pixels
    private val cornerStrokeWidth = 4f // Width of corner lines
    private val cornerRadius = 16f // Radius for rounded corners
    
    private val horizontalMarginPercent = 0.05f // 5% margin on each side
    private val verticalMarginPercent = 0.08f // 5% margin on each side
    
    private var overlayRect = RectF()
    private var idCardRect = RectF()
    
    init {
        paint.color = overlayColor
        paint.style = Paint.Style.FILL
        
        cornerPaint.color = cornerColor
        cornerPaint.style = Paint.Style.STROKE
        cornerPaint.strokeWidth = cornerStrokeWidth
        cornerPaint.strokeCap = Paint.Cap.ROUND
        
        iconPaint.color = iconColor
        iconPaint.style = Paint.Style.FILL
        iconPaint.textAlign = Paint.Align.CENTER
        iconPaint.textSize = 48f
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateRectangles(w, h)
    }
    
    private fun calculateRectangles(width: Int, height: Int) {
        // Calculate horizontal margins (5% on each side)

        if (height > width) {
            val horizontalMargin = width * horizontalMarginPercent

            // ID card aspect ratio is typically 3.375:2.125 (credit card standard)
            val idCardAspectRatio = 3.375f / 2.125f

            // Calculate ID card dimensions
            val availableWidth = width - (2 * horizontalMargin)
            val cardWidth = availableWidth
            val cardHeight = cardWidth / idCardAspectRatio

            // Center the card vertically
            val cardLeft = horizontalMargin
            val cardTop = (height - cardHeight) / 2f
            val cardRight = cardLeft + cardWidth
            val cardBottom = cardTop + cardHeight

            idCardRect.set(cardLeft, cardTop, cardRight, cardBottom)
            overlayRect.set(0f, 0f, width.toFloat(), height.toFloat())
        } else {
            val verticalMargin = height * verticalMarginPercent

            // ID card aspect ratio is typically 3.375:2.125 (credit card standard)
            val idCardAspectRatio = 3.375f / 2.125f

            // Calculate ID card dimensions
            val availableHeight = height - (2 * verticalMargin)
            val cardHeight = availableHeight
            val cardWidth = cardHeight * idCardAspectRatio

            // Center the card vertically
            val cardLeft = (width - cardWidth) / 2f
            val cardTop = height * (verticalMarginPercent + 0.04f)
            val cardRight = cardWidth + cardLeft
            val cardBottom = cardTop + cardHeight

            idCardRect.set(cardLeft, cardTop, cardRight, cardBottom)
            overlayRect.set(0f, 0f, width.toFloat(), height.toFloat())
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Create a path for the overlay with a hole for the ID card area
        val overlayPath = Path().apply {
            // Add the full screen rectangle
            addRect(overlayRect, Path.Direction.CW)
            // Subtract the ID card area (creates a hole)
            addRoundRect(idCardRect, cornerRadius, cornerRadius, Path.Direction.CCW)
        }
        
        // Draw the overlay with the hole
        canvas.drawPath(overlayPath, paint)
        
        // Draw the corner guides
        drawCorners(canvas)
        
        // Draw the ID card icon in the center
        drawIdCardIcon(canvas)
    }
    
    private fun drawCorners(canvas: Canvas) {
        val left = idCardRect.left
        val top = idCardRect.top
        val right = idCardRect.right
        val bottom = idCardRect.bottom
        
        // Top-left corner
        canvas.drawLine(left, top + cornerRadius + cornerLength, left, top + cornerRadius, cornerPaint)
        canvas.drawLine(left + cornerRadius, top, left + cornerRadius + cornerLength, top, cornerPaint)
        
        // Top-right corner
        canvas.drawLine(right, top + cornerRadius + cornerLength, right, top + cornerRadius, cornerPaint)
        canvas.drawLine(right - cornerRadius, top, right - cornerRadius - cornerLength, top, cornerPaint)
        
        // Bottom-left corner
        canvas.drawLine(left, bottom - cornerRadius - cornerLength, left, bottom - cornerRadius, cornerPaint)
        canvas.drawLine(left + cornerRadius, bottom, left + cornerRadius + cornerLength, bottom, cornerPaint)
        
        // Bottom-right corner
        canvas.drawLine(right, bottom - cornerRadius - cornerLength, right, bottom - cornerRadius, cornerPaint)
        canvas.drawLine(right - cornerRadius, bottom, right - cornerRadius - cornerLength, bottom, cornerPaint)
    }
    
    private fun drawIdCardIcon(canvas: Canvas) {
        val centerX = idCardRect.centerX()
        val centerY = idCardRect.centerY()
        
        // Try to use the vector drawable first
        try {
            val idCardDrawable = ContextCompat.getDrawable(context, R.drawable.ic_id_card)
            if (idCardDrawable != null) {
                // Create a mutable copy to avoid affecting other instances
                val mutableDrawable = idCardDrawable.mutate()
                
                // Set the bounds for the drawable (make it larger)
                val iconSize = 140
                val left = (centerX - iconSize / 2).toInt()
                val top = (centerY - iconSize / 2).toInt()
                val right = left + iconSize
                val bottom = top + iconSize
                
                mutableDrawable.setBounds(left, top, right, bottom)
                mutableDrawable.draw(canvas)
            } else {
                // Fallback to custom drawing
                drawCustomIdCardIcon(canvas, centerX, centerY)
            }
        } catch (e: Exception) {
            // Fallback to custom drawing if drawable fails
            drawCustomIdCardIcon(canvas, centerX, centerY)
        }
        
        // Draw instruction text
        val instructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iconColor
            textAlign = Paint.Align.CENTER
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
        }
        
        canvas.drawText(
            resources.getString(R.string.id_placment_location),
            centerX,
            centerY + 120f,
            instructionPaint
        )
    }
    
    private fun drawCustomIdCardIcon(canvas: Canvas, centerX: Float, centerY: Float) {
        // Draw a custom ID card icon using basic shapes - NO BACKGROUND - LARGER SIZE
        val iconWidth = 140f
        val iconHeight = 84f
        
        val iconRect = RectF(
            centerX - iconWidth / 2,
            centerY - iconHeight / 2,
            centerX + iconWidth / 2,
            centerY + iconHeight / 2
        )
        
        // Draw card outline ONLY (no fill/background)
        val cardOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iconColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(iconRect, 12f, 12f, cardOutlinePaint)
        
        // Draw person icon (simplified) - scaled up
        val personCenterX = centerX - 35f
        val personCenterY = centerY
        
        // Head (circle) - larger
        canvas.drawCircle(personCenterX, personCenterY - 16f, 12f, iconPaint)
        
        // Body (rounded rectangle) - larger and better proportioned
        val bodyRect = RectF(
            personCenterX - 16f,
            personCenterY - 3f,
            personCenterX + 16f,
            personCenterY + 25f
        )
        canvas.drawRoundRect(bodyRect, 8f, 8f, iconPaint)
        
        // Text lines (representing text on ID) - larger and thicker
        val linesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iconColor
            style = Paint.Style.FILL
        }
        
        // Line 1
        canvas.drawRoundRect(centerX - 5f, centerY - 25f, centerX + 50f, centerY - 21f, 2f, 2f, linesPaint)
        // Line 2  
        canvas.drawRoundRect(centerX - 5f, centerY - 12f, centerX + 42f, centerY - 8f, 2f, 2f, linesPaint)
        // Line 3
        canvas.drawRoundRect(centerX - 5f, centerY + 1f, centerX + 35f, centerY + 5f, 2f, 2f, linesPaint)
        // Line 4
        canvas.drawRoundRect(centerX - 5f, centerY + 14f, centerX + 28f, centerY + 18f, 2f, 2f, linesPaint)
    }
}
