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
        private const val DEBOUNCE_DELAY_MS = 300L
        private const val KEEP_ALIVE_INTERVAL_MS = 20000L

        var isServicePlaying = false
    }

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var notificationManager: NotificationManager
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var metadataUpdateRunnable: Runnable? = null
    private var keepAliveRunnable: Runnable? = null

    private var currentArtist = "Radio Teate OnAir"
    private var currentTitle = "In ascolto..."
    private var isPlaying = false
    private var isServiceDestroyed = false
    private val isPreparing = AtomicBoolean(false)
    private val isPrepared = AtomicBoolean(false)

    private var lastActionTime = 0L
    private val actionLock = Any()

    override fun onCreate() {
        super.onCreate()
        setupNotificationManager()
        prefetchAudioStream()
    }

    private fun fixUtf8Encoding(garbledText: String): String {
        return try {
            garbledText.toByteArray(Charsets.ISO_8859_1).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            garbledText
        }
    }

    private fun prefetchAudioStream() {
        if (isPreparing.get() || isPrepared.get()) {
            return
        }

        isPreparing.set(true)

        executor.execute {
            try {
                mediaPlayer?.let {
                    try {
                        it.stop()
                        it.reset()
                        it.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

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
                    }

                    setOnErrorListener { mp, what, extra ->
                        isPreparing.set(false)
                        isPrepared.set(false)
                        handler.postDelayed({ prefetchAudioStream() }, 2000)
                        true
                    }

                    setOnCompletionListener {
                        isPrepared.set(false)
                        prefetchAudioStream()
                    }

                    try {
                        setDataSource(STREAM_URL)
                        prepareAsync()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        isPreparing.set(false)
                        isPrepared.set(false)
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

    private fun startKeepAliveMonitor() {
        if (keepAliveRunnable != null) {
            return
        }

        keepAliveRunnable = object : Runnable {
            override fun run() {
                try {
                    mediaPlayer?.let { player ->
                        if (isPlaying && player.isPlaying) {
                            player.duration
                            player.currentPosition
                            player.audioSessionId
                        }
                    }

                    if (isPlaying && !isServiceDestroyed) {
                        handler.postDelayed(this, KEEP_ALIVE_INTERVAL_MS)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (isPlaying && !isServiceDestroyed) {
                        handler.postDelayed(this, KEEP_ALIVE_INTERVAL_MS)
                    }
                }
            }
        }

        handler.post(keepAliveRunnable!!)
    }

    private fun stopKeepAliveMonitor() {
        keepAliveRunnable?.let {
            handler.removeCallbacks(it)
        }
        keepAliveRunnable = null
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
            .build()

        return notification
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, RadioService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startMetadataUpdater() {
        if (metadataUpdateRunnable != null) {
            return
        }

        metadataUpdateRunnable = object : Runnable {
            override fun run() {
                if (!isServiceDestroyed && isPlaying) {
                    executor.execute {
                        var connection: java.net.HttpURLConnection? = null
                        try {
                            connection = URL("https://radioteateonair.it:8663/status-json.xsl")
                                .openConnection() as java.net.HttpURLConnection
                            connection.connectTimeout = 5000
                            connection.readTimeout = 5000

                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(response)

                            val fullTitle = json
                                .getJSONObject("icestats")
                                .getJSONObject("source")
                                .getString("title") //yp_currently_playing

                            val parts = fullTitle.split(" - ", limit = 2)
                            val artist = if (parts.isNotEmpty()) parts[0] else "Teate On Air"
                            val song = if (parts.size > 1) parts[1] else "In onda"

                            handler.post {
                                if (isPlaying && !isServiceDestroyed) {
                                    currentTitle = song
                                    currentArtist = artist
                                    updateNotification()
                                    metadataUpdateRunnable?.let { handler.postDelayed(it, 5000) }
                                }
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (isPlaying && !isServiceDestroyed) {
                                metadataUpdateRunnable?.let { handler.postDelayed(it, 5000) }
                            }
                        } finally {
                            try {
                                connection?.disconnect()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
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

        if (intent?.action != ACTION_PREFETCH) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastActionTime < DEBOUNCE_DELAY_MS) {
                return START_STICKY
            }
            lastActionTime = currentTime
        }

        when (intent?.action) {
            ACTION_PREFETCH -> {
                prefetchAudioStream()
            }
            ACTION_PLAY -> {
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
                prefetchAudioStream()
            }
        }

        return START_STICKY
    }

    private fun playStream() {
        synchronized(actionLock) {
            if (isPlaying) {
                return
            }

            val player = mediaPlayer
            if (player == null) {
                prefetchAudioStream()
                handler.postDelayed({ playStream() }, 500)
                return
            }

            try {
                when {
                    isPrepared.get() -> {
                        if (!player.isPlaying) {
                            player.start()
                            isPlaying = true
                            isServicePlaying = true

                            startKeepAliveMonitor()

                            val broadcastIntent = Intent(MainActivity.ACTION_AUDIO_STARTED)
                            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)

                            updateNotification()
                        }
                        Unit
                    }
                    isPreparing.get() -> {
                        handler.postDelayed({ playStream() }, 200)
                    }
                    else -> {
                        prefetchAudioStream()
                        handler.postDelayed({ playStream() }, 500)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isPlaying = false
                isPrepared.set(false)
                prefetchAudioStream()
            }
        }
    }

    private fun stopStreamButKeepNotification() {
        synchronized(actionLock) {
            if (!isPlaying) {
                return
            }

            isPlaying = false
            isServicePlaying = false

            stopKeepAliveMonitor()

            try {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val broadcastIntent = Intent(MainActivity.ACTION_AUDIO_STOPPED)
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)

            updateNotification()
        }
    }

    private fun closeNotificationAndService() {
        synchronized(actionLock) {
            isPlaying = false
            isServicePlaying = false
            isPrepared.set(false)
            isPreparing.set(false)

            metadataUpdateRunnable?.let { handler.removeCallbacks(it) }
            stopKeepAliveMonitor()

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

            val broadcastIntent = Intent(MainActivity.ACTION_AUDIO_STOPPED)
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)

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

        metadataUpdateRunnable?.let { handler.removeCallbacks(it) }
        stopKeepAliveMonitor()
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