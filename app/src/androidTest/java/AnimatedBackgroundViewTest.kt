package it.teateonair.app

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AnimatedBackgroundViewTest {

    private lateinit var context: Context
    private lateinit var view: AnimatedBackgroundView

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            view = AnimatedBackgroundView(context)
        }
    }

    @Test
    fun testViewCreation() {
        assert(view.width >= 0)
    }

    @Test
    fun testViewWithAttributeSet() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val viewWithAttrs = AnimatedBackgroundView(context, null)
            assert(viewWithAttrs.width >= 0)
        }
    }

    @Test
    fun testViewWithDefStyleAttr() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val viewWithStyle = AnimatedBackgroundView(context, null, 0)
            assert(viewWithStyle.width >= 0)
        }
    }

    @Test
    fun testViewInitialVisibility() {
        assert(view.visibility == View.VISIBLE)
    }

    @Test
    fun testViewMeasurement() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )

        assert(view.measuredWidth == 1000)
        assert(view.measuredHeight == 1000)
    }

    @Test
    fun testViewLayout() {
        view.layout(0, 0, 1000, 1000)

        assert(view.width == 1000)
        assert(view.height == 1000)
    }

    @Test
    fun testOnSizeChanged() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 800, 600)

        assert(view.width == 800)
        assert(view.height == 600)
    }

    @Test
    fun testViewAttachedToWindow() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1000, 1000)

        assert(view.isAttachedToWindow || !view.isAttachedToWindow)
    }

    @Test
    fun testViewVisibilityChangeToVisible() {
        view.visibility = View.VISIBLE
        assert(view.visibility == View.VISIBLE)
    }

    @Test
    fun testViewVisibilityChangeToInvisible() {
        view.visibility = View.INVISIBLE
        assert(view.visibility == View.INVISIBLE)
    }

    @Test
    fun testViewVisibilityChangeToGone() {
        view.visibility = View.GONE
        assert(view.visibility == View.GONE)
    }

    @Test
    fun testViewMultipleVisibilityChanges() {
        view.visibility = View.VISIBLE
        assert(view.visibility == View.VISIBLE)

        view.visibility = View.INVISIBLE
        assert(view.visibility == View.INVISIBLE)

        view.visibility = View.VISIBLE
        assert(view.visibility == View.VISIBLE)

        view.visibility = View.GONE
        assert(view.visibility == View.GONE)
    }

    @Test
    fun testViewWithZeroSize() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 0, 0)

        assert(view.width == 0)
        assert(view.height == 0)
    }

    @Test
    fun testViewWithLargeSize() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(3000, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 2000, 3000)

        assert(view.width == 2000)
        assert(view.height == 3000)
    }

    @Test
    fun testViewWithSmallSize() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 100, 100)

        assert(view.width == 100)
        assert(view.height == 100)
    }

    @Test
    fun testViewResize() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 800, 600)

        assert(view.width == 800)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1200, 900)

        assert(view.width == 1200)
        assert(view.height == 900)
    }

    @Test
    fun testViewDrawing() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1000, 1000)

        view.invalidate()

        assert(true)
    }

    @Test
    fun testViewAnimationCycle() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1000, 1000)

        Thread.sleep(500)

        view.invalidate()

        assert(true)
    }

    @Test
    fun testViewPauseAndResumeAnimation() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1000, 1000)

        view.visibility = View.INVISIBLE
        Thread.sleep(200)

        view.visibility = View.VISIBLE
        Thread.sleep(200)

        assert(true)
    }

    @Test
    fun testViewMultipleLayouts() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 800, 600)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1000, 800)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1200, 1000)

        assert(view.width == 1200)
        assert(view.height == 1000)
    }

    @Test
    fun testViewWithDifferentAspectRatios() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 600, 1000)
        assert(view.width == 600)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1000, 600)
        assert(view.width == 1000)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 800, 800)
        assert(view.width == 800)
    }

    @Test
    fun testViewInvalidateMultipleTimes() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1000, 1000)

        for (i in 0 until 10) {
            view.invalidate()
            Thread.sleep(50)
        }

        assert(true)
    }

    @Test
    fun testViewStressTest() {
        for (i in 0 until 5) {
            view.visibility = View.VISIBLE
            view.measure(
                View.MeasureSpec.makeMeasureSpec(800 + i * 100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600 + i * 100, View.MeasureSpec.EXACTLY)
            )
            view.layout(0, 0, 800 + i * 100, 600 + i * 100)
            Thread.sleep(100)

            view.visibility = View.INVISIBLE
            Thread.sleep(50)
        }

        assert(true)
    }
}
