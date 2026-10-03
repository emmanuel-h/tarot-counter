package fr.mandarine.tarotcounter

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/**
 * Generates the Google Play screenshots (issue #205) — not a regular test.
 *
 * Every screen is rendered with realistic sample data in EN / FR × light / dark
 * and saved as a 1080 × 1920 PNG (9:16, within Play's 2:1 limit). It only runs
 * when explicitly asked for, so normal test runs are unaffected:
 *
 *     ./gradlew connectedDebugAndroidTest \
 *         -Pandroid.testInstrumentationRunnerArguments.class=fr.mandarine.tarotcounter.StoreScreenshots \
 *         -Pandroid.testInstrumentationRunnerArguments.storeScreenshots=true
 *     adb pull /sdcard/Android/data/fr.mandarine.tarotcounter/files/store store/
 *
 * See docs/store-assets.md.
 */
@RunWith(Parameterized::class)
class StoreScreenshots(private val locale: AppLocale, private val dark: Boolean) {

    companion object {
        // Parameterized runs every test once per (locale, theme) pair — or not at
        // all when screenshots were not asked for: an empty parameter list
        // registers no test, so normal runs report nothing (a JUnit assumption
        // would be reported as a failure by the Android test runner).
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-dark={1}")
        fun combos(): List<Array<Any>> {
            val asked = InstrumentationRegistry.getArguments().getString("storeScreenshots") == "true"
            if (!asked) return emptyList()
            return listOf(
                arrayOf(AppLocale.EN, false), arrayOf(AppLocale.EN, true),
                arrayOf(AppLocale.FR, false), arrayOf(AppLocale.FR, true)
            )
        }

        private val players = listOf("Alice", "Bruno", "Chloé", "David")
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Sample game ───────────────────────────────────────────────────────────

    private fun details(bouts: Int, points: Int, petit: String? = null, poignees: List<String> = emptyList()) =
        RoundDetails(bouts = bouts, points = points, partnerName = null, petitAuBout = petit,
            poignees = poignees, chelem = Chelem.NONE)

    /** A game four rounds in, with a clear leader and some movement. */
    private fun sampleViewModel(): GameViewModel {
        val vm = GameViewModel(ApplicationProvider.getApplicationContext<Application>(), FakeGameStorage())
        vm.initGame(players, inProgressGame = null, startingIndexOverride = 3)
        vm.recordPlayed("Alice", Contract.GARDE, details(2, 52, poignees = listOf("Alice")))
        vm.recordPlayed("Bruno", Contract.PRISE, details(1, 44))
        vm.recordSkipped()
        vm.recordPlayed("Chloé", Contract.GARDE, details(3, 49, petit = "Chloé"))
        vm.recordPlayed("Alice", Contract.PRISE, details(2, 45))
        return vm
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /** Renders [content] in a fixed 1080 × 1920 px frame with the right theme and locale. */
    private fun shoot(name: String, content: @Composable () -> Unit, actions: () -> Unit = {}) {
        composeTestRule.setContent {
            TarotCounterTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalAppLocale provides locale,
                    LocalAppTheme provides if (dark) AppTheme.DARK else AppTheme.LIGHT,
                    // No count-up mid-capture.
                    LocalReducedMotion provides true
                ) {
                    val density = LocalDensity.current
                    // requiredSize in dp that equals exactly 1080 × 1920 px on this device.
                    val w = with(density) { 1080.toDp() }
                    val h = with(density) { 1920.toDp() }
                    // A Surface (like the app's Scaffold) provides both the page
                    // background and the matching text colour for dark mode.
                    Surface(
                        modifier = Modifier
                            .requiredSize(w, h)
                            .testTag("frame"),
                        color    = MaterialTheme.colorScheme.background
                    ) { content() }
                }
            }
        }
        actions()
        composeTestRule.waitForIdle()
        save(name, composeTestRule.onNodeWithTag("frame").captureToImage().asAndroidBitmap())
    }

    private fun save(name: String, bitmap: Bitmap) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val theme = if (dark) "dark" else "light"
        val dir = File(context.getExternalFilesDir(null), "store/${locale.name.lowercase()}/$theme")
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    // ── Screens ───────────────────────────────────────────────────────────────

    @Test
    fun s1_home() {
        val vm = sampleViewModel()
        val now = System.currentTimeMillis()
        shoot("1_home", {
            LandingScreen(
                inProgressGame = InProgressGame("g", players, vm.currentRound, 3, vm.roundHistory.toList()),
                pastGames = listOf(
                    SavedGame("p1", now - 2 * 86_400_000L, listOf("Marc", "Alice", "Bruno", "Chloé", "David"),
                        vm.roundHistory.toList(), mapOf("Marc" to 540, "Alice" to -120, "Bruno" to -140, "Chloé" to -130, "David" to -150)),
                    SavedGame("p2", now - 9 * 86_400_000L, listOf("Chloé", "Alice", "Bruno", "David"),
                        vm.roundHistory.toList(), mapOf("Chloé" to 288, "Alice" to -96, "Bruno" to -96, "David" to -96))
                )
            )
        })
    }

    @Test
    fun s2_game() {
        val vm = sampleViewModel()
        shoot("2_game", { GameScreen(viewModel = vm) })
    }

    @Test
    fun s3_round_entry() {
        val vm = sampleViewModel()
        shoot("3_round_entry", { GameScreen(viewModel = vm) }) {
            composeTestRule.onNodeWithTag("taker_tile_Chloé").performClick()
            composeTestRule.onNodeWithTag("contract_GARDE").performClick()
            composeTestRule.onNodeWithTag("bouts_chip_2").performClick()
            composeTestRule.onNodeWithTag("points_input").performTextInput("47")
            Espresso.closeSoftKeyboard()
            // Typing scrolled the form; bring the contract cards back into view.
            composeTestRule.onNodeWithTag("contract_PRISE").performScrollTo()
        }
    }

    @Test
    fun s4_game_over() {
        val vm = sampleViewModel()
        shoot("4_game_over", {
            FinalScoreScreen(players, vm.roundHistory.toList(), onBack = {}, onNewGame = {}, onMainMenu = {})
        })
    }

    @Test
    fun s5_history() {
        val vm = sampleViewModel()
        shoot("5_history", { ScoreHistoryScreen(players, vm.roundHistory.toList(), onBack = {}) })
    }
}
