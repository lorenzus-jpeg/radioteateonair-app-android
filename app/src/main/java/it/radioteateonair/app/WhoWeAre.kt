package it.teateonair.app

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class WhoWeAre(private val context: Context) {

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
            text = "Chi Siamo"
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

        val aboutUsText = context.getString(R.string.about_us_content)

        val wordsToHighlight = listOf(
            "Erga Omnes",
            "musica",
            "Teate On Air",
            "Chieti"
        )

        // Split text into paragraphs
        val paragraphs = aboutUsText.split("\n\n")

        for ((index, paragraph) in paragraphs.withIndex()) {
            if (paragraph.isNotBlank()) {
                val styledText = createStyledText(paragraph, wordsToHighlight)

                val textView = TextView(context).apply {
                    text = styledText
                    textSize = 14f
                    setTextColor(Color.parseColor("#2c3e50"))

                    typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Typeface.create("sans-serif-light", Typeface.NORMAL)
                    } else {
                        Typeface.DEFAULT
                    }

                    setLineSpacing(12f, 1.15f)
                    letterSpacing = 0.02f

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
                    }

                    setShadowLayer(1f, 0f, 1f, Color.parseColor("#10000000"))
                }

                contentLayout.addView(textView)

                // Add separator between paragraphs (except after the last one)
                if (index < paragraphs.size - 1) {
                    val separator = View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                        ).apply {
                            topMargin = 16
                            bottomMargin = 16
                        }
                        setBackgroundColor(Color.parseColor("#e0e0e0"))
                    }
                    contentLayout.addView(separator)
                }
            }
        }

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

    private fun createStyledText(text: String, wordsToHighlight: List<String>): SpannableStringBuilder {
        val spannableBuilder = SpannableStringBuilder(text)
        val greenColor = Color.parseColor("#00FF88")

        for (word in wordsToHighlight) {
            var startIndex = 0
            while (startIndex < text.length) {
                val foundIndex = text.indexOf(word, startIndex, ignoreCase = true)
                if (foundIndex == -1) break

                val endIndex = foundIndex + word.length

                if ((foundIndex == 0 || !text[foundIndex - 1].isLetterOrDigit()) &&
                    (endIndex == text.length || !text[endIndex].isLetterOrDigit())
                ) {
                    spannableBuilder.setSpan(
                        ForegroundColorSpan(greenColor),
                        foundIndex,
                        endIndex,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableBuilder.setSpan(
                        StyleSpan(Typeface.BOLD),
                        foundIndex,
                        endIndex,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    startIndex = endIndex
                }
            }
        }

        return spannableBuilder
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