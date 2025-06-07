package it.radioteateonair.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URL
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var artistView: TextView
    private lateinit var songTitleView: TextView
    private lateinit var bars: List<BarView>
    private lateinit var playButton: AppCompatImageButton
    private lateinit var startButton: AppCompatButton
    private lateinit var bottomBar: View
    private lateinit var logoText: TextView

    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        playButton = findViewById(R.id.playButton)
        bottomBar = findViewById(R.id.bottomBar)
        artistView = findViewById(R.id.artistName)
        songTitleView = findViewById(R.id.songTitle)
        logoText = findViewById(R.id.logoText)

        bars = listOf(
            findViewById(R.id.bar1), findViewById(R.id.bar2), findViewById(R.id.bar3),
            findViewById(R.id.bar4), findViewById(R.id.bar5), findViewById(R.id.bar6),
            findViewById(R.id.bar7), findViewById(R.id.bar8), findViewById(R.id.bar9),
            findViewById(R.id.bar10), findViewById(R.id.bar11), findViewById(R.id.bar12),
            findViewById(R.id.bar13), findViewById(R.id.bar14), findViewById(R.id.bar15)
        )

        bottomBar.visibility = View.GONE

        // Marquee strings
        val messages = listOf(
            "Benvenuti nell'app ufficiale di Radio Teate On Air",
            "Ascolta la diretta!",
            "Visita il sito radioteateonair.it",
            "Seguici su Instagram @radioteateonair",
            "Ascolta Soundcheck, la nostra nuova rubrica!"
        )
        val repeated = messages.joinToString("     •     ") + "     •     "
        val fullMarquee = repeated.repeat(10)
        logoText.text = fullMarquee
        logoText.setSingleLine(true)
        logoText.ellipsize = TextUtils.TruncateAt.MARQUEE
        logoText.marqueeRepeatLimit = -1
        logoText.setHorizontallyScrolling(true)
        logoText.isFocusable = true
        logoText.isFocusableInTouchMode = true
        logoText.requestFocus()
        logoText.isSelected = true

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
            if (isPlaying) {
                stopService(Intent(this, RadioService::class.java))
                bars.forEach { it.stopAnimation() }
                isPlaying = false

                startButton.apply {
                    visibility = View.VISIBLE
                    alpha = 0f
                    scaleX = 0.67f
                    scaleY = 0.67f
                    translationX = -resources.displayMetrics.widthPixels / 2f + 96f
                }

                bottomBar.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        bottomBar.visibility = View.GONE
                        bottomBar.alpha = 1f
                        startButton.animate()
                            .translationX(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(400)
                            .start()
                    }
                    .start()
            }
        }

        loadFilteredWebPage()
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
                        val artist = parts.getOrNull(0)?.trim() ?: "Unknown Artist"
                        val song = parts.getOrNull(1)?.trim() ?: "Unknown Title"

                        handler.post {
                            artistView.text = artist
                            songTitleView.text = song
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        handler.post {
                            artistView.text = "Loading..."
                            songTitleView.text = ""
                        }
                    } finally {
                        handler.postDelayed(this, 15000)
                    }
                }
            }
        }

        handler.post(updateTask)
    }

    private fun loadFilteredWebPage() {
        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true

        val url = "https://www.radioteateonair.it/palinsesto/"

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

                ///// Ensure background images are applied

                filteredDivs.forEach { div ->
                    div.select(".qt-header-bg").forEach { bgDiv ->
                        val bgUrl = bgDiv.attr("data-bgimage")
                        if (bgUrl.isNotEmpty()) {
                            bgDiv.attr("style", "background-image:url('$bgUrl'); background-size:cover; background-position:center; background-repeat:no-repeat;")
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
}
