package it.teateonair.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.Window
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
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
import android.graphics.Color
import android.text.Layout
import android.graphics.text.LineBreaker
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        const val ACTION_AUDIO_STOPPED = "it.teateonair.app.AUDIO_STOPPED"
        const val ACTION_AUDIO_STARTED = "it.teateonair.app.ACTION_AUDIO_STARTED"
        private const val CLICK_DEBOUNCE_DELAY = 500L
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
    private var lastClickTime = 0L
    private var isProcessingClick = false

    private val radioStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_AUDIO_STOPPED -> {
                    isPlaying = false
                    isProcessingClick = false
                    runOnUiThread {
                        animateToStoppedState()
                    }
                }
                ACTION_AUDIO_STARTED -> {
                    if (!isPlaying) {
                        isPlaying = true
                        isProcessingClick = false
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

        handler = Handler(Looper.getMainLooper())
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

        val intent = Intent(this, RadioService::class.java)
        stopService(intent)

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

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT

            // For API 30+ (Android 11), use the new method
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            }
        }
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener {
            handlePlayerClick()
        }

        playButton.setOnClickListener {
            handlePlayerClick()
        }

        findViewById<View>(R.id.box1).setOnClickListener {
            showImprovedPalinsestoModal("https://radioteateonair.it/palinsesto")
        }

        findViewById<View>(R.id.box2).setOnClickListener {
            showProgramsModal()
        }

        findViewById<View>(R.id.box3).setOnClickListener {
            showAboutUsModal()
        }

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

    private fun handlePlayerClick() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastClickTime < CLICK_DEBOUNCE_DELAY) {
            return
        }

        if (isProcessingClick) {
            return
        }

        lastClickTime = currentTime
        isProcessingClick = true

        if (!isPlaying) {
            if (hasNotificationPermission()) {
                startRadioService()
            } else {
                isProcessingClick = false
                requestNotificationPermission()
            }
        } else {
            stopRadioService()
        }
    }

    private fun startRadioService() {
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

        handler.postDelayed({
            isProcessingClick = false
        }, 300)
    }

    private fun stopRadioService() {
        val intent = Intent(this, RadioService::class.java).apply {
            action = RadioService.ACTION_STOP
        }
        startService(intent)
        isPlaying = false
        animateToStoppedState()

        handler.postDelayed({
            isProcessingClick = false
        }, 300)
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

        handler.postDelayed({
            setupMarqueeForTitle()
        }, 100)

        adjustTextSizes()
    }

    private fun setupMarqueeForTitle() {
        songTitle.apply {
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1
            isSingleLine = true
            isSelected = true
            isFocusable = true
            isFocusableInTouchMode = true

            post {
                requestFocus()
            }
        }

        artistName.apply {
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1
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

        adjustTextSize(artistName, availableWidth, 8f, 12f)
        adjustTextSize(songTitle, availableWidth, 9f, 14f)
    }

    private fun getAvailableTextWidth(): Int {
        val playButtonWidth = 48 * resources.displayMetrics.density.toInt()
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
    private fun showImprovedPalinsestoModal(url: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            setPadding(0, 0, 0, 0)
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#00FF88"))
            setPadding(24, 16, 16, 16)
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleText = TextView(this).apply {
            text = "Palinsesto Oggi"
            textSize = 18f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeButton = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            layoutParams = LinearLayout.LayoutParams(48, 48)
            setPadding(12, 12, 12, 12)
            background = createRippleDrawable()
            setOnClickListener { dialog.dismiss() }
        }

        headerLayout.addView(titleText)
        headerLayout.addView(closeButton)

        val progressLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 32)
            setBackgroundColor(Color.parseColor("#1a1a1a"))
        }

        val progressBar = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48)
            indeterminateDrawable.setColorFilter(Color.parseColor("#00FF88"), android.graphics.PorterDuff.Mode.SRC_IN)
        }

        val loadingText = TextView(this).apply {
            text = "Caricamento in corso..."
            textSize = 16f
            setTextColor(Color.parseColor("#00FF88"))
            setPadding(24, 0, 0, 0)
        }

        progressLayout.addView(progressBar)
        progressLayout.addView(loadingText)

        val webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
                displayZoomControls = false
                setSupportZoom(false)
            }
        }

        fun showErrorContent() {
            val errorHtml = """
                <html>
                <body style='padding:40px; font-family:sans-serif; text-align:center; background:#f8f9fa;'>
                    <div style='background:white; padding:32px; border-radius:12px; box-shadow:0 4px 12px rgba(0,0,0,0.1);'>
                        <h3 style='color:#dc3545; margin-bottom:16px;'>⚠️ Errore di connessione</h3>
                        <p style='color:#6c757d; margin-bottom:24px;'>Impossibile caricare il palinsesto. Controlla la connessione internet e riprova.</p>
                        <button onclick='window.location.reload()' style='background:#00FF88; color:white; border:none; padding:12px 24px; border-radius:8px; font-size:14px; cursor:pointer;'>
                            Riprova
                        </button>
                    </div>
                </body>
                </html>
            """.trimIndent()

            webView.loadData(errorHtml, "text/html", "UTF-8")
        }

        webView.webViewClient = createWebViewClient("https://radioteateonair.it/palinsesto", progressLayout)

        mainLayout.addView(headerLayout)
        mainLayout.addView(progressLayout)
        mainLayout.addView(webView)

        dialog.setContentView(mainLayout)

        dialog.window?.let { window ->
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.80).toInt()
            )
            window.setGravity(Gravity.CENTER)
            window.setBackgroundDrawableResource(android.R.color.transparent)

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f * resources.displayMetrics.density
                setColor(Color.parseColor("#1a1a1a"))
            }
            window.setBackgroundDrawable(drawable)
        }

        dialog.show()

        Thread {
            try {
                val connection = java.net.URL(url).openConnection()
                connection.connectTimeout = 3000
                connection.readTimeout = 5000

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
                    "<p>Nessun programma rimasto per $todayName</p>"
                }

                val cssLinks = doc.select("link[rel=stylesheet]").joinToString("\n") {
                    """<link rel="stylesheet" href="${it.absUrl("href")}">"""
                }

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
                    if (dialog.isShowing) {
                        webView.loadDataWithBaseURL(url, fullHtml, "text/html", "UTF-8", null)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    if (dialog.isShowing) {
                        showErrorContent()
                    }
                }
            }
        }.start()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showProgramsModal() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            setPadding(0, 0, 0, 0)
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#00FF88"))
            setPadding(24, 16, 16, 16)
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleText = TextView(this).apply {
            text = "Programmi"
            textSize = 18f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeButton = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            layoutParams = LinearLayout.LayoutParams(48, 48)
            setPadding(12, 12, 12, 12)
            background = createRippleDrawable()
            setOnClickListener { dialog.dismiss() }
        }

        headerLayout.addView(titleText)
        headerLayout.addView(closeButton)

        val progressLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 32)
            setBackgroundColor(Color.parseColor("#1a1a1a"))
        }

        val progressBar = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48)
            indeterminateDrawable.setColorFilter(Color.parseColor("#00FF88"), android.graphics.PorterDuff.Mode.SRC_IN)
        }

        val loadingText = TextView(this).apply {
            text = "Caricamento in corso..."
            textSize = 16f
            setTextColor(Color.parseColor("#00FF88"))
            setPadding(24, 0, 0, 0)
        }

        progressLayout.addView(progressBar)
        progressLayout.addView(loadingText)

        val webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
                displayZoomControls = false
                setSupportZoom(false)
            }
        }

        fun showErrorContent() {
            val errorHtml = """
            <html>
            <body style='padding:40px; font-family:sans-serif; text-align:center; background:#f8f9fa;'>
                <div style='background:white; padding:32px; border-radius:12px; box-shadow:0 4px 12px rgba(0,0,0,0.1);'>
                    <h3 style='color:#dc3545; margin-bottom:16px;'>⚠️ Errore di connessione</h3>
                    <p style='color:#6c757d; margin-bottom:24px;'>Impossibile caricare i programmi. Controlla la connessione internet e riprova.</p>
                    <button onclick='window.location.reload()' style='background:#00FF88; color:white; border:none; padding:12px 24px; border-radius:8px; font-size:14px; cursor:pointer;'>
                        Riprova
                    </button>
                </div>
            </body>
            </html>
        """.trimIndent()

            webView.loadData(errorHtml, "text/html", "UTF-8")
        }

        webView.webViewClient = createWebViewClient("https://www.radioteateonair.it/programmi/", progressLayout)

        mainLayout.addView(headerLayout)
        mainLayout.addView(progressLayout)
        mainLayout.addView(webView)

        dialog.setContentView(mainLayout)

        dialog.window?.let { window ->
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.80).toInt()
            )
            window.setGravity(Gravity.CENTER)
            window.setBackgroundDrawableResource(android.R.color.transparent)

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f * resources.displayMetrics.density
                setColor(Color.parseColor("#1a1a1a"))
            }
            window.setBackgroundDrawable(drawable)
        }

        dialog.show()

        // Parse the programs page with Jsoup
        Thread {
            try {
                val connection = java.net.URL("https://www.radioteateonair.it/programmi/").openConnection()
                connection.connectTimeout = 3000
                connection.readTimeout = 5000

                val doc = Jsoup.parse(connection.getInputStream(), "UTF-8", "https://www.radioteateonair.it/programmi/")

                // Remove any element containing "PROGRAMMI" text
                doc.select("*").forEach { element ->
                    if (element.ownText().equals("PROGRAMMI", ignoreCase = true)) {
                        element.remove()
                    }
                }

                // Style "podcast attivi" and "podcast archiviati" elements
                doc.select("*").forEach { element ->
                    val text = element.ownText().lowercase()
                    if (text.contains("podcast attivi") || text.contains("podcast archiviati")) {
                        element.attr("style", "${element.attr("style")}; color: black !important; font-variant: small-caps !important; font-weight: bold !important;")
                    }
                }

                // Try multiple selectors to find program elements
                val programs = doc.select("article, .program-item, .post, .entry, .content-item, .program, .show").ifEmpty {
                    // If no specific program elements found, try broader selectors
                    doc.select("div[class*='program'], div[class*='show'], div[class*='post']").ifEmpty {
                        // Last resort: get all divs with headings
                        doc.select("div:has(h1), div:has(h2), div:has(h3)")
                    }
                }

                val finalContent = if (programs.isNotEmpty()) {
                    // Process and clean up the program elements
                    val processedPrograms = programs.map { program ->
                        // Remove unwanted elements like navigation, sidebar, etc.
                        program.select("nav, .nav, .navigation, .sidebar, .widget, script, style").remove()

                        // Find and enhance images
                        program.select("img").forEach { img ->
                            val src = img.attr("src")
                            if (src.isNotEmpty()) {
                                if (src.startsWith("/")) {
                                    img.attr("src", "https://www.radioteateonair.it$src")
                                }
                                img.attr("style", "max-width: 100%; height: auto; border-radius: 8px; margin: 8px 0;")
                            }
                        }

                        // Add styling to program containers
                        program.attr("style", "margin-bottom: 24px; padding: 16px; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); background: white;")

                        program.outerHtml()
                    }.joinToString("\n")

                    processedPrograms
                } else {
                    // Fallback: get the main content
                    val mainContent = doc.select("main, .main, .content, .site-content, #content, .entry-content").first()

                    mainContent?.let { content ->
                        // Clean up the content
                        content.select("nav, .nav, .navigation, .sidebar, .widget, script, style, header, footer").remove()

                        // Fix image URLs
                        content.select("img").forEach { img ->
                            val src = img.attr("src")
                            if (src.startsWith("/")) {
                                img.attr("src", "https://www.radioteateonair.it$src")
                            }
                        }

                        content.html()
                    } ?: "<div style='text-align: center; padding: 40px;'><p>Nessun programma trovato. Visita il sito web per maggiori informazioni.</p></div>"
                }

                // Get CSS links for styling
                val cssLinks = doc.select("link[rel=stylesheet]").take(5).joinToString("\n") {
                    """<link rel="stylesheet" href="${it.absUrl("href")}">"""
                }

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
                        /* Special styling for podcast section headers */
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

                runOnUiThread {
                    if (dialog.isShowing) {
                        webView.loadDataWithBaseURL("https://www.radioteateonair.it/programmi/", fullHtml, "text/html", "UTF-8", null)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    if (dialog.isShowing) {
                        showErrorContent()
                    }
                }
            }
        }.start()
    }

    private fun showAboutUsModal() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            setPadding(0, 0, 0, 0)
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#00FF88"))
            setPadding(24, 16, 16, 16)
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleText = TextView(this).apply {
            text = "Chi Siamo"
            textSize = 18f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeButton = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            layoutParams = LinearLayout.LayoutParams(48, 48)
            setPadding(12, 12, 12, 12)
            background = createRippleDrawable()
            setOnClickListener { dialog.dismiss() }
        }

        headerLayout.addView(titleText)
        headerLayout.addView(closeButton)

        // Scrollable content layout
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        // Hardcoded "Chi Siamo" text content
        val aboutUsText = getString(R.string.about_us_content)

        val textView = TextView(this).apply {
            text = aboutUsText
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setLineSpacing(8f, 1.0f)
            setPadding(0, 0, 0, 20)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+
                justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26–28
                justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
            }
        }




        contentLayout.addView(textView)
        scrollView.addView(contentLayout)
        mainLayout.addView(headerLayout)
        mainLayout.addView(scrollView)

        dialog.setContentView(mainLayout)

        dialog.window?.let { window ->
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.80).toInt()
            )
            window.setGravity(Gravity.CENTER)
            window.setBackgroundDrawableResource(android.R.color.transparent)

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f * resources.displayMetrics.density
                setColor(Color.parseColor("#1a1a1a"))
            }
            window.setBackgroundDrawable(drawable)
        }

        dialog.show()
    }

    private fun createRippleDrawable(): Drawable {
        val rippleColor = Color.parseColor("#33000000")
        val content = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RippleDrawable(ColorStateList.valueOf(rippleColor), content, null)
        } else {
            content
        }
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


    private fun createWebViewClient(allowedBaseUrl: String, progressLayout: LinearLayout): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressLayout.visibility = View.VISIBLE
                view?.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                handler.postDelayed({
                    progressLayout.visibility = View.GONE
                    view?.visibility = View.VISIBLE
                }, 300)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                // Allow the initial allowed URL and its fragments/anchors
                if (url.startsWith(allowedBaseUrl)) {
                    return false // Let WebView handle it
                }

                // For any other URL, open in external browser
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    return true // Prevent WebView from loading it
                } catch (e: Exception) {
                    showToast("Link non supportato")
                    return true
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                showErrorContent(view)
            }

            private fun showErrorContent(webView: WebView?) {
                val errorHtml = """
                <html>
                <body style='padding:40px; font-family:sans-serif; text-align:center; background:#f8f9fa;'>
                    <div style='background:white; padding:32px; border-radius:12px; box-shadow:0 4px 12px rgba(0,0,0,0.1);'>
                        <h3 style='color:#dc3545; margin-bottom:16px;'>⚠️ Errore di connessione</h3>
                        <p style='color:#6c757d; margin-bottom:24px;'>Impossibile caricare il contenuto. Controlla la connessione internet e riprova.</p>
                        <button onclick='window.location.reload()' style='background:#00FF88; color:white; border:none; padding:12px 24px; border-radius:8px; font-size:14px; cursor:pointer;'>
                            Riprova
                        </button>
                    </div>
                </body>
                </html>
            """.trimIndent()

                webView?.loadData(errorHtml, "text/html", "UTF-8")
            }
        }
    }
}