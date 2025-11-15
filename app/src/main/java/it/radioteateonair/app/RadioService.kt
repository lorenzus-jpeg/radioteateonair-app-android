package it.teateonair.app

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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Improved RadioService with:
 * - Audio stream prefetching for instant playback
 * - Debouncing to prevent race conditions
 * - Better state management
 * - UTF-8 encoding fix for accented characters in metadata
 */
class RadioService : Service() {

    companion object {
        const val ACTION_PLAY = "it.teateonair.app.ACTION_PLAY"
        const val ACTION_PAUSE = "it.teateonair.app.ACTION_PAUSE"
        const val ACTION_STOP = "it.teateonair.app.ACTION_STOP"
        const val ACTION_CLOSE = "it.teateonair.app.ACTION_CLOSE"
        const val ACTION_PREFETCH = "it.teateonair.app.ACTION_PREFETCH"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "radio_channel"
        private const val STREAM_URL = "https://nr14.newradio.it:8663/radioteateonair"
        private const val DEBOUNCE_DELAY_MS = 300L // Prevent rapid clicks
    }

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var notificationManager: NotificationManager
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var metadataUpdateRunnable: Runnable? = null

    private var currentArtist = "Radio Teate OnAir"
    private var currentTitle = "In ascolto..."
    private var isPlaying = false
    private var isServiceDestroyed = false
    private val isPreparing = AtomicBoolean(false)
    private val isPrepared = AtomicBoolean(false)

    // Debouncing
    private var lastActionTime = 0L
    private val actionLock = Any()

    override fun onCreate() {
        super.onCreate()
        setupNotificationManager()
        // Prefetch audio immediately when service is created
        prefetchAudioStream()
    }

    /**
     * Fixes UTF-8 encoding issues where accented characters are garbled.
     * This reverses the mojibake effect caused by UTF-8 being misinterpreted as Latin-1.
     */
    private fun fixUtf8Encoding(garbledText: String): String {
        return try {
            // Try to reverse the common UTF-8 → Latin-1 encoding error
            garbledText.toByteArray(Charsets.ISO_8859_1).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            // If conversion fails, return original text
            garbledText
        }
    }

    /**
     * Prefetches the audio stream so it's ready for instant playback
     */
    private fun prefetchAudioStream() {
        if (isPreparing.get() || isPrepared.get()) {
            return // Already preparing or prepared
        }

        isPreparing.set(true)

        executor.execute {
            try {
                // Release old player if exists
                mediaPlayer?.let {
                    try {
                        it.stop()
                        it.reset()
                        it.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Create and prepare new player
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )

                    setOnPreparedListener {
                        isPreparing.set(false)
                        isPrepared.set(true)
                        // Don't start playing automatically - just ready to play
                    }

                    setOnErrorListener { mp, what, extra ->
                        isPreparing.set(false)
                        isPrepared.set(false)
                        // Retry prefetch after error
                        handler.postDelayed({ prefetchAudioStream() }, 2000)
                        true
                    }

                    setOnCompletionListener {
                        // If stream completes (shouldn't happen for radio), reprepare
                        isPrepared.set(false)
                        prefetchAudioStream()
                    }

                    try {
                        setDataSource(STREAM_URL)
                        prepareAsync() // Prepare in background
                    } catch (e: Exception) {
                        e.printStackTrace()
                        isPreparing.set(false)
                        isPrepared.set(false)
                        // Retry after error
                        handler.postDelayed({ prefetchAudioStream() }, 2000)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isPreparing.set(false)
                isPrepared.set(false)
            }
        }
    }

    private fun startServiceProperly() {
        if (!isServiceDestroyed) {
            startMetadataUpdater()
            try {
                startForeground(NOTIFICATION_ID, createNotification())
            } catch (e: Exception) {
                e.printStackTrace()
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
                NotificationManager.IMPORTANCE_LOW
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

        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainActivityPendingIntent = PendingIntent.getActivity(
            this, 0, mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPlaying) currentTitle else "Radio Teate OnAir")
            .setContentText(if (isPlaying) currentArtist else "Tocca per avviare")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(mainActivityPendingIntent)
            .setDeleteIntent(createPendingIntent(ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setShowWhen(false)
            .addAction(playPauseAction)
            .setStyle(MediaNotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0)
                .setMediaSession(null)
            )
            .setOngoing(isPlaying)
            .setAutoCancel(false)
            .setColor(0xFF008000.toInt())
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
                if (isServiceDestroyed || executor.isShutdown) {
                    return
                }

                executor.execute {
                    if (isServiceDestroyed || executor.isShutdown) {
                        return@execute
                    }

                    var connection: java.net.HttpURLConnection? = null
                    try {
                        val url = URL(jsonUrl)
                        connection = url.openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.requestMethod = "GET"

                        val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val json = JSONObject(response)
                        val fullTitle = json
                            .getJSONObject("icestats")
                            .getJSONObject("source")
                            .getString("yp_currently_playing")

                        // Fix UTF-8 encoding issues with accents and special characters
                        val fixedTitle = fixUtf8Encoding(fullTitle)

                        val parts = fixedTitle.split(" - ", limit = 2)
                        val artist = parts.getOrNull(0)?.trim() ?: ""
                        val song = parts.getOrNull(1)?.trim() ?: "In caricamento..."

                        handler.post {
                            if (!isServiceDestroyed && (currentArtist != artist || currentTitle != song)) {
                                currentArtist = artist
                                currentTitle = song
                                updateNotification()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            connection?.disconnect()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        if (!isServiceDestroyed && !executor.isShutdown) {
                            handler.postDelayed(this, 3000)
                        }
                    }
                }
            }
        }

        metadataUpdateRunnable?.let { handler.post(it) }
    }

    private fun updateNotification() {
        if (::notificationManager.isInitialized && !isServiceDestroyed) {
            try {
                notificationManager.notify(NOTIFICATION_ID, createNotification())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isServiceDestroyed) return START_NOT_STICKY

        // Debounce rapid actions (except PREFETCH)
        if (intent?.action != ACTION_PREFETCH) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastActionTime < DEBOUNCE_DELAY_MS) {
                // Too soon, ignore this action
                return START_STICKY
            }
            lastActionTime = currentTime
        }

        when (intent?.action) {
            ACTION_PREFETCH -> {
                // Just prefetch, don't start foreground service or show notification
                prefetchAudioStream()
            }
            ACTION_PLAY -> {
                // Start foreground service before playing
                startServiceProperly()
                playStream()
            }
            ACTION_PAUSE -> {
                startServiceProperly()
                stopStreamButKeepNotification()
            }
            ACTION_STOP -> {
                startServiceProperly()
                stopStreamButKeepNotification()
            }
            ACTION_CLOSE -> {
                closeNotificationAndService()
            }
            else -> {
                // If service is started without action, just prefetch
                prefetchAudioStream()
            }
        }

        return START_STICKY
    }

    private fun playStream() {
        synchronized(actionLock) {
            if (isPlaying) {
                return // Already playing
            }

            val player = mediaPlayer
            if (player == null) {
                // No player, create and prepare
                prefetchAudioStream()
                // Will need to wait for prepare to complete
                handler.postDelayed({ playStream() }, 500)
                return
            }

            try {
                when {
                    isPrepared.get() -> {
                        // Stream is ready, play immediately
                        if (!player.isPlaying) {
                            player.start()
                            isPlaying = true

                            // Send broadcast
                            val broadcastIntent = Intent(MainActivity.ACTION_AUDIO_STARTED)
                            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)

                            updateNotification()
                        }
                        Unit
                    }
                    isPreparing.get() -> {
                        // Still preparing, wait and retry
                        handler.postDelayed({ playStream() }, 200)
                    }
                    else -> {
                        // Not prepared, start prefetch
                        prefetchAudioStream()
                        handler.postDelayed({ playStream() }, 500)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isPlaying = false
                isPrepared.set(false)
                // Try to recover
                prefetchAudioStream()
            }
        }
    }

    private fun stopStreamButKeepNotification() {
        synchronized(actionLock) {
            if (!isPlaying) {
                return // Already stopped
            }

            isPlaying = false

            try {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                        // Keep stream prepared for quick restart
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Send broadcast
            val broadcastIntent = Intent(MainActivity.ACTION_AUDIO_STOPPED)
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)

            updateNotification()
        }
    }

    private fun closeNotificationAndService() {
        synchronized(actionLock) {
            isPlaying = false
            isPrepared.set(false)
            isPreparing.set(false)

            // Stop metadata updates
            metadataUpdateRunnable?.let { handler.removeCallbacks(it) }

            try {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.reset()
                    it.release()
                }
                mediaPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Send broadcast
            val broadcastIntent = Intent(MainActivity.ACTION_AUDIO_STOPPED)
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)

            // Stop the service completely
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        isServiceDestroyed = true

        // Remove all pending callbacks FIRST - this prevents new tasks from being queued
        metadataUpdateRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Shutdown executor and wait for pending tasks with timeout
        try {
            executor.shutdown()
            if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}