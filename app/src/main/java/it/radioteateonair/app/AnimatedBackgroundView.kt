package it.teateonair.app

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

    private var animator: ValueAnimator? = null
    private var waveMode = "wave"
    private val waveOffsets = FloatArray(10)

    // Bouncing trail animation properties
    private data class BouncingDot(
        var x: Float = 0f,
        var y: Float = 0f,
        var velocityX: Float = 0f,
        var velocityY: Float = 0f,
        val trailPoints: MutableList<Pair<Float, Float>> = mutableListOf(),
        val trailTimestamps: MutableList<Long> = mutableListOf(),
        val color: Int = 0
    )

    private val bouncingDots = mutableListOf<BouncingDot>()

    init {
        setupAnimator()
    }

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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (bouncingDots.isEmpty()) {
            initializeBouncingDots()
        }
    }

    private fun initializeBouncingDots() {
        val colors = listOf(
            Color.parseColor("#00FF88"),
            Color.parseColor("#00FFCC"),
            Color.parseColor("#00FFFF"),
            Color.parseColor("#00CCFF"),
            Color.parseColor("#0088FF")
        )

        val velocities = listOf(
            Pair(2.5f, 2f),
            Pair(-2.8f, 1.5f),
            Pair(2f, -2.5f),
            Pair(-1.8f, -2.2f),
            Pair(3f, 1.8f)
        )

        for (i in 0 until 5) {
            val dot = BouncingDot(
                x = (width / 2f) + (i * 40f - 80f),
                y = (height / 2f) + (i * 30f - 60f),
                velocityX = velocities[i].first,
                velocityY = velocities[i].second,
                color = colors[i]
            )
            bouncingDots.add(dot)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawWaves(canvas)
    }

    private fun drawWaves(canvas: Canvas) {
        if (waveMode == "bouncing") {
            drawBouncingTrail(canvas)
            return
        }

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

    private fun drawBouncingTrail(canvas: Canvas) {
        val currentTime = System.currentTimeMillis()
        val pointRadius = 8f
        val maxTrailLength = 600

        for (dot in bouncingDots) {
            dot.x += dot.velocityX
            dot.y += dot.velocityY

            if (dot.x - pointRadius <= 0 || dot.x + pointRadius >= width) {
                dot.velocityX *= -1
                dot.x = dot.x.coerceIn(pointRadius, (width - pointRadius).toFloat())
            }
            if (dot.y - pointRadius <= 0 || dot.y + pointRadius >= height) {
                dot.velocityY *= -1
                dot.y = dot.y.coerceIn(pointRadius, (height - pointRadius).toFloat())
            }

            dot.trailPoints.add(Pair(dot.x, dot.y))
            dot.trailTimestamps.add(currentTime)

            while (dot.trailPoints.size > maxTrailLength) {
                dot.trailPoints.removeAt(0)
                dot.trailTimestamps.removeAt(0)
            }
        }

        val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val trailColors = listOf(
            Color.argb(15, 0, 255, 136),
            Color.argb(30, 0, 220, 120),
            Color.argb(60, 0, 180, 100),
            Color.argb(100, 0, 150, 80),
            Color.argb(150, 0, 255, 136)
        )

        for (dot in bouncingDots) {
            for (i in 1 until dot.trailPoints.size) {
                val prevPoint = dot.trailPoints[i - 1]
                val currentPoint = dot.trailPoints[i]

                val progress = i.toFloat() / dot.trailPoints.size
                val colorIndex = (progress * (trailColors.size - 1)).toInt()
                val nextColorIndex = minOf(colorIndex + 1, trailColors.size - 1)

                val alphaMix = (progress * (trailColors.size - 1)) % 1f
                val color1 = trailColors[colorIndex]
                val color2 = trailColors[nextColorIndex]

                val a1 = Color.alpha(color1)
                val a2 = Color.alpha(color2)
                val r1 = Color.red(color1)
                val r2 = Color.red(color2)
                val g1 = Color.green(color1)
                val g2 = Color.green(color2)
                val b1 = Color.blue(color1)
                val b2 = Color.blue(color2)

                val interpolatedColor = Color.argb(
                    (a1 * (1 - alphaMix) + a2 * alphaMix).toInt(),
                    (r1 * (1 - alphaMix) + r2 * alphaMix).toInt(),
                    (g1 * (1 - alphaMix) + g2 * alphaMix).toInt(),
                    (b1 * (1 - alphaMix) + b2 * alphaMix).toInt()
                )

                trailPaint.color = interpolatedColor
                trailPaint.strokeWidth = 3f + (progress * 5f)

                canvas.drawLine(prevPoint.first, prevPoint.second, currentPoint.first, currentPoint.second, trailPaint)
            }
        }

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        for (dot in bouncingDots) {
            val glowRadius = pointRadius * 2.5f
            glowPaint.color = Color.argb(40, Color.red(dot.color), Color.green(dot.color), Color.blue(dot.color))
            canvas.drawCircle(dot.x, dot.y, glowRadius, glowPaint)

            pointPaint.color = dot.color
            canvas.drawCircle(dot.x, dot.y, pointRadius, pointPaint)

            canvas.drawCircle(dot.x, dot.y, pointRadius * 0.5f, corePaint)
        }
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
        if (visibility == VISIBLE) animator?.start()
        else animator?.cancel()
    }

    fun setWaveMode(mode: String) {
        waveMode = mode
        invalidate()
    }
}