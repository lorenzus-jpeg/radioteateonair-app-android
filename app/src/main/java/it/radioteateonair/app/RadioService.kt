package it.radioteateonair.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.Executors

class RadioService : Service() {

    companion object {
        const val ACTION_PLAY = "it.radioteateonair.app.ACTION_PLAY"
        const val ACTION_PAUSE = "it.radioteateonair.app.ACTION_PAUSE"
        const val ACTION_STOP = "it.radioteateonair.app.ACTION_STOP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "radio_channel"
    }

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var notificationManager: NotificationManager
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var currentArtist = "Radio Teate OnAir"
    private var currentTitle = "In ascolto..."
    private var isPlaying = false

    override fun onCreate() {
        super.onCreate()

        setupMediaPlayer()
        setupNotificationManager()
        startMetadataUpdater()

        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource("https://nr14.newradio.it:8663/radioteateonair")
                setOnPreparedListener {
                    start()
                    this@RadioService.isPlaying = true
                    updateNotification()
                }
                setOnErrorListener { _, _, _ ->
                    this@RadioService.isPlaying = false
                    updateNotification()
                    false
                }
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
                this@RadioService.isPlaying = false
            }
        }
    }

    private fun setupNotificationManager() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Radio Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controlli per Radio Teate OnAir"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pausa",
                createPendingIntent(ACTION_PAUSE)
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                "Play",
                createPendingIntent(ACTION_PLAY)
            ).build()
        }

        val stopAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            createPendingIntent(ACTION_STOP)
        ).build()

        // Create intent to open main activity when notification is tapped
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainActivityPendingIntent = PendingIntent.getActivity(
            this, 0, mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSmallIcon(R.drawable.ic_radio_notification)
            .setLargeIcon(createLargeIcon())
            .setContentIntent(mainActivityPendingIntent)
            .setDeleteIntent(createPendingIntent(ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .build()
    }

    private fun createLargeIcon(): Bitmap? {
        return try {
            BitmapFactory.decodeResource(resources, R.drawable.logo_toa)?.let { bitmap ->
                Bitmap.createScaledBitmap(bitmap, 256, 256, false)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, RadioService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startMetadataUpdater() {
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
                        val artist = parts.getOrNull(0)?.trim() ?: "Artista Sconosciuto"
                        val song = parts.getOrNull(1)?.trim() ?: "Titolo Sconosciuto"

                        handler.post {
                            if (currentArtist != artist || currentTitle != song) {
                                currentArtist = artist
                                currentTitle = song
                                updateNotification()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        handler.postDelayed(this, 5000)
                    }
                }
            }
        }

        handler.post(updateTask)
    }

    private fun updateNotification() {
        if (::notificationManager.isInitialized) {
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> playStream()
            ACTION_PAUSE -> pauseStream()
            ACTION_STOP -> stopStream()
        }

        return START_STICKY
    }

    private fun playStream() {
        if (!isPlaying) {
            try {
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource("https://nr14.newradio.it:8663/radioteateonair")
                    mediaPlayer.prepareAsync()
                }
                isPlaying = true
                updateNotification()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun pauseStream() {
        if (isPlaying) {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isPlaying = false
            updateNotification()
        }
    }

    private fun stopStream() {
        isPlaying = false
        mediaPlayer.stop()
        updateNotification()
        stopSelf()
    }

    override fun onDestroy() {
        mediaPlayer.stop()
        mediaPlayer.release()
        executor.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}