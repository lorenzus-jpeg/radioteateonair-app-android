package it.teateonair.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.Window
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.jsoup.Jsoup
import java.util.*

class ScheduleModal(private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled")
    fun show(url: String) {
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
            text = "Palinsesto Oggi"
            textSize = 18f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeButton = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            layoutParams = LinearLayout.LayoutParams(48, 48)
            setPadding(12, 12, 12, 12)
            background = createRippleDrawable()
            setOnClickListener { dialog.dismiss() }
        }

        headerLayout.addView(titleText)
        headerLayout.addView(closeButton)

        val progressLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 32)
            setBackgroundColor(Color.parseColor("#1a1a1a"))
        }

        val progressBar = ProgressBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48)
            indeterminateDrawable.setColorFilter(Color.parseColor("#00FF88"), android.graphics.PorterDuff.Mode.SRC_IN)
        }

        val loadingText = TextView(context).apply {
            text = "Caricamento..."
            textSize = 16f
            setTextColor(Color.parseColor("#00FF88"))
            setPadding(24, 0, 0, 0)
        }

        progressLayout.addView(progressBar)
        progressLayout.addView(loadingText)

        val webView = WebView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressLayout.visibility = View.VISIBLE
                view?.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressLayout.visibility = View.GONE
                view?.visibility = View.VISIBLE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("https://radioteateonair.it/palinsesto")) {
                    return false
                }
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    return true
                }
            }
        }

        mainLayout.addView(headerLayout)
        mainLayout.addView(progressLayout)
        mainLayout.addView(webView)

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

        val html = CacheManager.getScheduleHtml()

        if (html != null) {
            webView.loadDataWithBaseURL(url, html, "text/html", "UTF-8", null)
        } else {
            Thread {
                try {
                    val connection = java.net.URL(url).openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    val doc = Jsoup.parse(connection.getInputStream(), "UTF-8", url)

                    val dayNames = listOf("Domenica", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato")
                    val todayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
                    val todayName = dayNames[todayIndex]

                    val filteredDivs = doc.select("div.qt-part-show-schedule-day-item").toList().filter { element ->
                        element.select("span.qt-day").any { daySpan ->
                            daySpan.text().equals(todayName, ignoreCase = true)
                        }
                    }

                    filteredDivs.forEach { div ->
                        div.select(".qt-header-bg").forEach { bgDiv ->
                            val bgUrl = bgDiv.attr("data-bgimage")
                            if (bgUrl.isNotEmpty()) {
                                bgDiv.attr(
                                    "style",
                                    "background-image:url('$bgUrl'); background-size:cover; background-position:center; background-repeat:no-repeat;"
                                )
                            }
                        }
                    }

                    val finalContent = if (filteredDivs.isNotEmpty()) {
                        filteredDivs.joinToString("\n") { it.outerHtml() }
                    } else {
                        "<div style='display:flex;align-items:center;justify-content:center;min-height:300px;text-align:center;padding:32px;'><div><div style='font-size:48px;margin-bottom:16px;'>📻</div><h2 style='color:#00FF88;margin:0 0 8px 0;font-size:24px;'>E' tutto per oggi</h2><p style='color:#666;margin:0;'>Torna domani per il nuovo palinsesto</p></div></div>"
                    }

                    val cssLinks = doc.select("link[rel=stylesheet]").joinToString("\n") {
                        """<link rel="stylesheet" href="${it.absUrl("href")}">"""
                    }

                    val fallbackHtml = """
                        <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                $cssLinks
                                <style>
                                    body { 
                                        margin: 0; 
                                        padding: 16px; 
                                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
                                        background: #fff; 
                                        color: #000; 
                                        line-height: 1.4;
                                        font-size: 14px;
                                    }
                                    img, iframe { 
                                        max-width: 100% !important; 
                                        height: auto !important; 
                                        display: block; 
                                        margin: 8px auto; 
                                    }
                                    .qt-header-bg {
                                        min-height: 120px;
                                        background-size: cover !important;
                                        background-position: center !important;
                                        background-repeat: no-repeat !important;
                                        border-radius: 8px;
                                        margin-bottom: 16px;
                                    }
                                    .qt-part-show-schedule-day-item {
                                        margin-bottom: 20px;
                                        padding: 12px;
                                        border-radius: 8px;
                                        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                                    }
                                    @media (max-width: 600px) {
                                        body { padding: 12px; font-size: 13px; }
                                        .qt-header-bg { min-height: 100px; }
                                    }
                                    @media (min-width: 768px) {
                                        body { padding: 24px; font-size: 16px; }
                                        .qt-header-bg { min-height: 160px; }
                                    }
                                </style>
                            </head>
                            <body>
                                $finalContent
                            </body>
                        </html>
                    """.trimIndent()

                    (context as? android.app.Activity)?.runOnUiThread {
                        if (dialog.isShowing) {
                            webView.loadDataWithBaseURL(url, fallbackHtml, "text/html", "UTF-8", null)
                        }
                    }
                } catch (e: Exception) {
                    (context as? android.app.Activity)?.runOnUiThread {
                        if (dialog.isShowing) {
                            val errorHtml = "<html><body style='padding:40px;text-align:center;background:#f8f9fa;'><div style='background:white;padding:32px;border-radius:12px;'><h3 style='color:#dc3545;'>⚠️ Errore</h3><p>Impossibile caricare il palinsesto</p></div></body></html>"
                            webView.loadData(errorHtml, "text/html", "UTF-8")
                            progressLayout.visibility = View.GONE
                            webView.visibility = View.VISIBLE
                        }
                    }
                }
            }.start()
        }
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