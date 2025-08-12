package it.radioteateonair.app

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URL
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        const val ACTION_AUDIO_STOPPED = "it.radioteateonair.app.AUDIO_STOPPED"
        const val ACTION_AUDIO_STARTED = "it.radioteateonair.app.AUDIO_STARTED"
    }

    private lateinit var startButton: Button
    private lateinit var playButton: Button
    private lateinit var bottomBar: LinearLayout
    private lateinit var songTitle: TextView
    private lateinit var artistName: TextView
    private lateinit var handler: Handler
    private lateinit var topInsetSpacer: View
    private lateinit var bottomInsetSpacer: View
    private val executor = Executors.newSingleThreadExecutor()

    private var isPlaying = false
    private var isFirstLoad = true

    // BroadcastReceiver to listen for service state changes
    private val radioStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_AUDIO_STOPPED -> {
                    // Audio was stopped from notification or service
                    isPlaying = false
                    runOnUiThread {
                        animateToStoppedState()
                    }
                }
                ACTION_AUDIO_STARTED -> {
                    // Audio was started from notification
                    if (!isPlaying) {
                        isPlaying = true
                        runOnUiThread {
                            animateToPlayingState()
                            startSongInfoUpdater()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupSystemUI()
        requestNotificationPermission()
        setupClickListeners()
        registerRadioStateReceiver()
        // REMOVED: createPersistentNotification() - Don't create notification on app start

        handler = Handler(Looper.getMainLooper())
    }

    private fun createPersistentNotification() {
        // Create the persistent notification only when user first interacts
        if (hasNotificationPermission()) {
            val intent = Intent(this, RadioService::class.java)
            // Don't specify an action, just start the service to show notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun registerRadioStateReceiver() {
        val filter = IntentFilter()
        filter.addAction(ACTION_AUDIO_STOPPED)
        filter.addAction(ACTION_AUDIO_STARTED)
        LocalBroadcastManager.getInstance(this).registerReceiver(radioStateReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(radioStateReceiver)
        if (isPlaying) {
            val intent = Intent(this, RadioService::class.java).apply {
                action = RadioService.ACTION_STOP
            }
            startService(intent)
        }
        executor.shutdown()
    }

    private fun initializeViews() {
        startButton = findViewById(R.id.startButton)
        playButton = findViewById(R.id.playButton)
        bottomBar = findViewById(R.id.bottomBar)
        songTitle = findViewById(R.id.songTitle)
        artistName = findViewById(R.id.artistName)
        topInsetSpacer = findViewById(R.id.topInsetSpacer)
        bottomInsetSpacer = findViewById(R.id.bottomInsetSpacer)

        bottomBar.viewTreeObserver.addOnGlobalLayoutListener {
            if (bottomBar.width > 0 && isPlaying) {
                adjustTextSizes()
            }
        }
    }

    private fun setupSystemUI() {
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

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener {
            if (!isPlaying) {
                if (hasNotificationPermission()) {
                    startRadioService()
                } else {
                    requestNotificationPermission()
                }
            }
        }

        playButton.setOnClickListener {
            val intent = Intent(this, RadioService::class.java).apply {
                action = RadioService.ACTION_STOP
            }
            startService(intent)
            isPlaying = false
            animateToStoppedState()
        }

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

        // UPDATED SOCIAL MEDIA URLs - VERIFIED AND CORRECTED
        findViewById<View>(R.id.socialFacebook).setOnClickListener {
            val url = "https://www.facebook.com/radioteateonair"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        findViewById<View>(R.id.socialInstagram).setOnClickListener {
            val url = "https://www.instagram.com/radio_teateonair"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        findViewById<View>(R.id.socialTikTok).setOnClickListener {
            val url = "https://www.tiktok.com/@radioteateonair"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        findViewById<View>(R.id.socialYouTube).setOnClickListener {
            val url = "https://www.youtube.com/@radioteateonair4409"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun startRadioService() {
        // Create persistent notification on first play
        if (hasNotificationPermission()) {
            createPersistentNotification()
        }

        val intent = Intent(this, RadioService::class.java).apply {
            action = RadioService.ACTION_PLAY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isPlaying = true
        animateToPlayingState()
        startSongInfoUpdater()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Notifiche abilitate! Tocca play per iniziare.")
                // DON'T auto-create notification when permission is granted
            } else {
                showToast("Le notifiche sono necessarie per controllare la radio dalla barra delle notifiche")
            }
        }
    }

    private fun animateToPlayingState() {
        val screenWidth = resources.displayMetrics.widthPixels
        val targetTranslationX = -screenWidth / 2f + getResponsiveValue(80f)
        val targetScale = getResponsiveScaleValue(0.75f)

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
                bottomBar.post {
                    adjustTextSizes()
                }
            }
            .start()
    }

    private fun showStartButton() {
        val screenWidth = resources.displayMetrics.widthPixels
        val initialTranslationX = -screenWidth / 2f + getResponsiveValue(80f)
        val initialScale = getResponsiveScaleValue(0.75f)

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
            screenWidthDp >= 720 -> baseValue * 1.5f
            screenWidthDp >= 600 -> baseValue * 1.2f
            else -> baseValue
        }
    }

    private fun getResponsiveScaleValue(baseScale: Float): Float {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

        return when {
            screenWidthDp >= 720 -> baseScale * 0.8f
            screenWidthDp >= 600 -> baseScale * 0.9f
            else -> baseScale
        }
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
                            isFirstLoad = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        handler.post {
                            if (isFirstLoad) {
                                artistName.text = getString(R.string.loading)
                                songTitle.text = ""
                                adjustTextSizes()
                            }
                        }
                    } finally {
                        // Only continue updating if still playing
                        if (isPlaying) {
                            handler.postDelayed(this, 2000)
                        }
                    }
                }
            }
        }

        handler.post(updateTask)
    }

    private fun updateSongInfo(artist: String, song: String) {
        artistName.text = artist
        songTitle.text = song

        // Restart marquee for both fields when content updates
        handler.postDelayed({
            setupMarqueeForTitle()
        }, 100) // Small delay to ensure text is set

        adjustTextSizes()
    }

    private fun setupMarqueeForTitle() {
        // Setup marquee for song title
        songTitle.apply {
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1 // Infinite marquee
            isSingleLine = true
            isSelected = true
            isFocusable = true
            isFocusableInTouchMode = true

            post {
                requestFocus()
            }
        }

        // Setup marquee for artist name as well
        artistName.apply {
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1 // Infinite marquee
            isSingleLine = true
            isSelected = true
            isFocusable = true
            isFocusableInTouchMode = true

            post {
                requestFocus()
            }
        }
    }

    private fun adjustTextSizes() {
        val availableWidth = getAvailableTextWidth()
        if (availableWidth <= 0) return

        // REDUCED FONT SIZES for smaller player bar
        adjustTextSize(artistName, availableWidth, 8f, 12f)  // Reduced from 10f-18f
        adjustTextSize(songTitle, availableWidth, 9f, 14f)   // Reduced from 12f-20f
    }

    private fun getAvailableTextWidth(): Int {
        val playButtonWidth = 48 * resources.displayMetrics.density.toInt() // Updated button size
        val padding = resources.getDimensionPixelSize(R.dimen.song_info_padding) * 2
        val margins = resources.getDimensionPixelSize(R.dimen.bottom_bar_padding) * 2

        return bottomBar.width - playButtonWidth - padding - margins - 100
    }

    private fun adjustTextSize(textView: TextView, availableWidth: Int, minSize: Float, maxSize: Float) {
        if (availableWidth <= 0) return

        val paint = Paint()
        paint.typeface = textView.typeface

        var currentSize = maxSize
        val text = textView.text.toString()

        if (textView == songTitle) {
            when {
                text.length <= 20 -> currentSize = maxSize
                text.length <= 35 -> currentSize = maxSize * 0.9f
                text.length <= 50 -> currentSize = maxSize * 0.8f
                else -> currentSize = maxSize * 0.7f
            }
        } else {
            paint.textSize = currentSize * resources.displayMetrics.scaledDensity

            while (currentSize > minSize && paint.measureText(text) > availableWidth) {
                currentSize -= 0.5f
                paint.textSize = currentSize * resources.displayMetrics.scaledDensity
            }
        }

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
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
        webView.webViewClient = WebViewClient()

        // Create loading progress bar
        val progressBar = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
                setMargins(0, 100, 0, 0)
            }
            indeterminateDrawable.setColorFilter(
                0xFF00FF88.toInt(),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        val loadingText = TextView(this).apply {
            text = getString(R.string.loading)
            textSize = 16f
            setTextColor(0xFF00FF88.toInt())
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }
        }

        val loadingContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            addView(progressBar)
            addView(loadingText)
        }

        val mainContainer = FrameLayout(this).apply {
            addView(loadingContainer)
            addView(webView)
        }

        // Initially hide WebView and show loading
        webView.visibility = View.GONE
        loadingContainer.visibility = View.VISIBLE

        val dialog = AlertDialog.Builder(this)
            .setView(mainContainer)
            .setNegativeButton(getString(R.string.close)) { dialog, _ -> dialog.dismiss() }
            .create()

        // Style the close button to green
        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            button?.setTextColor(0xFF00FF88.toInt())

            val window = dialog.window
            val displayMetrics = resources.displayMetrics
            val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
            val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density

            val dialogWidth = when {
                screenWidthDp >= 720 -> (displayMetrics.widthPixels * 0.8).toInt()
                screenWidthDp >= 600 -> (displayMetrics.widthPixels * 0.85).toInt()
                else -> (displayMetrics.widthPixels * 0.95).toInt()
            }

            val dialogHeight = when {
                screenHeightDp >= 800 -> (displayMetrics.heightPixels * 0.8).toInt()
                else -> (displayMetrics.heightPixels * 0.85).toInt()
            }

            window?.setLayout(dialogWidth, dialogHeight)
        }

        dialog.show()

        // Load content in background thread with faster timeout
        Thread {
            try {
                // Set connection timeout for faster response
                val connection = java.net.URL(url).openConnection()
                connection.connectTimeout = 5000 // 5 seconds
                connection.readTimeout = 10000 // 10 seconds

                val doc = Jsoup.parse(connection.getInputStream(), "UTF-8", url)

                val dayNames = listOf("Domenica", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato")
                val todayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
                val todayName = dayNames[todayIndex]

                val allScheduleDivs = doc.select("div.qt-part-show-schedule-day-item").toList()

                val filteredDivs = allScheduleDivs.filter { element ->
                    element.select("span.qt-day").any { daySpan ->
                        daySpan.text().equals(todayName, ignoreCase = true)
                    }
                }

                // RESTORED ORIGINAL IMAGE HANDLING
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

                // RESTORED ORIGINAL HTML STRUCTURE
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
                    // Hide loading and show WebView
                    loadingContainer.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                    webView.loadDataWithBaseURL(url, fullHtml, "text/html", "UTF-8", null)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    loadingContainer.visibility = View.GONE
                    webView.visibility = View.VISIBLE
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isPlaying) {
            handler.postDelayed({
                // Refresh layout after orientation change
            }, 100)
        }
    }
}