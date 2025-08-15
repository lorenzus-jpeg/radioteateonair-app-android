package it.radioteateonair.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.Executors

class RadioService : Service() {

    companion object {
        const val ACTION_PLAY = "it.radioteateonair.app.ACTION_PLAY"
        const val ACTION_PAUSE = "it.radioteateonair.app.ACTION_PAUSE"
        const val ACTION_STOP = "it.radioteateonair.app.ACTION_STOP"
        const val ACTION_CLOSE = "it.radioteateonair.app.ACTION_CLOSE"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "radio_channel"
    }

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var notificationManager: NotificationManager
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var metadataUpdateRunnable: Runnable? = null

    private var currentArtist = "Radio Teate OnAir"
    private var currentTitle = "In ascolto..."
    private var isPlaying = false
    private var isServiceDestroyed = false

    override fun onCreate() {
        super.onCreate()

        setupMediaPlayer()
        setupNotificationManager()

        // DON'T start metadata updater or foreground service automatically
        // Only start when explicitly requested via ACTION_PLAY
    }

    private fun startServiceProperly() {
        // Start metadata updater and foreground service when actually needed
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
                "Radio Teate OnAir",
                NotificationManager.IMPORTANCE_LOW // Use LOW to avoid sound but still show
            ).apply {
                description = "Controlli per la riproduzione di Radio Teate OnAir"
                setShowBadge(true)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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

        // Create intent to open main activity when notification is tapped
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainActivityPendingIntent = PendingIntent.getActivity(
            this, 0, mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create a media-style notification with MediaStyle for ongoing audio stream
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPlaying) currentTitle else "Radio Teate OnAir")
            .setContentText(if (isPlaying) currentArtist else "Tocca per avviare")
            .setSmallIcon(android.R.drawable.ic_media_play) // Use system media icon for better recognition
            // Removed setLargeIcon() to hide the image as requested
            .setContentIntent(mainActivityPendingIntent)
            .setDeleteIntent(createPendingIntent(ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW) // LOW priority but still visible
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT) // Identify as media transport
            .setShowWhen(false)
            .addAction(playPauseAction) // Only pause/play button, no stop button
            .setStyle(MediaNotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0) // Show the play/pause button in compact view
                .setMediaSession(null) // No media session for simple streaming
            )
            .setOngoing(isPlaying) // Make it persistent while playing
            .setAutoCancel(false) // Don't auto-cancel when tapped
            .setColor(0xFF008000.toInt()) // Green color for the notification accent
            .build()

        return notification
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

        metadataUpdateRunnable = object : Runnable {
            override fun run() {
                // Check if service is destroyed or executor is shut down
                if (isServiceDestroyed || executor.isShutdown) {
                    return
                }

                executor.execute {
                    // Double check inside executor
                    if (isServiceDestroyed || executor.isShutdown) {
                        return@execute
                    }

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
                            if (!isServiceDestroyed && currentArtist != artist || currentTitle != song) {
                                currentArtist = artist
                                currentTitle = song
                                updateNotification()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        // Only reschedule if service is still active
                        if (!isServiceDestroyed && !executor.isShutdown) {
                            handler.postDelayed(this, 2000) // Update every 2 seconds instead of 5
                        }
                    }
                }
            }
        }

        metadataUpdateRunnable?.let { handler.post(it) }
    }

    private fun updateNotification() {
        if (::notificationManager.isInitialized) {
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always start foreground service when any command is received
        if (!isServiceDestroyed) {
            startServiceProperly()
        }

        when (intent?.action) {
            ACTION_PLAY -> playStream()
            ACTION_PAUSE -> stopStreamButKeepNotification() // Stop audio but keep notification
            ACTION_STOP -> stopStreamButKeepNotification() // Stop audio but keep notification
            ACTION_CLOSE -> closeNotificationAndService() // Close notification and service
            else -> {
                // If service is started without action, just show the notification (don't play)
                updateNotification()
            }
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

                // SEND BROADCAST TO NOTIFY MAINACTIVITY THAT AUDIO STARTED
                val broadcastIntent = Intent(MainActivity.ACTION_AUDIO_STARTED)
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)

                updateNotification()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun pauseStream() {
        // For radio streaming, pause = stop audio but keep notification
        stopStreamButKeepNotification()
    }

    private fun stopStreamButKeepNotification() {
        val wasPlaying = isPlaying
        isPlaying = false

        // Stop metadata updates
        metadataUpdateRunnable?.let { handler.removeCallbacks(it) }

        try {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // SEND BROADCAST TO NOTIFY MAINACTIVITY THAT AUDIO WAS STOPPED
        if (wasPlaying) {
            val broadcastIntent = Intent(MainActivity.ACTION_AUDIO_STOPPED)
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
        }

        // Update notification to show play button
        updateNotification()
    }

    private fun closeNotificationAndService() {
        isPlaying = false

        // Stop metadata updates
        metadataUpdateRunnable?.let { handler.removeCallbacks(it) }

        try {
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // SEND BROADCAST TO NOTIFY MAINACTIVITY THAT AUDIO WAS STOPPED
        val broadcastIntent = Intent(MainActivity.ACTION_AUDIO_STOPPED)
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)

        // Stop the service completely and remove notification
        stopForeground(true)
        stopSelf()
    }

    private fun stopStream() {
        // This method is kept for backward compatibility but now just calls the new method
        closeNotificationAndService()
    }

    override fun onDestroy() {
        isServiceDestroyed = true

        // Remove all pending callbacks
        metadataUpdateRunnable?.let { handler.removeCallbacks(it) }

        try {
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.stop()
                mediaPlayer.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Shutdown executor last
        try {
            executor.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}