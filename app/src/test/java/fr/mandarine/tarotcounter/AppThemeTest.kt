package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for AppTheme.kt.
 *
 * AppTheme is used in two ways:
 *   1. UI: composables switch their color palette based on the current value.
 *   2. Persistence: GameStorage serializes the enum via `theme.name` and deserializes
 *      it with `AppTheme.valueOf(string)`. Both directions must round-trip correctly.
 *
 * Run with: ./gradlew testDebugUnitTest
 */
class AppThemeTest {

    // ── Enum structure ────────────────────────────────────────────────────────

    @Test
    fun `AppTheme has exactly two values`() {
        // If a new value is added the persistence layer (GameStorage) must be
        // updated, and this test will fail to draw attention to that requirement.
        assertEquals(2, AppTheme.entries.size)
    }

    @Test
    fun `AppTheme contains LIGHT`() {
        assertNotNull(AppTheme.entries.find { it == AppTheme.LIGHT })
    }

    @Test
    fun `AppTheme contains DARK`() {
        assertNotNull(AppTheme.entries.find { it == AppTheme.DARK })
    }

    // ── Serialization round-trip ──────────────────────────────────────────────
    //
    // GameStorage persists the theme as theme.name ("LIGHT" or "DARK") and reads
    // it back with AppTheme.valueOf(string). Both names must survive the round-trip.

    @Test
    fun `LIGHT name round-trips through valueOf`() {
        assertEquals(AppTheme.LIGHT, AppTheme.valueOf(AppTheme.LIGHT.name))
    }

    @Test
    fun `DARK name round-trips through valueOf`() {
        assertEquals(AppTheme.DARK, AppTheme.valueOf(AppTheme.DARK.name))
    }

    @Test
    fun `LIGHT and DARK have distinct names`() {
        // Distinct names are required for correct DataStore serialization.
        assert(AppTheme.LIGHT.name != AppTheme.DARK.name)
    }

    @Test
    fun `valueOf with unknown string throws IllegalArgumentException`() {
        // GameStorage wraps this call in runCatching so corrupt data won't crash;
        // but the underlying throw must happen for that safety net to work.
        try {
            AppTheme.valueOf("UNKNOWN_THEME")
            assert(false) { "Expected IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    // ── Enum ordinal order ────────────────────────────────────────────────────
    //
    // LIGHT is the default (used by LocalAppTheme and MainActivity), so it should
    // remain the first entry. Ordinal order is used for `entries` comparison.

    @Test
    fun `LIGHT is the first entry (default theme)`() {
        assertEquals(AppTheme.LIGHT, AppTheme.entries.first())
    }
}
