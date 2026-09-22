package fr.mandarine.tarotcounter

// ─────────────────────────────────────────────────────────────────────────────
// Data behind the tables of the Rules page (issue #203). Built from the same
// functions the scoring uses, so the rules shown can never disagree with the
// scores computed. Unit-tested in RulesDataTest.
// ─────────────────────────────────────────────────────────────────────────────

/** (bouts, points needed) for 0 to 3 bouts, from [requiredPoints]. */
fun boutThresholdRows(): List<Pair<Int, Int>> = (0..3).map { bouts -> bouts to requiredPoints(bouts) }

/** (contract, "×N") for every contract, weakest first. */
fun contractMultiplierRows(): List<Pair<Contract, String>> =
    Contract.entries.map { it to "×${it.multiplier}" }
