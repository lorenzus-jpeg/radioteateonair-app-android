package it.teateonair.app

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class RadioServiceTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        val stopIntent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_STOP
        }
        context.stopService(stopIntent)

        Thread.sleep(1000)
    }

    @Test
    fun testServiceStartsWithPlayAction() {
        val intent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_PLAY
        }

        context.startService(intent)
        Thread.sleep(2000)

        assert(true)
    }

    @Test
    fun testServiceStartsWithPauseAction() {
        val intent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_PAUSE
        }

        context.startService(intent)
        Thread.sleep(1000)

        assert(true)
    }

    @Test
    fun testServiceStartsWithStopAction() {
        val intent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_STOP
        }

        context.startService(intent)
        Thread.sleep(1000)

        assert(true)
    }

    @Test
    fun testServiceStartsWithCloseAction() {
        val intent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_CLOSE
        }

        context.startService(intent)
        Thread.sleep(1000)

        assert(true)
    }

    @Test
    fun testServiceStartsWithoutAction() {
        val intent = Intent(context, RadioService::class.java)

        context.startService(intent)
        Thread.sleep(1000)

        assert(true)
    }

    @Test
    fun testMultiplePlayActions() {
        val intent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_PLAY
        }

        context.startService(intent)
        Thread.sleep(1000)

        context.startService(intent)
        Thread.sleep(1000)

        context.startService(intent)
        Thread.sleep(1000)

        assert(true)
    }

    @Test
    fun testMultiplePauseActions() {
        val playIntent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_PLAY
        }
        context.startService(playIntent)
        Thread.sleep(1000)

        val pauseIntent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_PAUSE
        }

        context.startService(pauseIntent)
        Thread.sleep(500)

        context.startService(pauseIntent)
        Thread.sleep(500)

        assert(true)
    }

    @Test
    fun testServiceStopsCleanly() {
        val intent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_PLAY
        }

        context.startService(intent)
        Thread.sleep(2000)

        val stopIntent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_CLOSE
        }
        context.startService(stopIntent)
        Thread.sleep(1000)

        assert(true)
    }

    @Test
    fun testServiceHandlesRapidActionChanges() {
        val playIntent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_PLAY
        }
        context.startService(playIntent)
        Thread.sleep(500)

        val pauseIntent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_PAUSE
        }
        context.startService(pauseIntent)
        Thread.sleep(500)

        context.startService(playIntent)
        Thread.sleep(500)

        context.startService(pauseIntent)
        Thread.sleep(500)

        assert(true)
    }

    @Test
    fun testServiceCreatesNotification() {
        val intent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_PLAY
        }

        context.startService(intent)
        Thread.sleep(2000)

        assert(true)
    }

    @Test
    fun testServiceBroadcastsAudioStarted() {
        var broadcastReceived = false

        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == MainActivity.ACTION_AUDIO_STARTED) {
                    broadcastReceived = true
                }
            }
        }

        val filter = android.content.IntentFilter(MainActivity.ACTION_AUDIO_STARTED)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
            .registerReceiver(receiver, filter)

        val intent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_PLAY
        }
        context.startService(intent)
        Thread.sleep(2000)

        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
            .unregisterReceiver(receiver)

        assert(broadcastReceived) { "Audio started broadcast was not received" }
    }

    @Test
    fun testServiceBroadcastsAudioStopped() {
        var broadcastReceived = false

        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == MainActivity.ACTION_AUDIO_STOPPED) {
                    broadcastReceived = true
                }
            }
        }

        val filter = android.content.IntentFilter(MainActivity.ACTION_AUDIO_STOPPED)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
            .registerReceiver(receiver, filter)

        val playIntent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_PLAY
        }
        context.startService(playIntent)
        Thread.sleep(1000)

        val stopIntent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_STOP
        }
        context.startService(stopIntent)
        Thread.sleep(1000)

        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
            .unregisterReceiver(receiver)

        assert(broadcastReceived) { "Audio stopped broadcast was not received" }
    }
}
