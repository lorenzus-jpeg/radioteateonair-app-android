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
 * Supports three wave modes: smooth waves, flat steps, and triangular waves.
 *
 * @author lorenzus-jpeg
 * @since v11
 */
class AnimatedBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var animator: ValueAnimator? = null
    private var waveMode = "wave"
    private val waveOffsets = FloatArray(10)

    init {
        setupAnimator()
    }

    /**
     * Initializes and configures the ValueAnimator for continuous wave animation.
     * Creates a time-based animator that runs indefinitely and updates wave offsets
     * for all 10 layers based on system time.
     *
     * @author lorenzus-jpeg
     * @since v11
     */
    private fun setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, Float.MAX_VALUE).apply {
            duration = Long.MAX_VALUE
            repeatCount = 0
            interpolator = LinearInterpolator()
            addUpdateListener {
                val timeSeconds = System.currentTimeMillis() / 1000.0
                for (i in 0..9) {
                    waveOffsets[i] = (timeSeconds * (15f + (i + 1) * 5f)).toFloat()
                }
                invalidate()
            }
            start()
        }
    }

    /**
     * Called when the view size changes. Currently a placeholder override.
     *
     * @param w Current width of the view
     * @param h Current height of the view
     * @param oldw Previous width of the view
     * @param oldh Previous height of the view
     *
     * @author lorenzus-jpeg
     * @since v11
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }

    /**
     * Main drawing method that renders all animated wave layers.
     * Delegates to drawWaves() for rendering.
     *
     * @param canvas The canvas to draw on
     *
     * @author lorenzus-jpeg
     * @since v11
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawWaves(canvas)
    }

    /**
     * Draws all 10 wave layers with responsive amplitude scaling.
     * Each layer has unique amplitude, frequency, and color values.
     *
     * @param canvas The canvas to draw the waves on
     *
     * @author lorenzus-jpeg
     * @since v11
     */
    private fun drawWaves(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val amplitudeMultiplier = when {
            width >= 1200 -> 1.2f
            width >= 800 -> 1.0f
            else -> 0.8f
        }

        val waves = listOf(
            Triple(0.25f, 0.008f, Color.argb(35, 0, 180, 0)),
            Triple(0.22f, 0.012f, Color.argb(28, 0, 150, 0)),
            Triple(0.28f, 0.006f, Color.argb(32, 0, 200, 0)),
            Triple(0.20f, 0.015f, Color.argb(25, 0, 120, 0)),
            Triple(0.24f, 0.009f, Color.argb(50, 255, 255, 255)),
            Triple(0.26f, 0.011f, Color.argb(30, 0, 140, 0)),
            Triple(0.23f, 0.007f, Color.argb(34, 0, 190, 0)),
            Triple(0.21f, 0.014f, Color.argb(26, 0, 110, 0)),
            Triple(0.27f, 0.010f, Color.argb(36, 0, 170, 0)),
            Triple(0.19f, 0.013f, Color.argb(23, 0, 130, 0))
        )

        waves.forEachIndexed { index, (amplitude, frequency, color) ->
            drawWaveLayer(
                canvas, paint, index + 1,
                amplitude * amplitudeMultiplier,
                frequency, color,
                isWhiteWave = (index == 4)
            )
        }
    }

    /**
     * Draws a single wave layer with configurable wave mode (smooth, flat, or triangle).
     *
     * @param canvas The canvas to draw on
     * @param paint Paint object for rendering
     * @param layerIndex Index of the wave layer (1-10) for unique positioning
     * @param baseAmplitude Base amplitude multiplier for the wave height
     * @param frequency Wave frequency controlling the wave pattern density
     * @param color ARGB color value for the wave layer
     * @param isWhiteWave Whether this is the white wave with enhanced variability
     *
     * @author lorenzus-jpeg
     * @since v11
     */
    private fun drawWaveLayer(
        canvas: Canvas,
        paint: Paint,
        layerIndex: Int,
        baseAmplitude: Float,
        frequency: Float,
        color: Int,
        isWhiteWave: Boolean
    ) {
        val path = Path()
        val currentTime = System.currentTimeMillis() / 1000.0
        val amplitudeVariation = sin(currentTime * 0.5 + layerIndex).toFloat()
        val dynamicAmplitude = height * (baseAmplitude + amplitudeVariation * if (isWhiteWave) 0.25f else 0.08f)

        val baseHeight = height * when (layerIndex) {
            1 -> 0.75f; 2 -> 0.65f; 3 -> 0.85f; 4 -> 0.70f; 5 -> 0.80f
            6 -> 0.72f; 7 -> 0.88f; 8 -> 0.68f; 9 -> 0.78f; else -> 0.74f
        }

        path.moveTo(0f, height.toFloat())

        val stepSize = when { width >= 1200 -> 1; width >= 800 -> 2; else -> 3 }
        val timePhase = currentTime * (0.5 + layerIndex * 0.15)

        if (width > 0) {
            for (x in 0..width step stepSize) {
                val y = when (waveMode) {
                    "flat" -> calculateFlatWaveY(x, baseHeight, dynamicAmplitude, currentTime, layerIndex)
                    "triangle" -> calculateTriangleWaveY(x, baseHeight, dynamicAmplitude, frequency, timePhase)
                    else -> calculateSmoothWaveY(x, baseHeight, dynamicAmplitude, frequency, timePhase, isWhiteWave)
                }

                if (x == 0) path.moveTo(x.toFloat(), y)
                else path.lineTo(x.toFloat(), y)
            }
        } else {
            path.lineTo(0f, baseHeight)
        }

        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()

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

        paint.color = Color.argb(alphaRange, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawPath(path, paint)
    }

    /**
     * Calculates Y coordinate for smooth wave mode.
     * Combines multiple sine waves to create complex, non-repeating patterns.
     *
     * @param x Current X pixel position
     * @param baseHeight Base height of the wave layer
     * @param dynamicAmplitude Current amplitude of the wave
     * @param frequency Wave frequency controlling pattern density
     * @param timePhase Phase offset based on time and layer index
     * @param isWhiteWave Whether this is the white wave with enhanced complexity
     * @return Y coordinate for the current position
     *
     * @author lorenzus-jpeg
     * @since v11
     */
    private fun calculateSmoothWaveY(
        x: Int,
        baseHeight: Float,
        dynamicAmplitude: Float,
        frequency: Float,
        timePhase: Double,
        isWhiteWave: Boolean
    ): Float {
        val primaryWave = sin(frequency * x + timePhase).toFloat()
        val heightVariation = sin(frequency * x * 2.5f + timePhase * 1.3).toFloat()
        val microVariation = sin(frequency * x * 4f + timePhase * 0.7).toFloat()
        val extraVariation = sin(frequency * x * 6f + timePhase * 0.4).toFloat()

        val combinedAmplitude = if (isWhiteWave) {
            dynamicAmplitude * (primaryWave * 0.5f + heightVariation * 0.3f + microVariation * 0.15f + extraVariation * 0.05f)
        } else {
            dynamicAmplitude * (primaryWave * 0.6f + heightVariation * 0.25f + microVariation * 0.1f + extraVariation * 0.05f)
        }

        val peakModifierIntensity = if (isWhiteWave) 0.9f else 0.6f
        val peakHeightModifier = 1f + peakModifierIntensity * sin(frequency * x * 0.5f + timePhase * 0.3).toFloat()

        return baseHeight - combinedAmplitude * peakHeightModifier
    }

    /**
     * Calculates Y coordinate for flat step mode.
     * Creates discrete step-like changes that shift over time like a step chart.
     *
     * @param x Current X pixel position
     * @param baseHeight Base height of the wave layer
     * @param dynamicAmplitude Current amplitude of the wave
     * @param currentTime Current time in seconds
     * @param layerIndex Index of the current layer (1-10)
     * @return Y coordinate for the current position
     *
     * @author lorenzus-jpeg
     * @since v11
     */
    private fun calculateFlatWaveY(
        x: Int,
        baseHeight: Float,
        dynamicAmplitude: Float,
        currentTime: Double,
        layerIndex: Int
    ): Float {
        val stepWidth = width / 8f
        val stepIndex = (x / stepWidth).toInt()
        val stepPhase = (currentTime * 2 + stepIndex + layerIndex).toInt() % 4
        return baseHeight - dynamicAmplitude * when (stepPhase) {
            0 -> 0.3f; 1 -> 0.7f; 2 -> 0.5f; else -> 0.4f
        }
    }

    /**
     * Calculates Y coordinate for triangular wave mode.
     * Creates sharp peaks and valleys with linear transitions between them.
     *
     * @param x Current X pixel position
     * @param baseHeight Base height of the wave layer
     * @param dynamicAmplitude Current amplitude of the wave
     * @param frequency Wave frequency controlling pattern density
     * @param timePhase Phase offset based on time and layer index
     * @return Y coordinate for the current position
     *
     * @author lorenzus-jpeg
     * @since v11
     */
    private fun calculateTriangleWaveY(
        x: Int,
        baseHeight: Float,
        dynamicAmplitude: Float,
        frequency: Float,
        timePhase: Double
    ): Float {
        val triangleWave = (asin(sin((frequency * x + timePhase).toDouble())) / (PI / 2)).toFloat()
        return baseHeight - dynamicAmplitude * triangleWave
    }

    /**
     * Called when the view is attached to a window. Starts the wave animation.
     *
     * @author lorenzus-jpeg
     * @since v11
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator?.start()
    }

    /**
     * Called when the view is detached from a window. Stops the animation
     * to prevent memory leaks and unnecessary processing.
     *
     * @author lorenzus-jpeg
     * @since v11
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
     *
     * @author lorenzus-jpeg
     * @since v11
     */
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) animator?.start()
        else animator?.cancel()
    }

    /**
     * Sets the wave mode (smooth, flat, or triangular).
     * Updates will be applied on the next frame.
     *
     * @param mode Wave mode: "wave" (smooth), "flat" (steps), or "triangle" (triangular)
     *
     * @author lorenzus-jpeg
     * @since v11
     */
    fun setWaveMode(mode: String) {
        waveMode = mode
        invalidate()
    }
}