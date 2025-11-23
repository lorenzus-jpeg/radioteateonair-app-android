package it.teateonair.app

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView

class MyOptions(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "teateonair_settings"
        private const val KEY_WAVE_SHAPE = "wave_shape"
        const val WAVE_SHAPE_WAVE = "wave"
        const val WAVE_SHAPE_FLAT = "flat"
        const val WAVE_SHAPE_TRIANGLE = "triangle"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            setPadding(0, 0, 0, 0)
        }

        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#00FF88"))
            setPadding(24, 16, 16, 16)
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleText = TextView(context).apply {
            text = context.getString(R.string.le_mie_opzioni)
            textSize = 18f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeButton = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            layoutParams = LinearLayout.LayoutParams(48, 48)
            setPadding(8, 8, 8, 8)
            setColorFilter(Color.BLACK)
            background = createRippleDrawable()
            setOnClickListener { dialog.dismiss() }
        }

        headerLayout.addView(titleText)
        headerLayout.addView(closeButton)

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
        }

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Wave Shape Section
        val waveShapeTitle = TextView(context).apply {
            text = context.getString(R.string.wave_shape_title)
            textSize = 16f
            setTextColor(Color.parseColor("#00FF88"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }
        contentLayout.addView(waveShapeTitle)

        // Get current selection
        val currentWaveShape = getWaveShape()

        // Radio Group for wave shape
        val radioGroup = RadioGroup(context).apply {
            orientation = RadioGroup.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
        }

        // Wave option
        val waveRadio = RadioButton(context).apply {
            text = context.getString(R.string.wave_smooth)
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            layoutParams = RadioGroup.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#00FF88"))
        }

        // Flat option
        val flatRadio = RadioButton(context).apply {
            text = context.getString(R.string.wave_flat)
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            layoutParams = RadioGroup.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#00FF88"))
        }

        // Triangle option
        val triangleRadio = RadioButton(context).apply {
            text = context.getString(R.string.wave_triangle)
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            layoutParams = RadioGroup.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#00FF88"))
        }

        radioGroup.addView(waveRadio)
        radioGroup.addView(flatRadio)
        radioGroup.addView(triangleRadio)

        // Set the correct selection using RadioGroup.check()
        when (currentWaveShape) {
            WAVE_SHAPE_WAVE -> radioGroup.check(waveRadio.id)
            WAVE_SHAPE_FLAT -> radioGroup.check(flatRadio.id)
            WAVE_SHAPE_TRIANGLE -> radioGroup.check(triangleRadio.id)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newShape = when (checkedId) {
                waveRadio.id -> WAVE_SHAPE_WAVE
                flatRadio.id -> WAVE_SHAPE_FLAT
                triangleRadio.id -> WAVE_SHAPE_TRIANGLE
                else -> WAVE_SHAPE_WAVE
            }
            saveWaveShape(newShape)

            // Notify MainActivity to update
            if (context is MainActivity) {
                context.updateWaveShape(newShape)
            }
        }

        contentLayout.addView(radioGroup)

        // Description
        val description = TextView(context).apply {
            text = context.getString(R.string.wave_description)
            textSize = 13f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
            setLineSpacing(8f, 1.1f)
        }
        contentLayout.addView(description)

        scrollView.addView(contentLayout)
        mainLayout.addView(headerLayout)
        mainLayout.addView(scrollView)

        dialog.setContentView(mainLayout)

        dialog.window?.let { window ->
            window.setLayout(
                (context.resources.displayMetrics.widthPixels * 0.95).toInt(),
                (context.resources.displayMetrics.heightPixels * 0.80).toInt()
            )
            window.setGravity(Gravity.CENTER)
            window.setBackgroundDrawableResource(android.R.color.transparent)

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f * context.resources.displayMetrics.density
                setColor(Color.parseColor("#1a1a1a"))
            }
            window.setBackgroundDrawable(drawable)
        }

        dialog.show()
    }

    fun getWaveShape(): String {
        return sharedPreferences.getString(KEY_WAVE_SHAPE, WAVE_SHAPE_WAVE) ?: WAVE_SHAPE_WAVE
    }

    private fun saveWaveShape(shape: String) {
        sharedPreferences.edit().putString(KEY_WAVE_SHAPE, shape).apply()
    }

    private fun createRippleDrawable(): android.graphics.drawable.Drawable {
        val rippleColor = Color.parseColor("#33000000")
        val content = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(rippleColor),
                content,
                null
            )
        } else {
            content
        }
    }
}