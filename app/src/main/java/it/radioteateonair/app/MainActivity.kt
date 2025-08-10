package it.radioteateonair.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.jsoup.Jsoup
import java.util.*
import android.net.Uri
import android.widget.ImageView
import org.json.JSONObject
import java.net.URL
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import android.util.DisplayMetrics
import android.content.res.Configuration
import android.text.TextUtils
import android.graphics.Paint
import android.view.ViewTreeObserver


class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var playButton: Button
    private lateinit var bottomBar: LinearLayout
    private lateinit var songTitle: TextView
    private lateinit var artistName: TextView
    private lateinit var handler: Handler
    private lateinit var topInsetSpacer: View
    private lateinit var bottomInsetSpacer: View
    private val executor = java.util.concurrent.Executors.newSingleThreadExecutor()

    private var isPlaying = false
    private var isFirstLoad = true // Track if this is the first metadata load

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        initializeViews()

        // Setup system UI handling
        setupSystemUI()

        // Setup click listeners
        setupClickListeners()

        handler = Handler(Looper.getMainLooper())
    }

    private fun initializeViews() {
        startButton = findViewById(R.id.startButton)
        playButton = findViewById(R.id.playButton)
        bottomBar = findViewById(R.id.bottomBar)
        songTitle = findViewById(R.id.songTitle)
        artistName = findViewById(R.id.artistName)
        topInsetSpacer = findViewById(R.id.topInsetSpacer)
        bottomInsetSpacer = findViewById(R.id.bottomInsetSpacer)

        // Setup layout listener for text size adjustments
        bottomBar.viewTreeObserver.addOnGlobalLayoutListener {
            if (bottomBar.width > 0 && isPlaying) {
                adjustTextSizes()
            }
        }

        // The AnimatedBackgroundView will start automatically when attached to window
    }

    private fun setupSystemUI() {
        // Handle system UI insets for proper spacing
        ViewCompat.setOnApplyWindowInsetsListener(topInsetSpacer) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                height = statusBarHeight
            }
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomInsetSpacer) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                height = navBarHeight
            }
            insets
        }

        // Enable edge-to-edge display
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener {
            if (!isPlaying) {
                startService(Intent(this, RadioService::class.java))
                isPlaying = true
                animateToPlayingState()
                startSongInfoUpdater()
            }
        }

        playButton.setOnClickListener {
            stopService(Intent(this, RadioService::class.java))
            isPlaying = false
            animateToStoppedState()
        }

        // Box click listeners with responsive behavior
        findViewById<View>(R.id.box1).setOnClickListener {
            showPopupWithWebView("https://radioteateonair.it/palinsesto")
        }

        findViewById<View>(R.id.box2).setOnClickListener {
            val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.radioteateonair.it/programmi/"))
            startActivity(urlIntent)
        }

        findViewById<View>(R.id.box3).setOnClickListener {
            val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.radioteateonair.it/about-us/"))
            startActivity(urlIntent)
        }

        findViewById<View>(R.id.box4).setOnClickListener {
            showSocialDialog()
        }
    }

    private fun animateToPlayingState() {
        val screenWidth = resources.displayMetrics.widthPixels
        val targetTranslationX = -screenWidth / 2f + getResponsiveValue(96f)
        val targetScale = getResponsiveScaleValue(0.67f)

        startButton.animate()
            .translationX(targetTranslationX)
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(400)
            .withEndAction {
                startButton.visibility = View.GONE
                showBottomBar()
            }
            .start()
    }

    private fun animateToStoppedState() {
        bottomBar.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                bottomBar.visibility = View.GONE
                showStartButton()
            }
            .start()
    }

    private fun showBottomBar() {
        bottomBar.alpha = 0f
        bottomBar.visibility = View.VISIBLE
        bottomBar.animate()
            .alpha(1f)
            .setDuration(200)
            .withEndAction {
                // Ensure text sizing happens after layout is complete
                bottomBar.post {
                    adjustTextSizes()
                }
            }
            .start()
    }

    private fun showStartButton() {
        val screenWidth = resources.displayMetrics.widthPixels
        val initialTranslationX = -screenWidth / 2f + getResponsiveValue(96f)
        val initialScale = getResponsiveScaleValue(0.67f)

        startButton.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationX = initialTranslationX
            scaleX = initialScale
            scaleY = initialScale
            animate()
                .alpha(1f)
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .start()
        }
    }

    private fun getResponsiveValue(baseValue: Float): Float {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

        return when {
            screenWidthDp >= 720 -> baseValue * 1.5f  // Large tablets
            screenWidthDp >= 600 -> baseValue * 1.2f  // Tablets
            else -> baseValue  // Phones
        }
    }

    private fun getResponsiveScaleValue(baseScale: Float): Float {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

        return when {
            screenWidthDp >= 720 -> baseScale * 0.8f  // Large tablets - less scaling
            screenWidthDp >= 600 -> baseScale * 0.9f  // Tablets - moderate scaling
            else -> baseScale  // Phones - original scaling
        }
    }

    private fun showSocialDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_socials, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Make dialog responsive
        dialog.setOnShowListener {
            val window = dialog.window
            val displayMetrics = resources.displayMetrics
            val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

            val dialogWidth = when {
                screenWidthDp >= 720 -> (displayMetrics.widthPixels * 0.6).toInt()  // 60% on large tablets
                screenWidthDp >= 600 -> (displayMetrics.widthPixels * 0.7).toInt()  // 70% on tablets
                else -> (displayMetrics.widthPixels * 0.9).toInt()  // 90% on phones
            }

            window?.setLayout(dialogWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        dialogView.findViewById<ImageView>(R.id.instagram).setOnClickListener {
            val url = "https://www.instagram.com/radio_teateonair"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            dialog.dismiss()
        }

        dialogView.findViewById<ImageView>(R.id.facebook).setOnClickListener {
            val url = "https://www.facebook.com/radioteateonair/"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            dialog.dismiss()
        }

        dialogView.findViewById<ImageView>(R.id.youtube).setOnClickListener {
            val url = "https://www.youtube.com/@radioteateonair4409"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            dialog.dismiss()
        }

        dialogView.findViewById<ImageView>(R.id.tiktok).setOnClickListener {
            val url = "https://www.tiktok.com/@radioteateonair"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startSongInfoUpdater() {
        val jsonUrl = "https://nr14.newradio.it:8663/status-json.xsl"

        val updateTask = object : Runnable {
            override fun run() {
                executor.execute {
                    try {
                        val response = URL(jsonUrl).readText()
                        val json = JSONObject(response)
                        val fullTitle = json
                            .getJSONObject("icestats")
                            .getJSONObject("source")
                            .getString("yp_currently_playing")

                        val parts = fullTitle.split(" - ", limit = 2)
                        val artist = parts.getOrNull(0)?.trim() ?: getString(R.string.unknown_artist)
                        val song = parts.getOrNull(1)?.trim() ?: getString(R.string.unknown_title)

                        handler.post {
                            updateSongInfo(artist, song)
                            isFirstLoad = false // Mark that we've loaded at least once
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        handler.post {
                            // Only show loading on first load, not on subsequent failures
                            if (isFirstLoad) {
                                artistName.text = getString(R.string.loading)
                                songTitle.text = ""
                                adjustTextSizes()
                            }
                            // If it's not the first load, keep the previous song info displayed
                        }
                    } finally {
                        handler.postDelayed(this, 10000)
                    }
                }
            }
        }

        handler.post(updateTask)
    }

    private fun updateSongInfo(artist: String, song: String) {
        // Update the text
        artistName.text = artist
        songTitle.text = song

        // Enable marquee for title if it's long
        setupMarqueeForTitle()

        // Adjust text sizes based on content length
        adjustTextSizes()
    }

    private fun setupMarqueeForTitle() {
        songTitle.apply {
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1 // Infinite marquee
            isSingleLine = true
            isSelected = true // Required for marquee to work
            isFocusable = true
            isFocusableInTouchMode = true

            // Start marquee after a short delay
            post {
                requestFocus()
            }
        }
    }

    private fun adjustTextSizes() {
        // Get the available width for text
        val availableWidth = getAvailableTextWidth()
        if (availableWidth <= 0) return

        // Adjust artist name size
        adjustTextSize(artistName, availableWidth, 10f, 18f)

        // Adjust song title size
        adjustTextSize(songTitle, availableWidth, 12f, 20f)
    }

    private fun getAvailableTextWidth(): Int {
        val playButtonWidth = resources.getDimensionPixelSize(R.dimen.play_button_size)
        val padding = resources.getDimensionPixelSize(R.dimen.song_info_padding) * 2
        val margins = resources.getDimensionPixelSize(R.dimen.bottom_bar_padding) * 2

        return bottomBar.width - playButtonWidth - padding - margins - 100 // Extra margin for safety
    }

    private fun adjustTextSize(textView: TextView, availableWidth: Int, minSize: Float, maxSize: Float) {
        if (availableWidth <= 0) return

        val paint = Paint()
        paint.typeface = textView.typeface

        var currentSize = maxSize
        val text = textView.text.toString()

        // For marquee text (song title), we don't need to fit in width
        if (textView == songTitle) {
            // Use content-based sizing for song title
            when {
                text.length <= 20 -> currentSize = maxSize
                text.length <= 35 -> currentSize = maxSize * 0.9f
                text.length <= 50 -> currentSize = maxSize * 0.8f
                else -> currentSize = maxSize * 0.7f
            }
        } else {
            // For artist name, fit to available width
            paint.textSize = currentSize * resources.displayMetrics.scaledDensity

            while (currentSize > minSize && paint.measureText(text) > availableWidth) {
                currentSize -= 0.5f
                paint.textSize = currentSize * resources.displayMetrics.scaledDensity
            }
        }

        // Apply size with smooth transition
        textView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .withEndAction {
                textView.textSize = currentSize
            }
            .start()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showPopupWithWebView(url: String) {
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.webViewClient = WebViewClient()

        val dialog = AlertDialog.Builder(this)
            .setView(webView)
            .setNegativeButton(getString(R.string.close)) { dialog, _ -> dialog.dismiss() }
            .create()

        // Make WebView dialog responsive
        dialog.setOnShowListener {
            val window = dialog.window
            val displayMetrics = resources.displayMetrics
            val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
            val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density

            val dialogWidth = when {
                screenWidthDp >= 720 -> (displayMetrics.widthPixels * 0.8).toInt()  // 80% on large tablets
                screenWidthDp >= 600 -> (displayMetrics.widthPixels * 0.85).toInt() // 85% on tablets
                else -> (displayMetrics.widthPixels * 0.95).toInt() // 95% on phones
            }

            val dialogHeight = when {
                screenHeightDp >= 800 -> (displayMetrics.heightPixels * 0.8).toInt() // 80% on tall screens
                else -> (displayMetrics.heightPixels * 0.85).toInt() // 85% on normal screens
            }

            window?.setLayout(dialogWidth, dialogHeight)
        }

        dialog.show()

        Thread {
            try {
                val doc = Jsoup.connect(url).get()

                val dayNames = listOf("Domenica", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato")
                val todayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
                val todayName = dayNames[todayIndex]

                val allScheduleDivs = doc.select("div.qt-part-show-schedule-day-item").toList()

                val filteredDivs = allScheduleDivs.filter { element ->
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
                    "<p>${getString(R.string.no_programs_today, todayName)}</p>"
                }

                val cssLinks = doc.select("link[rel=stylesheet]").joinToString("\n") {
                    """<link rel="stylesheet" href="${it.absUrl("href")}">"""
                }

                // Create responsive HTML with better mobile optimization
                val fullHtml = """
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

                runOnUiThread {
                    webView.loadDataWithBaseURL(url, fullHtml, "text/html", "UTF-8", null)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    webView.loadData(
                        "<html><body style='padding:20px;font-family:sans-serif;'><h3>Errore di connessione</h3><p>Impossibile caricare il palinsesto. Verifica la connessione internet.</p></body></html>",
                        "text/html",
                        "UTF-8"
                    )
                }
            }
        }.start()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isPlaying) {
            stopService(Intent(this, RadioService::class.java))
        }
        executor.shutdown()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle orientation changes gracefully
        if (isPlaying) {
            // Refresh the layout if needed
            handler.postDelayed({
                // Minor delay to ensure layout is settled
            }, 100)
        }
    }
}