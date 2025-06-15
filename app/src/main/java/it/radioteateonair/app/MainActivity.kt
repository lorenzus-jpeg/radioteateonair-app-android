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

class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var playButton: Button
    private lateinit var bottomBar: LinearLayout
    private lateinit var songTitle: TextView
    private lateinit var artistName: TextView
    private lateinit var bars: List<BarView>
    private lateinit var handler: Handler
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        playButton = findViewById(R.id.playButton)
        bottomBar = findViewById(R.id.bottomBar)
        songTitle = findViewById(R.id.songTitle)
        artistName = findViewById(R.id.artistName)

        bars = listOf(
            findViewById(R.id.bar1), findViewById(R.id.bar2), findViewById(R.id.bar3),
            findViewById(R.id.bar4), findViewById(R.id.bar5), findViewById(R.id.bar6),
            findViewById(R.id.bar7), findViewById(R.id.bar8), findViewById(R.id.bar9),
            findViewById(R.id.bar10), findViewById(R.id.bar11), findViewById(R.id.bar12),
            findViewById(R.id.bar13), findViewById(R.id.bar14), findViewById(R.id.bar15)
        )

        handler = Handler(Looper.getMainLooper())

        startButton.setOnClickListener {
            startService(Intent(this, RadioService::class.java))
            bars.forEach { it.startAnimation() }
            isPlaying = true

            startButton.animate()
                .translationX(-resources.displayMetrics.widthPixels / 2f + 96f)
                .scaleX(0.67f)
                .scaleY(0.67f)
                .setDuration(400)
                .withEndAction {
                    startButton.visibility = View.GONE
                    bottomBar.visibility = View.VISIBLE
                }
                .start()

            startSongInfoUpdater()
        }

        playButton.setOnClickListener {
            stopService(Intent(this, RadioService::class.java))
            bars.forEach { it.stopAnimation() }
            isPlaying = false

            bottomBar.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    bottomBar.visibility = View.GONE
                    startButton.apply {
                        visibility = View.VISIBLE
                        alpha = 0f
                        translationX = -resources.displayMetrics.widthPixels / 2f + 96f
                        scaleX = 0.67f
                        scaleY = 0.67f
                        animate()
                            .alpha(1f)
                            .translationX(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(400)
                            .start()
                    }
                }
                .start()
        }

        findViewById<View>(R.id.box1).setOnClickListener {
            showPopupWithWebView("https://radioteateonair.it/palinsesto")
        }

        findViewById<View>(R.id.box2).setOnClickListener { showToast("Box 2 clicked") }
        findViewById<View>(R.id.box3).setOnClickListener { showToast("Box 3 clicked") }
        findViewById<View>(R.id.box4).setOnClickListener { showToast("Box 4 clicked") }
        findViewById<View>(R.id.box5).setOnClickListener { showToast("Box 5 clicked") }
        findViewById<View>(R.id.box6).setOnClickListener { showToast("Box 6 clicked") }
    }

    private fun startSongInfoUpdater() {
        handler.post(object : Runnable {
            override fun run() {
                songTitle.text = "Titolo Esempio"
                artistName.text = "Artista Esempio"
                if (isPlaying) {
                    handler.postDelayed(this, 5000)
                }
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showPopupWithWebView(url: String) {
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.webViewClient = WebViewClient()

        AlertDialog.Builder(this)
            .setView(webView)
            .setNegativeButton("Chiudi") { dialog, _ -> dialog.dismiss() }
            .show()

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
                    "<p>Nessun programma disponibile per $todayName</p>"
                }

                val cssLinks = doc.select("link[rel=stylesheet]").joinToString("\n") {
                    """<link rel="stylesheet" href="${it.absUrl("href")}">"""
                }

                val fullHtml = """
                    <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            $cssLinks
                            <style>
                                body { margin: 0; padding: 16px; font-family: sans-serif; background: #fff; color: #000; }
                                img, iframe { max-width: 100%; height: auto; display: block; margin: 8px auto; }
                                .qt-header-bg {
                                    min-height: 180px;
                                    background-size: cover !important;
                                    background-position: center !important;
                                    background-repeat: no-repeat !important;
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
            }
        }.start()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
