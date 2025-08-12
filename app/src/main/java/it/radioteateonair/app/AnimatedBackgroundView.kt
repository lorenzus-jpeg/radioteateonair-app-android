package it.radioteateonair.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*

class AnimatedBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var animationProgress = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gradient: LinearGradient? = null
    private var animator: ValueAnimator? = null

    // Wave parameters for multiple layers - INCREASED NUMBER OF WAVES
    private var waveOffset1 = 0f
    private var waveOffset2 = 0f
    private var waveOffset3 = 0f
    private var waveOffset4 = 0f
    private var waveOffset5 = 0f
    private var waveOffset6 = 0f
    private var waveOffset7 = 0f
    private var waveOffset8 = 0f
    private var waveOffset9 = 0f
    private var waveOffset10 = 0f

    init {
        setupAnimator()
    }

    private fun setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, Float.MAX_VALUE).apply {
            duration = Long.MAX_VALUE // Infinite duration
            repeatCount = 0 // No repeats needed
            interpolator = LinearInterpolator()

            addUpdateListener { animation ->
                val currentTime = System.currentTimeMillis()

                // Use system time for truly continuous animation
                val timeSeconds = currentTime / 1000.0

                // Create different phase offsets that never reset - MORE WAVES
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()
    }

    private fun updateGradient() {
        if (width <= 0 || height <= 0) return

        // STATIC GRADIENT - NO ANIMATION
        val colors = intArrayOf(
            Color.rgb(0, 0, 0), // Pure black
            Color.rgb(0, 32, 0), // Dark green
            Color.rgb(0, 64, 0), // Medium green
            Color.rgb(0, 128, 0)  // Bright green
        )

        val positions = floatArrayOf(0f, 0.3f, 0.7f, 1f)

        gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors, positions,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        updateGradient()

        gradient?.let { grad ->
            paint.shader = grad
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        // Add HIGHER and MORE VISIBLE wave overlay
        drawWaveOverlay(canvas)
    }

    private fun drawWaveOverlay(canvas: Canvas) {
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Draw MORE wave layers with HIGHER amplitudes and MORE VISIBILITY
        drawWaveLayer(canvas, overlayPaint, 1, 0.25f, 0.008f, waveOffset1, Color.argb(60, 0, 180, 0))
        drawWaveLayer(canvas, overlayPaint, 2, 0.22f, 0.012f, waveOffset2, Color.argb(45, 0, 150, 0))
        drawWaveLayer(canvas, overlayPaint, 3, 0.28f, 0.006f, waveOffset3, Color.argb(55, 0, 200, 0))
        drawWaveLayer(canvas, overlayPaint, 4, 0.20f, 0.015f, waveOffset4, Color.argb(40, 0, 120, 0))
        drawWaveLayer(canvas, overlayPaint, 5, 0.24f, 0.009f, waveOffset5, Color.argb(65, 0, 160, 0))
        drawWaveLayer(canvas, overlayPaint, 6, 0.26f, 0.011f, waveOffset6, Color.argb(50, 0, 140, 0))
        drawWaveLayer(canvas, overlayPaint, 7, 0.23f, 0.007f, waveOffset7, Color.argb(58, 0, 190, 0))
        drawWaveLayer(canvas, overlayPaint, 8, 0.21f, 0.014f, waveOffset8, Color.argb(42, 0, 110, 0))
        drawWaveLayer(canvas, overlayPaint, 9, 0.27f, 0.010f, waveOffset9, Color.argb(62, 0, 170, 0))
        drawWaveLayer(canvas, overlayPaint, 10, 0.19f, 0.013f, waveOffset10, Color.argb(38, 0, 130, 0))
    }

    private fun drawWaveLayer(
        canvas: Canvas,
        paint: Paint,
        layerIndex: Int,
        baseAmplitude: Float,
        frequency: Float,
        phase: Float,
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

        // Create wave with varying heights using continuous time
        for (x in 0..width step 2) {
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            animator?.start()
        } else {
            animator?.pause()
        }
    }
}