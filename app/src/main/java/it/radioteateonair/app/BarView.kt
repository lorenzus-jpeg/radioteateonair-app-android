package it.radioteateonair.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class BarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = 0xFFFFFFFF.toInt() // white
        style = Paint.Style.FILL
    }

    private var barHeight = 0
    private var animator: ValueAnimator? = null

    fun startAnimation() {
        stopAnimation()
        animator = ValueAnimator.ofInt(10, height).apply {
            duration = 800L // slower animation
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                barHeight = it.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        barHeight = 0
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = 0f
        val right = width.toFloat()
        val top = height - barHeight.toFloat()
        val bottom = height.toFloat()
        canvas.drawRect(left, top, right, bottom, paint)
    }
}
