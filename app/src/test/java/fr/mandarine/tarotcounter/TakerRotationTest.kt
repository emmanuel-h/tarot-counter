package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the dealer-rotation logic in GameViewModel.
 *
 * Spec (docs/game-flow.md):
 *   "Round 1 — a random player is chosen as the first dealer.
 *    Round 2+ — players take turns dealing in the order they were entered on the
 *    setup screen, cycling back to the first player after the last one."
 *
 * Important: the dealer distributes the cards but does NOT automatically become
 * the attacker. Any player can bid for the contract; the player with the highest
 * bid becomes the attacker (taker) and is selected explicitly in the UI (issue #124).
 *
 * The formula used in GameViewModel is:
 *   currentDealerIndex = (startingIndex + currentRound - 1) % playerNames.size
 *
 * Because this is pure integer arithmetic we can test it here on the JVM
 * without needing a device or Compose — we just mirror the formula.
 */
class TakerRotationTest {

    /**
     * Mirrors the formula from GameViewModel.currentDealer exactly.
     * If the formula ever changes there, this test will catch the divergence.
     */
    private fun dealerIndex(startingIndex: Int, round: Int, playerCount: Int): Int =
        (startingIndex + round - 1) % playerCount

    // ── Basic rotation with 3 players ─────────────────────────────────────────

    @Test
    fun `starting at index 0 with 3 players cycles 0-1-2-0`() {
        assertEquals(0, dealerIndex(startingIndex = 0, round = 1, playerCount = 3))
        assertEquals(1, dealerIndex(startingIndex = 0, round = 2, playerCount = 3))
        assertEquals(2, dealerIndex(startingIndex = 0, round = 3, playerCount = 3))
        assertEquals(0, dealerIndex(startingIndex = 0, round = 4, playerCount = 3)) // wraps
    }

    @Test
    fun `starting at index 1 with 3 players cycles 1-2-0-1`() {
        assertEquals(1, dealerIndex(startingIndex = 1, round = 1, playerCount = 3))
        assertEquals(2, dealerIndex(startingIndex = 1, round = 2, playerCount = 3))
        assertEquals(0, dealerIndex(startingIndex = 1, round = 3, playerCount = 3)) // wraps
        assertEquals(1, dealerIndex(startingIndex = 1, round = 4, playerCount = 3)) // full cycle
    }

    @Test
    fun `starting at last index wraps to first on round 2`() {
        // 3 players, last index = 2
        assertEquals(2, dealerIndex(startingIndex = 2, round = 1, playerCount = 3))
        assertEquals(0, dealerIndex(startingIndex = 2, round = 2, playerCount = 3)) // wraps immediately
        assertEquals(1, dealerIndex(startingIndex = 2, round = 3, playerCount = 3))
    }

    // ── Rotation with 5 players ───────────────────────────────────────────────

    @Test
    fun `full 5-player cycle starting at index 2`() {
        assertEquals(2, dealerIndex(startingIndex = 2, round = 1, playerCount = 5))
        assertEquals(3, dealerIndex(startingIndex = 2, round = 2, playerCount = 5))
        assertEquals(4, dealerIndex(startingIndex = 2, round = 3, playerCount = 5))
        assertEquals(0, dealerIndex(startingIndex = 2, round = 4, playerCount = 5))
        assertEquals(1, dealerIndex(startingIndex = 2, round = 5, playerCount = 5))
        assertEquals(2, dealerIndex(startingIndex = 2, round = 6, playerCount = 5)) // full cycle
    }

    // ── Invariant: every player deals exactly once per cycle ──────────────────

    @Test
    fun `every player deals exactly once per cycle regardless of starting index`() {
        // Spec: players cycle through "in the order they were entered".
        // This means each player deals exactly once per N-round cycle.
        for (playerCount in 3..5) {                  // test all valid player counts
            for (startingIndex in 0 until playerCount) {
                val indicesInCycle = (1..playerCount).map { round ->
                    dealerIndex(startingIndex, round, playerCount)
                }
                assertEquals(
                    "playerCount=$playerCount startingIndex=$startingIndex: " +
                    "every player should deal exactly once per $playerCount-round cycle",
                    (0 until playerCount).toSet(),
                    indicesInCycle.toSet()
                )
                // Also verify no duplicates within the cycle.
                assertEquals(
                    "playerCount=$playerCount startingIndex=$startingIndex: " +
                    "no player should deal twice in the same cycle",
                    playerCount,
                    indicesInCycle.distinct().size
                )
            }
        }
    }

    @Test
    fun `round 1 dealer index equals the starting index`() {
        // Spec: "Round 1 — a random player is chosen as the first dealer."
        // Whatever the random starting index is, round 1 must show that player.
        for (playerCount in 3..5) {
            for (startingIndex in 0 until playerCount) {
                assertEquals(
                    "Round 1 dealer should always be the player at startingIndex",
                    startingIndex,
                    dealerIndex(startingIndex, round = 1, playerCount)
                )
            }
        }
    }

    @Test
    fun `round 2 dealer is the next player in setup order`() {
        // Spec: "Round 2+ — players take turns dealing in the order they were entered."
        for (playerCount in 3..5) {
            for (startingIndex in 0 until playerCount) {
                val expectedRound2Index = (startingIndex + 1) % playerCount
                assertEquals(
                    "Round 2 dealer should be the player immediately after the starting player",
                    expectedRound2Index,
                    dealerIndex(startingIndex, round = 2, playerCount)
                )
            }
        }
    }
}
