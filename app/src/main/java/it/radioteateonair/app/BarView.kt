package it.radioteateonair.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.animation.ValueAnimator

class BarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.WHITE
    }

    private var barHeight: Float = 0f
    private var animator: ValueAnimator? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val topY = height - barHeight
        canvas.drawRect(centerX - 5, topY, centerX + 5, height.toFloat(), paint)
    }

    fun startAnimation() {
        animator = ValueAnimator.ofFloat(20f, height.toFloat()).apply {
            duration = (800..1200).random().toLong() // ⏳ slower animation
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener {
                barHeight = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        barHeight = 0f
        invalidate()
    }
}
