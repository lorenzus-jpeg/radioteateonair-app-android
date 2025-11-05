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
import android.view.Gravity
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

class ProgramsModal(private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled")
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
            text = "Programmi"
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
            visibility = android.view.View.GONE

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressLayout.visibility = android.view.View.VISIBLE
                view?.visibility = android.view.View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressLayout.visibility = android.view.View.GONE
                view?.visibility = android.view.View.VISIBLE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("https://www.radioteateonair.it/programmi/")) {
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

        val html = CacheManager.getProgramsHtml()

        if (html != null) {
            webView.loadDataWithBaseURL("https://www.radioteateonair.it/programmi/", html, "text/html", "UTF-8", null)
        } else {
            Thread {
                try {
                    val connection = java.net.URL("https://www.radioteateonair.it/programmi/").openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    val doc = Jsoup.parse(connection.getInputStream(), "UTF-8", "https://www.radioteateonair.it/programmi/")

                    doc.select("*").forEach { element ->
                        if (element.ownText().equals("PROGRAMMI", ignoreCase = true)) {
                            element.remove()
                        }
                    }

                    doc.select("*").forEach { element ->
                        val text = element.ownText().lowercase()
                        if (text.contains("podcast attivi") || text.contains("podcast archiviati")) {
                            element.attr("style", "${element.attr("style")}; color: black !important; font-variant: small-caps !important; font-weight: bold !important;")
                        }
                    }

                    val programs = doc.select("article, .program-item, .post, .entry, .content-item, .program, .show").ifEmpty {
                        doc.select("div[class*='program'], div[class*='show'], div[class*='post']").ifEmpty {
                            doc.select("div:has(h1), div:has(h2), div:has(h3)")
                        }
                    }

                    val finalContent = if (programs.isNotEmpty()) {
                        val processedPrograms = programs.map { program ->
                            program.select("nav, .nav, .navigation, .sidebar, .widget, script, style").remove()

                            program.select("img").forEach { img ->
                                val src = img.attr("src")
                                if (src.isNotEmpty()) {
                                    if (src.startsWith("/")) {
                                        img.attr("src", "https://www.radioteateonair.it$src")
                                    }
                                    img.attr("style", "max-width: 100%; height: auto; border-radius: 8px; margin: 8px 0;")
                                }
                            }

                            program.attr("style", "margin-bottom: 24px; padding: 16px; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); background: white;")

                            program.outerHtml()
                        }.joinToString("\n")

                        processedPrograms
                    } else {
                        val mainContent = doc.select("main, .main, .content, .site-content, #content, .entry-content").first()

                        mainContent?.let { content ->
                            content.select("nav, .nav, .navigation, .sidebar, .widget, script, style, header, footer").remove()

                            content.select("img").forEach { img ->
                                val src = img.attr("src")
                                if (src.startsWith("/")) {
                                    img.attr("src", "https://www.radioteateonair.it$src")
                                }
                            }

                            content.html()
                        } ?: "<div style='text-align: center; padding: 40px;'><p>Nessun programma trovato</p></div>"
                    }

                    val cssLinks = doc.select("link[rel=stylesheet]").take(2).joinToString("\n") {
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
                                        background: #f8f9fa; 
                                        color: #333; 
                                        line-height: 1.5;
                                        font-size: 14px;
                                    }
                                    img { 
                                        max-width: 100% !important; 
                                        height: auto !important; 
                                        display: block; 
                                        margin: 8px auto; 
                                        border-radius: 8px;
                                    }
                                    h1, h2, h3, h4, h5, h6 {
                                        color: #00FF88;
                                        margin-top: 20px;
                                        margin-bottom: 12px;
                                    }
                                    p {
                                        margin-bottom: 16px;
                                        color: #555;
                                    }
                                    a {
                                        color: #00FF88;
                                        text-decoration: none;
                                    }
                                    a:hover {
                                        text-decoration: underline;
                                    }
                                    .program-item, article, .post {
                                        background: white;
                                        margin-bottom: 20px;
                                        padding: 16px;
                                        border-radius: 12px;
                                        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                                    }
                                    *[style*="small-caps"] {
                                        color: black !important;
                                        font-variant: small-caps !important;
                                        font-weight: bold !important;
                                    }
                                    @media (max-width: 600px) {
                                        body { padding: 12px; font-size: 13px; }
                                    }
                                    @media (min-width: 768px) {
                                        body { padding: 24px; font-size: 16px; }
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
                            webView.loadDataWithBaseURL("https://www.radioteateonair.it/programmi/", fallbackHtml, "text/html", "UTF-8", null)
                        }
                    }
                } catch (e: Exception) {
                    (context as? android.app.Activity)?.runOnUiThread {
                        if (dialog.isShowing) {
                            val errorHtml = "<html><body style='padding:40px;text-align:center;background:#f8f9fa;'><div style='background:white;padding:32px;border-radius:12px;'><h3 style='color:#dc3545;'>⚠️ Errore</h3><p>Impossibile caricare i programmi</p></div></body></html>"
                            webView.loadData(errorHtml, "text/html", "UTF-8")
                            progressLayout.visibility = android.view.View.GONE
                            webView.visibility = android.view.View.VISIBLE
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
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
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