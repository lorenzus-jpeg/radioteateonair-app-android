package it.teateonair.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*

class PlayerAnimatedBackgroundView : View {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private var animator: ValueAnimator? = null
    private var clipPath: Path? = null
    private var cornerRadius = 0f

    private var waveOffset1 = 0f
    private var waveOffset2 = 0f
    private var waveOffset3 = 0f
    private var waveOffset4 = 0f
    private var waveOffset5 = 0f
    private var waveOffset6 = 0f

    init {
        setupAnimator()
    }

    private fun init() {
        cornerRadius = 24f * resources.displayMetrics.density // Typical player bar corner radius
        setupAnimator()
    }

    private fun setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, Float.MAX_VALUE).apply {
            duration = Long.MAX_VALUE
            repeatCount = 0
            interpolator = LinearInterpolator()

            addUpdateListener { _ ->
                val currentTime = System.currentTimeMillis()
                val timeSeconds = currentTime / 1000.0

                waveOffset1 = (timeSeconds * 20.0).toFloat()
                waveOffset2 = (timeSeconds * 15.0).toFloat()
                waveOffset3 = (timeSeconds * 25.0).toFloat()
                waveOffset4 = (timeSeconds * 18.0).toFloat()
                waveOffset5 = (timeSeconds * 22.0).toFloat()
                waveOffset6 = (timeSeconds * 12.0).toFloat()

                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateClipPath()
    }

    private fun updateClipPath() {
        if (width <= 0 || height <= 0) return

        clipPath = Path().apply {
            addRoundRect(
                0f, 0f, width.toFloat(), height.toFloat(),
                cornerRadius, cornerRadius,
                Path.Direction.CW
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Clip to rounded rectangle before drawing waves
        clipPath?.let { canvas.clipPath(it) }

        drawPlayerWaveOverlay(canvas)
    }

    private fun drawPlayerWaveOverlay(canvas: Canvas) {
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val amplitudeMultiplier = when {
            width >= 1200 -> 0.4f
            width >= 800 -> 0.3f
            else -> 0.25f
        }

        drawWaveLayer(canvas, overlayPaint, 1, 0.15f * amplitudeMultiplier, 0.012f, Color.argb(25, 0, 0, 0))
        drawWaveLayer(canvas, overlayPaint, 2, 0.12f * amplitudeMultiplier, 0.015f, Color.argb(20, 20, 20, 20))
        drawWaveLayer(canvas, overlayPaint, 3, 0.18f * amplitudeMultiplier, 0.010f, Color.argb(30, 0, 0, 0))
        drawWaveLayer(canvas, overlayPaint, 4, 0.10f * amplitudeMultiplier, 0.018f, Color.argb(15, 40, 40, 40))
        drawWaveLayer(canvas, overlayPaint, 5, 0.14f * amplitudeMultiplier, 0.013f, Color.argb(22, 0, 0, 0))
        drawWaveLayer(canvas, overlayPaint, 6, 0.16f * amplitudeMultiplier, 0.011f, Color.argb(18, 60, 60, 60))
    }

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

        val amplitudeVariation = sin(currentTime * 0.3 + layerIndex).toFloat()
        val dynamicAmplitude = height * (baseAmplitude + amplitudeVariation * 0.03f)

        val baseHeight = when (layerIndex) {
            1 -> height * 0.85f
            2 -> height * 0.90f
            3 -> height * 0.80f
            4 -> height * 0.95f
            5 -> height * 0.88f
            6 -> height * 0.92f
            else -> height * 0.90f
        }

        path.moveTo(0f, height.toFloat())

        val stepSize = 4

        for (x in 0..width step stepSize) {
            val timePhase = currentTime * (0.3 + layerIndex * 0.1)

            val primaryWave = sin(frequency * x + timePhase).toFloat()
            val secondaryWave = sin(frequency * x * 1.5f + timePhase * 0.8).toFloat()

            val combinedAmplitude = dynamicAmplitude * (
                    primaryWave * 0.7f + secondaryWave * 0.3f
                    )

            val y = baseHeight - combinedAmplitude

            if (x == 0) {
                path.moveTo(x.toFloat(), y)
            } else {
                path.lineTo(x.toFloat(), y)
            }
        }

        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()

        val alphaModifier = 0.95f + 0.05f * sin(currentTime * 0.5 + layerIndex).toFloat()
        val dynamicAlpha = (Color.alpha(color) * alphaModifier).toInt().coerceIn(10, 35)
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