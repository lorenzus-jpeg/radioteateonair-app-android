package it.radioteateonair.app

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
import android.view.ViewTreeObserver
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

class MainActivity : AppCompatActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        const val ACTION_AUDIO_STOPPED = "it.radioteateonair.app.AUDIO_STOPPED"
        const val ACTION_AUDIO_STARTED = "it.radioteateonair.app.AUDIO_STARTED"
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

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
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
            val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.radioteateonair.it/programmi/"))
            startActivity(urlIntent)
        }

        findViewById<View>(R.id.box3).setOnClickListener {
            val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.radioteateonair.it/about-us/"))
            startActivity(urlIntent)
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

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressLayout.visibility = View.VISIBLE
                webView.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                handler.postDelayed({
                    progressLayout.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                }, 300)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                showErrorContent()
            }
        }

        mainLayout.addView(headerLayout)
        mainLayout.addView(progressLayout)
        mainLayout.addView(webView)

        dialog.setContentView(mainLayout)

        dialog.window?.let { window ->
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.80).toInt() // Reduced height to leave space below
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
                // Set connection timeout for faster response
                val connection = java.net.URL(url).openConnection()
                connection.connectTimeout = 3000 // 3 seconds
                connection.readTimeout = 5000 // 5 seconds

                val doc = Jsoup.parse(connection.getInputStream(), "UTF-8", url)

                // FIXED: Use proper UTF-8 encoded Italian day names
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
                    "<p>Nessun programma rimasto per $todayName</p>"
                }

                val cssLinks = doc.select("link[rel=stylesheet]").joinToString("\n") {
                    """<link rel="stylesheet" href="${it.absUrl("href")}">"""
                }

                // RESTORED ORIGINAL HTML STRUCTURE with minor improvements
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
}