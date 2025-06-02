package it.radioteateonair.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var artistView: TextView
    private lateinit var songTitleView: TextView
    private lateinit var bars: List<BarView>
    private var isPlaying = false

    val fallbackImages = mapOf(
        "METEO ON AIR" to "https://www.radioteateonair.it/wp-content/uploads/2023/07/copertina_meteo-400x400.png",
        "PILLOLE DI ECONOMIA" to "https://www.radioteateonair.it/wp-content/uploads/2023/07/copertina_meteo-400x400.png"
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val playButton = findViewById<Button>(R.id.playButton)
        artistView = findViewById(R.id.artistName)
        songTitleView = findViewById(R.id.songTitle)

        bars = listOf(
            findViewById(R.id.bar1), findViewById(R.id.bar2), findViewById(R.id.bar3),
            findViewById(R.id.bar4), findViewById(R.id.bar5), findViewById(R.id.bar6),
            findViewById(R.id.bar7), findViewById(R.id.bar8), findViewById(R.id.bar9),
            findViewById(R.id.bar10), findViewById(R.id.bar11), findViewById(R.id.bar12),
            findViewById(R.id.bar13), findViewById(R.id.bar14), findViewById(R.id.bar15)
        )

        playButton.setOnClickListener {
            if (!isPlaying) {
                startService(Intent(this, RadioService::class.java))
                bars.forEach { it.startAnimation() }
                playButton.text = "Stop Radio"
            } else {
                stopService(Intent(this, RadioService::class.java))
                bars.forEach { it.stopAnimation() }
                playButton.text = "Play Radio"
            }
            isPlaying = !isPlaying
        }

        startSongInfoUpdater()
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
                val doc = org.jsoup.Jsoup.connect(url).get()

                doc.select("select#qwShowDropdown").remove()
                doc.select("*:matchesOwn(^\\s*Scegli giorno\\s*\$)").forEach { it.remove() }

                val element = doc.selectFirst("div.qt-show-schedule.qt-shows-schedule-refresh")

                element?.select("img")?.forEach { img ->
                    val realSrc = img.absUrl("src").ifEmpty { img.absUrl("data-src") }
                    img.attr("src", realSrc)
                    img.removeAttr("data-src")
                }

                val fallbackImages = mapOf(
                    "METEO ON AIR" to "https://yourdomain.it/fallbacks/meteo.png",
                    "PILLOLE DI ECONOMIA" to "https://yourdomain.it/fallbacks/economia.png"
                )

                element?.select(".qt-header-bg")?.forEach { div ->
                    val bgUrl = div.absUrl("data-bgimage")
                    var finalUrl = bgUrl

                    if (bgUrl.isEmpty()) {
                        // Try to find fallback from adjacent .qt-ellipsis a
                        val parent = div.parent()
                        val fallbackKey = parent?.selectFirst(".qt-ellipsis a")?.text()?.trim()?.uppercase()
                        if (fallbackKey != null && fallbackImages.containsKey(fallbackKey)) {
                            finalUrl = fallbackImages[fallbackKey] ?: ""
                        }
                    }

                    if (finalUrl.isNotEmpty()) {
                        div.attr(
                            "style", "background-image: url('$finalUrl'); " +
                                    "background-size: cover; background-position: center; " +
                                    "background-repeat: no-repeat; min-height: 180px;"
                        )
                    }
                }

                val content = element?.html() ?: "<p>Contenuto non disponibile</p>"

                val cssLinks = doc.select("link[rel=stylesheet]").joinToString("\n") {
                    """<link rel="stylesheet" href="${it.absUrl("href")}">"""
                }

                val cleanHtml = """
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
                        $content
                    </body>
                </html>
            """.trimIndent()

                runOnUiThread {
                    webView.loadDataWithBaseURL(url, cleanHtml, "text/html", "UTF-8", null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }


}
