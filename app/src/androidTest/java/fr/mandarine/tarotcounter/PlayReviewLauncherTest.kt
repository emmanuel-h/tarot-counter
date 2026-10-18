package fr.mandarine.tarotcounter

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.review.testing.FakeReviewManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for [PlayReviewLauncher] (issue #223).
 *
 * `FakeReviewManager` ships with the Play review library: it answers like the real
 * manager (a ReviewInfo, then a completed launch) but never shows any UI, so the
 * whole request → launch path can be exercised on a device without the Play Store.
 */
@RunWith(AndroidJUnit4::class)
class PlayReviewLauncherTest {

    @Test
    fun launch_with_fake_review_manager_reports_that_the_flow_ran() {
        val fake = FakeReviewManager(ApplicationProvider.getApplicationContext())
        val launcher = PlayReviewLauncher(fake)

        // The review flow needs a real Activity to attach to.
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var activity: Activity? = null
            scenario.onActivity { activity = it }
            // runBlocking bridges the suspend `launch` into this plain test function.
            val launched = runBlocking { launcher.launch(activity!!) }
            assertTrue("FakeReviewManager flow should complete", launched)
        }
    }
}
