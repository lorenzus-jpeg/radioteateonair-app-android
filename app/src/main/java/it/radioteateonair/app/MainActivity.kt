package it.teateonair.app

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
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.WindowCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.Executors
import android.graphics.Color
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

    internal var isPlaying = false
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

        CacheManager.prefetchAll()

        prefetchAudioStream()
    }

    private fun prefetchAudioStream() {
        val intent = Intent(this, RadioService::class.java).apply {
            action = RadioService.ACTION_PREFETCH
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
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
        WindowCompat.setDecorFitsSystemWindows(window, false)

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        //    window.statusBarColor = Color.TRANSPARENT
        //window.navigationBarColor = Color.TRANSPARENT
        //}

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container)) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            topInsetSpacer.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                height = insets.top
            }

            bottomInsetSpacer.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                height = insets.bottom
            }

            WindowInsetsCompat.CONSUMED
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

        findViewById<View>(R.id.socialSpotify).setOnClickListener {
            val url = "https://open.spotify.com/user/bdubob5m8sthl8504ab0xx88y"
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
                startRadioService()
            } else {
                showToast("Permesso notifiche necessario per il player")
            }
        }
    }

    private fun animateToPlayingState() {
        hideStartButton()
        showBottomBar()
    }

    private fun animateToStoppedState() {
        hideBottomBar()
        showStartButton()
    }

    private fun hideStartButton() {
        val screenWidth = resources.displayMetrics.widthPixels
        val targetTranslationX = -screenWidth / 2f - getResponsiveValue(100f)

        startButton.animate()
            .alpha(0f)
            .translationX(targetTranslationX)
            .scaleX(getResponsiveScaleValue(0.5f))
            .scaleY(getResponsiveScaleValue(0.5f))
            .setDuration(400)
            .withEndAction {
                startButton.visibility = View.GONE
            }
            .start()
    }

    private fun showBottomBar() {
        val screenWidth = resources.displayMetrics.widthPixels
        val initialTranslationX = screenWidth / 2f + getResponsiveValue(100f)

        bottomBar.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationX = initialTranslationX
            scaleX = getResponsiveScaleValue(0.5f)
            scaleY = getResponsiveScaleValue(0.5f)
            animate()
                .alpha(1f)
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .start()
        }

        songTitle.isSelected = true
    }

    private fun hideBottomBar() {
        val screenWidth = resources.displayMetrics.widthPixels
        val targetTranslationX = screenWidth / 2f + getResponsiveValue(100f)

        bottomBar.animate()
            .alpha(0f)
            .translationX(targetTranslationX)
            .scaleX(getResponsiveScaleValue(0.5f))
            .scaleY(getResponsiveScaleValue(0.5f))
            .setDuration(400)
            .withEndAction {
                bottomBar.visibility = View.GONE
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
                        val artist = if (parts.isNotEmpty()) parts[0] else "Teate On Air"
                        val song = if (parts.size > 1) parts[1] else "In onda"

                        runOnUiThread {
                            if (isPlaying) {
                                songTitle.text = song
                                artistName.text = artist
                                adjustTextSizes()
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (isPlaying) {
                    handler.postDelayed(this, 5000)
                }
            }
        }

        handler.post(updateTask)
    }

    private fun adjustTextSizes() {
        if (bottomBar.width <= 0) return

        val maxWidth = (bottomBar.width * 0.65f).toInt()

        songTitle.post {
            var currentSize = 20f
            songTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, currentSize)

            while (songTitle.paint.measureText(songTitle.text.toString()) > maxWidth && currentSize > 10f) {
                currentSize -= 0.5f
                songTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, currentSize)
            }

            songTitle.ellipsize = TextUtils.TruncateAt.END
            songTitle.maxLines = 1
        }

        artistName.post {
            var currentSize = 16f
            artistName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, currentSize)

            while (artistName.paint.measureText(artistName.text.toString()) > maxWidth && currentSize > 8f) {
                currentSize -= 0.5f
                artistName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, currentSize)
            }

            artistName.ellipsize = TextUtils.TruncateAt.END
            artistName.maxLines = 1
        }
    }

    private fun showImprovedPalinsestoModal(url: String) {
        ScheduleModal(this).show(url)
    }

    private fun showProgramsModal() {
        ProgramsModal(this).show()
    }

    private fun showAboutUsModal() {
        WhoWeAre(this).show()
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
                }, 100)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                if (url.startsWith(allowedBaseUrl)) {
                    return false
                }

                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    return true
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