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

    // Wave parameters for multiple layers
    private var waveOffset1 = 0f
    private var waveOffset2 = 0f
    private var waveOffset3 = 0f
    private var waveOffset4 = 0f
    private var waveOffset5 = 0f

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

                // Create different phase offsets that never reset
                waveOffset1 = (timeSeconds * 30.0).toFloat()   // 30 degrees per second
                waveOffset2 = (timeSeconds * 45.0).toFloat()   // 45 degrees per second
                waveOffset3 = (timeSeconds * 20.0).toFloat()   // 20 degrees per second
                waveOffset4 = (timeSeconds * 60.0).toFloat()   // 60 degrees per second
                waveOffset5 = (timeSeconds * 15.0).toFloat()   // 15 degrees per second

                // Animation progress for gradient (cycles every 8 seconds)
                animationProgress = ((timeSeconds % 8.0) / 8.0).toFloat()

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

        // Use system time for seamless gradient rotation
        val currentTime = System.currentTimeMillis()
        val timeSeconds = currentTime / 1000.0

        // Rotate gradient continuously (full rotation every 16 seconds)
        val angle = ((timeSeconds * 22.5) % 360.0).toFloat() + 135f // 22.5 deg/sec
        val radians = Math.toRadians(angle.toDouble())

        // Calculate gradient endpoints
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = max(width, height) / 2f

        val startX = centerX - cos(radians).toFloat() * radius
        val startY = centerY - sin(radians).toFloat() * radius
        val endX = centerX + cos(radians).toFloat() * radius
        val endY = centerY + sin(radians).toFloat() * radius

        // Create smooth color transitions using continuous time
        val wave1 = sin(timeSeconds * 0.8).toFloat()  // Slower color transitions
        val wave2 = sin(timeSeconds * 1.2).toFloat()
        val wave3 = sin(timeSeconds * 0.6).toFloat()

        // Calculate colors based on wave patterns
        val greenComponent1 = (32 + (wave1 * 16f).toInt()).coerceIn(0, 64)
        val greenComponent2 = (64 + (wave2 * 32f).toInt()).coerceIn(32, 128)
        val greenComponent3 = (128 + (wave3 * 64f).toInt()).coerceIn(64, 255)

        val colors = intArrayOf(
            Color.rgb(0, 0, 0), // Pure black
            Color.rgb(0, greenComponent1, 0), // Dark green
            Color.rgb(0, greenComponent2, 0), // Medium green
            Color.rgb(0, greenComponent3, 0)  // Bright green
        )

        val positions = floatArrayOf(0f, 0.3f, 0.7f, 1f)

        gradient = LinearGradient(
            startX, startY, endX, endY,
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

        // Add subtle wave overlay for extra effect
        drawWaveOverlay(canvas)
    }

    private fun drawWaveOverlay(canvas: Canvas) {
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Draw multiple wave layers with different properties
        drawWaveLayer(canvas, overlayPaint, 1, 0.15f, 0.008f, waveOffset1, Color.argb(40, 0, 180, 0))
        drawWaveLayer(canvas, overlayPaint, 2, 0.12f, 0.012f, waveOffset2, Color.argb(30, 0, 150, 0))
        drawWaveLayer(canvas, overlayPaint, 3, 0.18f, 0.006f, waveOffset3, Color.argb(35, 0, 200, 0))
        drawWaveLayer(canvas, overlayPaint, 4, 0.10f, 0.015f, waveOffset4, Color.argb(25, 0, 120, 0))
        drawWaveLayer(canvas, overlayPaint, 5, 0.14f, 0.009f, waveOffset5, Color.argb(45, 0, 160, 0))
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

        // Calculate dynamic amplitude that varies continuously
        val amplitudeVariation = sin(currentTime * 0.5 + layerIndex).toFloat()
        val dynamicAmplitude = height * (baseAmplitude + amplitudeVariation * 0.05f)

        // Different base heights for each layer to create depth
        val baseHeight = when (layerIndex) {
            1 -> height * 0.88f  // Bottom layer - highest waves
            2 -> height * 0.82f  // Second layer
            3 -> height * 0.92f  // Third layer - lowest waves
            4 -> height * 0.85f  // Fourth layer
            5 -> height * 0.90f  // Top layer
            else -> height * 0.85f
        }

        path.moveTo(0f, height.toFloat())

        // Create wave with varying heights using continuous time
        for (x in 0..width step 2) {
            // Use continuous time instead of phase for seamless animation
            val timePhase = currentTime * (0.5 + layerIndex * 0.2) // Different speeds per layer

            // Primary wave
            val primaryWave = sin(frequency * x + timePhase).toFloat()

            // Secondary wave for height variation
            val heightVariation = sin(frequency * x * 2.5f + timePhase * 1.3).toFloat()

            // Tertiary wave for micro-variations
            val microVariation = sin(frequency * x * 4f + timePhase * 0.7).toFloat()

            // Combine waves with different amplitudes
            val combinedAmplitude = dynamicAmplitude * (
                    primaryWave * 0.6f +
                            heightVariation * 0.3f +
                            microVariation * 0.1f
                    )

            // Calculate wave peak heights that vary continuously
            val peakHeightModifier = 1f + 0.4f * sin(frequency * x * 0.5f + timePhase * 0.3).toFloat()
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

        // Apply color with continuous alpha variation
        val alphaModifier = 0.8f + 0.2f * sin(currentTime + layerIndex).toFloat()
        val dynamicAlpha = (Color.alpha(color) * alphaModifier).toInt().coerceIn(10, 80)
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