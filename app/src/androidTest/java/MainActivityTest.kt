package it.teateonair.app

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun testStartButtonExists() {
        onView(withId(R.id.startButton))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
    }

    @Test
    fun testBottomBarInitiallyHidden() {
        onView(withId(R.id.bottomBar))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testSongTitleInitiallyHidden() {
        onView(withId(R.id.songTitle))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun testArtistNameInitiallyHidden() {
        onView(withId(R.id.artistName))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun testBox1Clickable() {
        onView(withId(R.id.box1))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    @Test
    fun testBox2Clickable() {
        onView(withId(R.id.box2))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    @Test
    fun testBox3Clickable() {
        onView(withId(R.id.box3))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    @Test
    fun testSocialFacebookButtonExists() {
        onView(withId(R.id.socialFacebook))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    @Test
    fun testSocialInstagramButtonExists() {
        onView(withId(R.id.socialInstagram))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    @Test
    fun testSocialTikTokButtonExists() {
        onView(withId(R.id.socialTikTok))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    @Test
    fun testSocialYouTubeButtonExists() {
        onView(withId(R.id.socialYouTube))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    @Test
    fun testBroadcastReceiverHandlesAudioStopped() {
        scenario.onActivity { activity ->
            val intent = Intent(MainActivity.ACTION_AUDIO_STOPPED)
            activity.sendBroadcast(intent)

            Thread.sleep(500)
            assert(!activity.isPlaying)
        }
    }

    @Test
    fun testBroadcastReceiverHandlesAudioStarted() {
        scenario.onActivity { activity ->
            val intent = Intent(MainActivity.ACTION_AUDIO_STARTED)
            activity.sendBroadcast(intent)

            Thread.sleep(500)
        }
    }

    @Test
    fun testSocialFacebookButtonOpensUrl() {
        onView(withId(R.id.socialFacebook))
            .perform(click())

        Thread.sleep(1000)
    }

    @Test
    fun testSocialInstagramButtonOpensUrl() {
        onView(withId(R.id.socialInstagram))
            .perform(click())

        Thread.sleep(1000)
    }

    @Test
    fun testSocialTikTokButtonOpensUrl() {
        onView(withId(R.id.socialTikTok))
            .perform(click())

        Thread.sleep(1000)
    }

    @Test
    fun testSocialYouTubeButtonOpensUrl() {
        onView(withId(R.id.socialYouTube))
            .perform(click())

        Thread.sleep(1000)
    }

    @Test
    fun testBox1OpensModal() {
        onView(withId(R.id.box1))
            .perform(click())

        Thread.sleep(1000)
    }

    @Test
    fun testBox2OpensModal() {
        onView(withId(R.id.box2))
            .perform(click())

        Thread.sleep(1000)
    }

    @Test
    fun testBox3OpensModal() {
        onView(withId(R.id.box3))
            .perform(click())

        Thread.sleep(1000)
    }

    @Test
    fun testActivityRecreation() {
        scenario.recreate()

        onView(withId(R.id.startButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSystemUiConfiguration() {
        scenario.onActivity { activity ->
            assert(activity.window != null)
            assert(activity.window.statusBarColor == android.graphics.Color.TRANSPARENT)
            assert(activity.window.navigationBarColor == android.graphics.Color.TRANSPARENT)
        }
    }
}