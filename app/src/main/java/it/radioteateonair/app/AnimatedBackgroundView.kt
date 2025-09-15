package it.radioteateonair.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*

/**
 * Animated background view that displays multiple layered wave animations.
 *
 * This view creates a dynamic water-like effect with 10 different wave layers,
 * each with unique animation speeds, amplitudes, and opacity levels. The waves
 * are responsive to different screen sizes and create a fluid, continuous animation
 * suitable for radio/audio streaming applications.
 *
 * @author lrnz
 * @since 13
 */
class AnimatedBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Linear gradient for background (currently set to null for transparency)
     */
    private var gradient: LinearGradient? = null

    /**
     * ValueAnimator instance that drives the continuous wave animation
     */
    private var animator: ValueAnimator? = null

    // Wave offset parameters for 10 different wave layers
    /** Wave offset for layer 1 (30°/second rotation speed) */
    private var waveOffset1 = 0f
    /** Wave offset for layer 2 (45°/second rotation speed) */
    private var waveOffset2 = 0f
    /** Wave offset for layer 3 (20°/second rotation speed) */
    private var waveOffset3 = 0f
    /** Wave offset for layer 4 (60°/second rotation speed) */
    private var waveOffset4 = 0f
    /** Wave offset for layer 5 (15°/second rotation speed) */
    private var waveOffset5 = 0f
    /** Wave offset for layer 6 (35°/second rotation speed) */
    private var waveOffset6 = 0f
    /** Wave offset for layer 7 (50°/second rotation speed) */
    private var waveOffset7 = 0f
    /** Wave offset for layer 8 (25°/second rotation speed) */
    private var waveOffset8 = 0f
    /** Wave offset for layer 9 (40°/second rotation speed) */
    private var waveOffset9 = 0f
    /** Wave offset for layer 10 (55°/second rotation speed) */
    private var waveOffset10 = 0f

    init {
        setupAnimator()
    }

    /**
     * Initializes and configures the ValueAnimator for continuous wave animation.
     *
     * Creates a time-based animator that runs indefinitely and updates wave offsets
     * for all 10 layers based on system time. Each layer has a different rotation
     * speed to create complex, non-repeating wave patterns.
     *
     * Animation speeds per layer (degrees per second):
     * - Layer 1: 30°/s, Layer 2: 45°/s, Layer 3: 20°/s
     * - Layer 4: 60°/s, Layer 5: 15°/s, Layer 6: 35°/s
     * - Layer 7: 50°/s, Layer 8: 25°/s, Layer 9: 40°/s, Layer 10: 55°/s
     */
    private fun setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, Float.MAX_VALUE).apply {
            duration = Long.MAX_VALUE // Infinite duration
            repeatCount = 0 // No repeats needed
            interpolator = LinearInterpolator()

            addUpdateListener { _ ->
                val currentTime = System.currentTimeMillis()

                // Use system time for truly continuous animation
                val timeSeconds = currentTime / 1000.0

                waveOffset1 = (timeSeconds * 30.0).toFloat()   // 30 degrees per second
                waveOffset2 = (timeSeconds * 45.0).toFloat()   // 45 degrees per second
                waveOffset3 = (timeSeconds * 20.0).toFloat()   // 20 degrees per second
                waveOffset4 = (timeSeconds * 60.0).toFloat()   // 60 degrees per second
                waveOffset5 = (timeSeconds * 15.0).toFloat()   // 15 degrees per second
                waveOffset6 = (timeSeconds * 35.0).toFloat()   // 35 degrees per second
                waveOffset7 = (timeSeconds * 50.0).toFloat()   // 50 degrees per second
                waveOffset8 = (timeSeconds * 25.0).toFloat()   // 25 degrees per second
                waveOffset9 = (timeSeconds * 40.0).toFloat()   // 40 degrees per second
                waveOffset10 = (timeSeconds * 55.0).toFloat()  // 55 degrees per second

                invalidate()
            }
            start()
        }
    }

    /**
     * Called when the view size changes. Updates the gradient configuration.
     *
     * @param w Current width of the view
     * @param h Current height of the view
     * @param oldw Previous width of the view
     * @param oldh Previous height of the view
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()
    }

    /**
     * Updates the background gradient configuration.
     * Currently sets gradient to null to maintain transparency and allow
     * underlying content (like background images) to show through.
     */
    private fun updateGradient() {
        if (width <= 0 || height <= 0) return

        // NO GRADIENT - completely transparent background to show image
        gradient = null
    }

    /**
     * Main drawing method that renders the animated wave overlay.
     * Skips gradient drawing and only renders the wave layers for transparency.
     *
     * @param canvas The canvas to draw on
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Skip gradient drawing - only draw waves
        drawWaveOverlay(canvas)
    }

    /**
     * Draws all 10 wave layers with responsive amplitude scaling.
     *
     * Creates a sophisticated multi-layered wave effect by drawing 10 different
     * wave layers, each with unique properties. The amplitude is automatically
     * scaled based on screen size to ensure optimal visual appearance across
     * different devices.
     *
     * Screen size scaling:
     * - Phones (< 800px): 0.8x amplitude
     * - Tablets (800-1200px): 1.0x amplitude
     * - Large tablets (> 1200px): 1.2x amplitude
     *
     * @param canvas The canvas to draw the waves on
     */
    private fun drawWaveOverlay(canvas: Canvas) {
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Responsive amplitude based on screen size
        val amplitudeMultiplier = when {
            width >= 1200 -> 1.2f // Larger waves for big tablets
            width >= 800 -> 1.0f  // Normal waves for tablets
            else -> 0.8f          // Smaller waves for phones
        }

        drawWaveLayer(canvas, overlayPaint, 1, 0.25f * amplitudeMultiplier, 0.008f, Color.argb(60, 0, 180, 0))
        drawWaveLayer(canvas, overlayPaint, 2, 0.22f * amplitudeMultiplier, 0.012f, Color.argb(45, 0, 150, 0))
        drawWaveLayer(canvas, overlayPaint, 3, 0.28f * amplitudeMultiplier, 0.006f, Color.argb(55, 0, 200, 0))
        drawWaveLayer(canvas, overlayPaint, 4, 0.20f * amplitudeMultiplier, 0.015f, Color.argb(40, 0, 120, 0))
        drawWaveLayer(canvas, overlayPaint, 5, 0.24f * amplitudeMultiplier, 0.009f, Color.argb(65, 0, 160, 0))
        drawWaveLayer(canvas, overlayPaint, 6, 0.26f * amplitudeMultiplier, 0.011f, Color.argb(50, 0, 140, 0))
        drawWaveLayer(canvas, overlayPaint, 7, 0.23f * amplitudeMultiplier, 0.007f, Color.argb(58, 0, 190, 0))
        drawWaveLayer(canvas, overlayPaint, 8, 0.21f * amplitudeMultiplier, 0.014f, Color.argb(42, 0, 110, 0))
        drawWaveLayer(canvas, overlayPaint, 9, 0.27f * amplitudeMultiplier, 0.010f, Color.argb(62, 0, 170, 0))
        drawWaveLayer(canvas, overlayPaint, 10, 0.19f * amplitudeMultiplier, 0.013f, Color.argb(38, 0, 130, 0))
    }

    /**
     * Draws a single wave layer with complex multi-frequency wave patterns.
     *
     * Creates sophisticated wave animations by combining multiple sine waves:
     * - Primary wave (60% contribution): Main wave pattern
     * - Height variation wave (25%): Secondary variation for natural look
     * - Micro variation wave (10%): Fine detail variations
     * - Extra variation wave (5%): Additional complexity
     *
     * The wave positioning varies by layer index to create depth, and the
     * step size is optimized based on screen resolution for performance.
     *
     * @param canvas The canvas to draw on
     * @param paint Paint object for rendering
     * @param layerIndex Index of the wave layer (1-10) for unique positioning
     * @param baseAmplitude Base amplitude multiplier for the wave height
     * @param frequency Wave frequency controlling the wave pattern density
     * @param color ARGB color value for the wave layer
     */
    private fun drawWaveLayer(
        canvas: Canvas,
        paint: Paint,
        layerIndex: Int,
        baseAmplitude: Float,
        frequency: Float,
        color: Int
    ) {
        val path = Path()
        val currentTime = System.currentTimeMillis() / 1000.0

        // Calculate dynamic amplitude that varies continuously - HIGHER WAVES
        val amplitudeVariation = sin(currentTime * 0.5 + layerIndex).toFloat()
        val dynamicAmplitude = height * (baseAmplitude + amplitudeVariation * 0.08f)

        // Different base heights for each layer to create depth - HIGHER VARIATIONS
        val baseHeight = when (layerIndex) {
            1 -> height * 0.75f  // Much higher waves
            2 -> height * 0.65f  // Higher waves
            3 -> height * 0.85f  // Highest waves
            4 -> height * 0.70f  // Higher waves
            5 -> height * 0.80f  // Higher waves
            6 -> height * 0.72f  // Higher waves
            7 -> height * 0.88f  // Highest waves
            8 -> height * 0.68f  // Higher waves
            9 -> height * 0.78f  // Higher waves
            10 -> height * 0.74f // Higher waves
            else -> height * 0.75f
        }

        path.moveTo(0f, height.toFloat())

        // Responsive step size for smoother waves on larger screens
        val stepSize = when {
            width >= 1200 -> 1 // Very smooth for large tablets
            width >= 800 -> 2  // Smooth for tablets
            else -> 3          // Standard for phones
        }

        // Create wave with varying heights using continuous time
        for (x in 0..width step stepSize) {
            // Use continuous time instead of phase for seamless animation
            val timePhase = currentTime * (0.5 + layerIndex * 0.15) // Different speeds per layer

            // Primary wave
            val primaryWave = sin(frequency * x + timePhase).toFloat()

            // Secondary wave for height variation
            val heightVariation = sin(frequency * x * 2.5f + timePhase * 1.3).toFloat()

            // Tertiary wave for micro-variations
            val microVariation = sin(frequency * x * 4f + timePhase * 0.7).toFloat()

            // Quaternary wave for extra complexity
            val extraVariation = sin(frequency * x * 6f + timePhase * 0.4).toFloat()

            // Combine waves with different amplitudes - MORE PROMINENT
            val combinedAmplitude = dynamicAmplitude * (
                    primaryWave * 0.6f +
                            heightVariation * 0.25f +
                            microVariation * 0.1f +
                            extraVariation * 0.05f
                    )

            // Calculate wave peak heights that vary continuously
            val peakHeightModifier = 1f + 0.6f * sin(frequency * x * 0.5f + timePhase * 0.3).toFloat()
            val finalAmplitude = combinedAmplitude * peakHeightModifier

            val y = baseHeight - finalAmplitude

            if (x == 0) {
                path.moveTo(x.toFloat(), y)
            } else {
                path.lineTo(x.toFloat(), y)
            }
        }

        // Close the path to fill the wave area
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()

        // Apply color with continuous alpha variation - MORE VISIBLE
        val alphaModifier = 0.9f + 0.1f * sin(currentTime + layerIndex).toFloat()
        val dynamicAlpha = (Color.alpha(color) * alphaModifier).toInt().coerceIn(30, 100)
        paint.color = Color.argb(
            dynamicAlpha,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )

        canvas.drawPath(path, paint)
    }

    /**
     * Called when the view is attached to a window. Starts the wave animation.
     * This ensures the animation begins when the view becomes visible.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator?.start()
    }

    /**
     * Called when the view is detached from a window. Stops the animation
     * to prevent memory leaks and unnecessary processing.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    /**
     * Called when the view's visibility changes. Manages animation lifecycle
     * based on visibility to optimize performance.
     *
     * @param changedView The view whose visibility changed
     * @param visibility New visibility state (VISIBLE, INVISIBLE, or GONE)
     */
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            animator?.start()
        } else {
            animator?.pause()
        }
    }
}