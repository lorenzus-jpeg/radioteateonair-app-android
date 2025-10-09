package it.teateonair.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*

/**
 * Animated background view that displays multiple layered wave animations.
 * One white wave is always visible and highly variable for visual interest.
 *
 * The waves use sine wave patterns with multiple frequencies combined to create
 * a natural, organic movement. Layer 5 is designated as the white wave with enhanced
 * variability, while the other 9 layers display green waves with subtle transparency.
 *
 * @property gradient Linear gradient for background (currently null for transparency)
 * @property animator ValueAnimator that drives the continuous wave animation
 * @property whiteWaveLayer Index of the layer that displays as white (always layer 5)
 *
 * @author lrnz
 * @since v1
 */
class AnimatedBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var gradient: LinearGradient? = null
    private var animator: ValueAnimator? = null

    /** Wave offset for layer 1 (30°/second rotation speed) */
    private var waveOffset1 = 0f
    /** Wave offset for layer 2 (45°/second rotation speed) */
    private var waveOffset2 = 0f
    /** Wave offset for layer 3 (20°/second rotation speed) */
    private var waveOffset3 = 0f
    /** Wave offset for layer 4 (60°/second rotation speed) */
    private var waveOffset4 = 0f
    /** Wave offset for layer 5 (15°/second rotation speed) - White wave */
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

    /** White wave is always layer 5 (middle layer) */
    private val whiteWaveLayer = 5

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
            duration = Long.MAX_VALUE
            repeatCount = 0
            interpolator = LinearInterpolator()

            addUpdateListener { _ ->
                val currentTime = System.currentTimeMillis()
                val timeSeconds = currentTime / 1000.0

                waveOffset1 = (timeSeconds * 30.0).toFloat()
                waveOffset2 = (timeSeconds * 45.0).toFloat()
                waveOffset3 = (timeSeconds * 20.0).toFloat()
                waveOffset4 = (timeSeconds * 60.0).toFloat()
                waveOffset5 = (timeSeconds * 15.0).toFloat()
                waveOffset6 = (timeSeconds * 35.0).toFloat()
                waveOffset7 = (timeSeconds * 50.0).toFloat()
                waveOffset8 = (timeSeconds * 25.0).toFloat()
                waveOffset9 = (timeSeconds * 40.0).toFloat()
                waveOffset10 = (timeSeconds * 55.0).toFloat()

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
     *
     * Currently sets gradient to null to maintain transparency and allow
     * underlying content (like background images) to show through the waves.
     */
    private fun updateGradient() {
        if (width <= 0 || height <= 0) return
        gradient = null
    }

    /**
     * Main drawing method that renders the animated wave overlay.
     *
     * Delegates to [drawWaveOverlay] to render all wave layers.
     *
     * @param canvas The canvas to draw on
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawWaveOverlay(canvas)
    }

    /**
     * Draws all 10 wave layers with responsive amplitude scaling.
     *
     * @param canvas The canvas to draw the waves on
     */
    private fun drawWaveOverlay(canvas: Canvas) {
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val amplitudeMultiplier = when {
            width >= 1200 -> 1.2f
            width >= 800 -> 1.0f
            else -> 0.8f
        }

        // Draw all 10 layers - layer 5 is white and more variable, others are green and less opaque
        drawWaveLayer(canvas, overlayPaint, 1, 0.25f * amplitudeMultiplier, 0.008f,
            Color.argb(35, 0, 180, 0), isWhiteWave = false)

        drawWaveLayer(canvas, overlayPaint, 2, 0.22f * amplitudeMultiplier, 0.012f,
            Color.argb(28, 0, 150, 0), isWhiteWave = false)

        drawWaveLayer(canvas, overlayPaint, 3, 0.28f * amplitudeMultiplier, 0.006f,
            Color.argb(32, 0, 200, 0), isWhiteWave = false)

        drawWaveLayer(canvas, overlayPaint, 4, 0.20f * amplitudeMultiplier, 0.015f,
            Color.argb(25, 0, 120, 0), isWhiteWave = false)

        // WHITE WAVE - layer 5, always visible, more variable
        drawWaveLayer(canvas, overlayPaint, 5, 0.24f * amplitudeMultiplier, 0.009f,
            Color.argb(50, 255, 255, 255), isWhiteWave = true)

        drawWaveLayer(canvas, overlayPaint, 6, 0.26f * amplitudeMultiplier, 0.011f,
            Color.argb(30, 0, 140, 0), isWhiteWave = false)

        drawWaveLayer(canvas, overlayPaint, 7, 0.23f * amplitudeMultiplier, 0.007f,
            Color.argb(34, 0, 190, 0), isWhiteWave = false)

        drawWaveLayer(canvas, overlayPaint, 8, 0.21f * amplitudeMultiplier, 0.014f,
            Color.argb(26, 0, 110, 0), isWhiteWave = false)

        drawWaveLayer(canvas, overlayPaint, 9, 0.27f * amplitudeMultiplier, 0.010f,
            Color.argb(36, 0, 170, 0), isWhiteWave = false)

        drawWaveLayer(canvas, overlayPaint, 10, 0.19f * amplitudeMultiplier, 0.013f,
            Color.argb(23, 0, 130, 0), isWhiteWave = false)
    }

    /**
     * Draws a single wave layer with complex multi-frequency wave patterns.
     *
     * @param canvas The canvas to draw on
     * @param paint Paint object for rendering
     * @param layerIndex Index of the wave layer (1-10) for unique positioning
     * @param baseAmplitude Base amplitude multiplier for the wave height
     * @param frequency Wave frequency controlling the wave pattern density
     * @param color ARGB color value for the wave layer
     * @param isWhiteWave Whether this is the white wave (layer 5) with enhanced variability
     */
    private fun drawWaveLayer(
        canvas: Canvas,
        paint: Paint,
        layerIndex: Int,
        baseAmplitude: Float,
        frequency: Float,
        color: Int,
        isWhiteWave: Boolean = false
    ) {
        val path = Path()
        val currentTime = System.currentTimeMillis() / 1000.0

        // White wave has MORE variation in amplitude
        val amplitudeVariationIntensity = if (isWhiteWave) 0.25f else 0.08f
        val amplitudeVariation = sin(currentTime * 0.5 + layerIndex).toFloat()
        val dynamicAmplitude = height * (baseAmplitude + amplitudeVariation * amplitudeVariationIntensity)

        val baseHeight = when (layerIndex) {
            1 -> height * 0.75f
            2 -> height * 0.65f
            3 -> height * 0.85f
            4 -> height * 0.70f
            5 -> height * 0.80f
            6 -> height * 0.72f
            7 -> height * 0.88f
            8 -> height * 0.68f
            9 -> height * 0.78f
            10 -> height * 0.74f
            else -> height * 0.75f
        }

        path.moveTo(0f, height.toFloat())

        val stepSize = when {
            width >= 1200 -> 1
            width >= 800 -> 2
            else -> 3
        }

        if (width > 0) {
            for (x in 0..width step stepSize) {
                val timePhase = currentTime * (0.5 + layerIndex * 0.15)

                // White wave has MORE complex wave patterns
                val primaryWave = sin(frequency * x + timePhase).toFloat()
                val heightVariation = sin(frequency * x * 2.5f + timePhase * 1.3).toFloat()
                val microVariation = sin(frequency * x * 4f + timePhase * 0.7).toFloat()
                val extraVariation = sin(frequency * x * 6f + timePhase * 0.4).toFloat()

                // White wave uses different wave combination weights for more variability
                val combinedAmplitude = if (isWhiteWave) {
                    dynamicAmplitude * (
                            primaryWave * 0.5f +
                                    heightVariation * 0.3f +
                                    microVariation * 0.15f +
                                    extraVariation * 0.05f
                            )
                } else {
                    dynamicAmplitude * (
                            primaryWave * 0.6f +
                                    heightVariation * 0.25f +
                                    microVariation * 0.1f +
                                    extraVariation * 0.05f
                            )
                }

                // White wave has MORE dramatic peak height variations
                val peakModifierIntensity = if (isWhiteWave) 0.9f else 0.6f
                val peakHeightModifier = 1f + peakModifierIntensity * sin(frequency * x * 0.5f + timePhase * 0.3).toFloat()
                val finalAmplitude = combinedAmplitude * peakHeightModifier

                val y = baseHeight - finalAmplitude

                if (x == 0) {
                    path.moveTo(x.toFloat(), y)
                } else {
                    path.lineTo(x.toFloat(), y)
                }
            }
        } else {
            path.lineTo(0f, baseHeight)
        }

        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()

        // White wave has MORE alpha variation
        val alphaModifier = if (isWhiteWave) {
            0.8f + 0.2f * sin(currentTime * 1.5 + layerIndex).toFloat()
        } else {
            0.9f + 0.1f * sin(currentTime + layerIndex).toFloat()
        }

        val alphaRange = if (isWhiteWave) {
            (Color.alpha(color) * alphaModifier).toInt().coerceIn(30, 80)
        } else {
            (Color.alpha(color) * alphaModifier).toInt().coerceIn(15, 50)
        }

        paint.color = Color.argb(
            alphaRange,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )

        canvas.drawPath(path, paint)
    }

    /**
     * Called when the view is attached to a window. Starts the wave animation.
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