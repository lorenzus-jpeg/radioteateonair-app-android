package it.radioteateonair.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var isPlaying = false
    private lateinit var bars: List<BarView>

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
    }

    private fun startSongInfoUpdater() {
        val url = "https://nr14.newradio.it:8663/status-json.xsl"

        val updateTask = object : Runnable {
            override fun run() {
                executor.execute {
                    try {
                        val response = URL(url).readText()
                        val json = JSONObject(response)
                        val fullTitle = json
                            .getJSONObject("icestats")
                            .getJSONObject("source")
                            .getString("title")

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
                        handler.postDelayed(this, 20000) // refresh every 20 sec
                    }
                }
            }
        }

        handler.post(updateTask)
    }
}
