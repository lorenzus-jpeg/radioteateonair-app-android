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


class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var playButton: Button
    private lateinit var bottomBar: LinearLayout
    private lateinit var songTitle: TextView
    private lateinit var artistName: TextView
    private lateinit var handler: Handler
    private val executor = java.util.concurrent.Executors.newSingleThreadExecutor()

    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        playButton = findViewById(R.id.playButton)
        bottomBar = findViewById(R.id.bottomBar)
        songTitle = findViewById(R.id.songTitle)
        artistName = findViewById(R.id.artistName)


        handler = Handler(Looper.getMainLooper())

        startButton.setOnClickListener {
            if (!isPlaying) {
                startService(Intent(this, RadioService::class.java))
                isPlaying = true

                startButton.animate()
                    .translationX(-resources.displayMetrics.widthPixels / 2f + 96f)
                    .scaleX(0.67f)
                    .scaleY(0.67f)
                    .setDuration(400)
                    .withEndAction {
                        // Hide play button
                        startButton.visibility = View.GONE

                        // Always reset bottomBar after animation
                        bottomBar.alpha = 0f
                        bottomBar.visibility = View.VISIBLE
                        bottomBar.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                    }
                    .start()

                startSongInfoUpdater()
            }
        }

        playButton.setOnClickListener {
            stopService(Intent(this, RadioService::class.java))
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
        //findViewById<View>(R.id.box5).setOnClickListener { showToast("Box 5 clicked") }
        //findViewById<View>(R.id.box6).setOnClickListener { showToast("Box 6 clicked") }

        val box2: ImageView = findViewById(R.id.box2)
        box2.setOnClickListener {
            val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.radioteateonair.it/programmi/"))
            startActivity(urlIntent)
        }

        val box3: ImageView = findViewById(R.id.box3)
        box3.setOnClickListener {
            val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.radioteateonair.it/about-us/"))
            startActivity(urlIntent)
        }


        val box4: ImageView = findViewById(R.id.box4)

        box4.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_socials, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            dialogView.findViewById<ImageView>(R.id.instagram).setOnClickListener {
                val url = "https://www.instagram.com/yourprofile"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                dialog.dismiss()
            }

            dialogView.findViewById<ImageView>(R.id.facebook).setOnClickListener {
                val url = "https://www.instagram.com/radio_teateonair"
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
                            artistName.text = artist
                            songTitle.text = song
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        handler.post {
                            artistName.text = "Caricamento..."
                            songTitle.text = ""
                        }
                    } finally {
                        handler.postDelayed(this, 10000)
                    }
                }
            }
        }

        handler.post(updateTask)
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
