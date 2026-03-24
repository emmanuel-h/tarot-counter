package fr.mandarine.tarotcounter

import androidx.compose.runtime.staticCompositionLocalOf

// ── Locale enum ───────────────────────────────────────────────────────────────
//
// EN = English, FR = French.
// More locales can be added here in the future without changing any other code.
enum class AppLocale { EN, FR }

// ── CompositionLocal ──────────────────────────────────────────────────────────
//
// CompositionLocal lets any composable in the tree read the current locale without
// threading it through every function parameter.
//
// Usage:  val locale = LocalAppLocale.current
//         val strings = appStrings(locale)
//
// `staticCompositionLocalOf` is used instead of `compositionLocalOf` because locale
// changes always recompose the entire tree (via CompositionLocalProvider in MainActivity),
// so the extra per-caller tracking of `compositionLocalOf` would add cost with no benefit.
// The default value is EN so that Previews and tests that don't set a provider
// still compile and render correctly.
val LocalAppLocale = staticCompositionLocalOf { AppLocale.EN }

// ── String bundle ─────────────────────────────────────────────────────────────
//
// AppStrings holds every user-visible string for one language.
// Static text uses `val field: String`.
// Text with dynamic parts (player names, counts, scores) uses a lambda so the
// caller composes the final string with the right values at display time.
data class AppStrings(

    // ── Landing Screen ────────────────────────────────────────────────────────
    val appTitle: String,
    val numberOfPlayers: String,
    // Section header above the name text fields.
    val playerNamesLabel: String,
    // Default label for a blank player slot, e.g. playerFallback(1) → "Player 1".
    // The index is 1-based so the displayed label matches what the user sees.
    val playerFallback: (index: Int) -> String,
    val nameAlreadyUsed: String,
    val startGame: String,
    val pastGames: String,
    // Title of the "resume" card shown when an unfinished game exists.
    val resumeGameTitle: String,
    // "1 round played" vs "N rounds played" for the resume-card subtitle.
    val roundsPlayed: (count: Int) -> String,
    // Combines a round number with a rounds-played label, e.g. "Round 3 · 2 rounds played".
    val resumeRoundDetail: (round: Int, label: String) -> String,
    val resume: String,
    val noRoundsPlayed: String,
    // Single-winner summary in the past-game card, e.g. "Winner: Alice (+120)".
    val winnerResult: (name: String, sign: String, score: Int) -> String,
    // Tie summary in the past-game card, e.g. "Tie: Alice & Bob".
    val tieResult: (names: String) -> String,
    // "1 round" vs "N rounds" shown in the past-game card footer.
    val roundCount: (count: Int) -> String,

    // ── Game Screen ───────────────────────────────────────────────────────────
    // "Round 1" / "Manche 1" header.
    val roundHeader: (n: Int) -> String,
    // "{Player} — choose a contract:" prompt above the contract chips.
    val chooseContract: (taker: String) -> String,
    val skipRound: String,
    val numberOfBouts: String,
    val pointsScoredByTaker: String,
    // Segmented-button label for the attacker (taker) side of the toggle.
    val attackerMode: String,
    // Segmented-button label for the defenders' side of the toggle.
    val defenderMode: String,
    val partnerCalledByTaker: String,
    val confirmRound: String,
    val changeContract: String,
    val scores: String,
    val history: String,
    val endGame: String,
    // Tooltip title and section label for the chelem bonus (still used by the ⓘ icon).
    val chelemLabel: String,
    // Text shown inside the dropdown field when no chelem is selected (Chelem.NONE).
    // This acts as a self-describing placeholder so no separate section header is needed.
    val chelemPlaceholder: String,
    // Label for the player selector shown below the chelem dropdown when a non-None option is chosen.
    val chelemPlayerLabel: String,
    // Informational note shown when a chelem is announced, reminding the table that the
    // named player leads the first trick. E.g. "Alice plays first this round."
    val chelemPlaysFirst: (playerName: String) -> String,
    // "None" chip in the partner selector (5-player games only).
    val noneOption: String,
    // Compact bonus grid row labels — all French Tarot terms, unchanged in both languages.
    val petit: String,
    val poignee: String,
    val doublePoignee: String,
    val triplePoignee: String,
    // Tooltip body texts shown when the user taps the ⓘ icon next to a bonus or chelem label.
    val petitTooltipBody: String,
    val poigneeTooltipBody: String,
    val doublePoigneeTooltipBody: String,
    val triplePoigneeTooltipBody: String,
    val chelemTooltipBody: String,
    // Hint shown below the points text field to communicate the valid range.
    val pointsRange: String,
    // Error shown below the points text field when the entered value exceeds 91.
    val pointsOutOfRange: String,
    // Confirmation dialog for "Skip round".
    val skipRoundConfirmTitle: String,
    val skipRoundConfirmBody: String,
    // Confirmation dialog for "End Game".
    val endGameConfirmTitle: String,
    val endGameConfirmBody: String,
    // Generic action labels used in confirmation dialogs.
    val cancel: String,
    // "Skipped" label in the round-history list when no contract was played.
    val skipped: String,
    // "Round N: Taker — " prefix used in each round-history line.
    val roundHistoryPrefix: (roundNum: Int, taker: String) -> String,
    // " · N bouts · N pts" details appended to a history line when available.
    val boutsPointsDetail: (bouts: Int, points: Int) -> String,
    // " — Won (+N)" outcome segment; `score` is already formatted with sign.
    val wonOutcome: (score: String) -> String,
    // " — Lost (N)" outcome segment.
    val lostOutcome: (score: String) -> String,

    // ── Final Score Screen ────────────────────────────────────────────────────
    val backToGame: String,
    val gameOver: String,
    val winner: String,
    // "+N pts" score label shown below the winner name.
    val scoreDisplay: (sign: String, score: Int) -> String,
    val itsATie: String,
    val newGame: String,

    // ── Score History Screen ──────────────────────────────────────────────────
    val scoreHistory: String,
    // "Round" / "Manche" column header in the score table.
    val roundColumn: String,

    // ── Chelem enum labels ────────────────────────────────────────────────────
    val chelemNone: String,
    val chelemAnnouncedRealized: String,
    val chelemAnnouncedNotRealized: String,
    val chelemNotAnnouncedRealized: String,
)

// ── English strings ───────────────────────────────────────────────────────────

val EnStrings = AppStrings(
    appTitle              = "Tarot Counter",
    numberOfPlayers       = "Number of players",
    playerNamesLabel      = "Player names",
    playerFallback        = { i -> "Player $i" },
    nameAlreadyUsed       = "Name already used",
    startGame             = "Start Game",
    pastGames             = "Past Games",
    resumeGameTitle       = "Resume Game",
    roundsPlayed          = { n -> if (n == 1) "1 round played" else "$n rounds played" },
    resumeRoundDetail     = { round, label -> "Round $round · $label" },
    resume                = "Resume",
    noRoundsPlayed        = "No rounds played",
    winnerResult          = { name, sign, score -> "Winner: $name ($sign$score)" },
    tieResult             = { names -> "Tie: $names" },
    roundCount            = { n -> if (n == 1) "1 round" else "$n rounds" },

    roundHeader           = { n -> "Round $n" },
    chooseContract        = { taker -> "$taker — choose a contract:" },
    skipRound             = "Skip round",
    numberOfBouts         = "Number of bouts (oudlers)",
    pointsScoredByTaker   = "Points scored by taker",
    attackerMode          = "Attacker",
    defenderMode          = "Defenders",
    partnerCalledByTaker  = "Partner (called by taker)",
    confirmRound          = "Confirm round",
    changeContract        = "← Change contract",
    scores                = "Scores",
    history               = "History",
    endGame               = "End Game",
    chelemLabel           = "Chelem (grand slam)",
    chelemPlaceholder     = "Chelem",
    chelemPlayerLabel     = "Who called the chelem?",
    chelemPlaysFirst      = { name -> "$name plays first this round." },
    noneOption            = "None",
    petit                 = "Petit",
    poignee               = "Poignée",
    doublePoignee         = "Dbl poignée",
    triplePoignee         = "Trp poignée",
    petitTooltipBody      = "The Petit (1 of trumps) is played in the last trick.\n+10 pts × contract multiplier.",
    poigneeTooltipBody    = "10 trumps shown before play.\nBonus: 20 pts per player.",
    doublePoigneeTooltipBody = "13 trumps shown before play.\nBonus: 30 pts per player.",
    triplePoigneeTooltipBody = "15 trumps shown before play.\nBonus: 40 pts per player.",
    chelemTooltipBody     = "All tricks won by the same team.\n\nAnnounced & realized: +400 pts\nNot announced, realized: +200 pts\nAnnounced, not realized: −200 pts",
    pointsRange           = "0 – 91",
    pointsOutOfRange      = "Must be between 0 and 91",
    skipRoundConfirmTitle = "Skip this round?",
    skipRoundConfirmBody  = "No contract will be recorded for this round.",
    endGameConfirmTitle   = "End the game?",
    endGameConfirmBody    = "The current round will not be saved.",
    cancel                = "Cancel",
    skipped               = "Skipped",
    roundHistoryPrefix    = { n, taker -> "Round $n: $taker — " },
    boutsPointsDetail     = { bouts, points -> " · $bouts bouts · $points pts" },
    wonOutcome            = { s -> " — Won$s" },
    lostOutcome           = { s -> " — Lost$s" },

    backToGame            = "Back to game",
    gameOver              = "Game Over",
    winner                = "Winner",
    scoreDisplay          = { sign, score -> "$sign$score pts" },
    itsATie               = "It's a tie!",
    newGame               = "New Game",

    scoreHistory          = "Score history",
    roundColumn           = "Round",

    chelemNone               = "None",
    chelemAnnouncedRealized  = "Announced & realized",
    chelemAnnouncedNotRealized = "Announced, not realized",
    chelemNotAnnouncedRealized = "Not announced, realized",
)

// ── French strings ────────────────────────────────────────────────────────────

val FrStrings = AppStrings(
    appTitle              = "Compteur de points",
    numberOfPlayers       = "Nombre de joueurs",
    playerNamesLabel      = "Noms des joueurs",
    playerFallback        = { i -> "Joueur $i" },
    nameAlreadyUsed       = "Nom déjà utilisé",
    startGame             = "Démarrer",
    pastGames             = "Parties précédentes",
    resumeGameTitle       = "Partie en cours",
    roundsPlayed          = { n -> if (n == 1) "1 manche jouée" else "$n manches jouées" },
    resumeRoundDetail     = { round, label -> "Manche $round · $label" },
    resume                = "Reprendre",
    noRoundsPlayed        = "Aucune manche jouée",
    winnerResult          = { name, sign, score -> "Gagnant : $name ($sign$score)" },
    tieResult             = { names -> "Égalité : $names" },
    roundCount            = { n -> if (n == 1) "1 manche" else "$n manches" },

    roundHeader           = { n -> "Manche $n" },
    chooseContract        = { taker -> "$taker — choisissez un contrat :" },
    skipRound             = "Passer la manche",
    numberOfBouts         = "Nombre de bouts (oudlers)",
    pointsScoredByTaker   = "Points marqués par le preneur",
    attackerMode          = "Attaquant",
    defenderMode          = "Défenseurs",
    partnerCalledByTaker  = "Appelé (par le preneur)",
    confirmRound          = "Valider la manche",
    changeContract        = "← Changer de contrat",
    scores                = "Scores",
    history               = "Historique",
    endGame               = "Fin de partie",
    chelemLabel           = "Chelem (grand chelem)",
    chelemPlaceholder     = "Chelem",
    chelemPlayerLabel     = "Qui a annoncé le chelem ?",
    chelemPlaysFirst      = { name -> "$name joue en premier ce tour." },
    noneOption            = "Personne",
    petit                 = "Petit",
    poignee               = "Poignée",
    doublePoignee         = "Dbl poignée",
    triplePoignee         = "Trp poignée",
    petitTooltipBody      = "Le Petit est joué au dernier pli.\n+10 pts × multiplicateur du contrat.",
    poigneeTooltipBody    = "10 atouts déclarés avant le jeu.\nBonus : 20 pts par joueur.",
    doublePoigneeTooltipBody = "13 atouts déclarés avant le jeu.\nBonus : 30 pts par joueur.",
    triplePoigneeTooltipBody = "15 atouts déclarés avant le jeu.\nBonus : 40 pts par joueur.",
    chelemTooltipBody     = "Tous les plis remportés par la même équipe.\n\nAnnoncé et réalisé : +400 pts\nNon annoncé, réalisé : +200 pts\nAnnoncé, non réalisé : −200 pts",
    pointsRange           = "0 – 91",
    pointsOutOfRange      = "Doit être entre 0 et 91",
    skipRoundConfirmTitle = "Passer ce tour ?",
    skipRoundConfirmBody  = "Aucun contrat ne sera enregistré pour ce tour.",
    endGameConfirmTitle   = "Terminer la partie ?",
    endGameConfirmBody    = "Le tour en cours ne sera pas sauvegardé.",
    cancel                = "Annuler",
    skipped               = "Passée",
    roundHistoryPrefix    = { n, taker -> "Manche $n : $taker — " },
    boutsPointsDetail     = { bouts, points -> " · $bouts bouts · $points pts" },
    wonOutcome            = { s -> " — Gagné$s" },
    lostOutcome           = { s -> " — Perdu$s" },

    backToGame            = "Retour à la partie",
    gameOver              = "Fin de partie",
    winner                = "Gagnant·e",
    scoreDisplay          = { sign, score -> "$sign$score pts" },
    itsATie               = "Égalité !",
    newGame               = "Nouvelle partie",

    scoreHistory          = "Historique des scores",
    roundColumn           = "Manche",

    chelemNone               = "Aucun",
    chelemAnnouncedRealized  = "Annoncé et réalisé",
    chelemAnnouncedNotRealized = "Annoncé, non réalisé",
    chelemNotAnnouncedRealized = "Non annoncé, réalisé",
)

// Returns the AppStrings for the given locale.
fun appStrings(locale: AppLocale): AppStrings = when (locale) {
    AppLocale.EN -> EnStrings
    AppLocale.FR -> FrStrings
}

// ── Enum localization extensions ──────────────────────────────────────────────

// Returns the localized display name for a Contract.
// The four Tarot contract names (Prise, Garde, Garde Sans, Garde Contre) are French
// terms used internationally — they are the same in both English and French.
@Suppress("UNUSED_PARAMETER")
fun Contract.localizedName(locale: AppLocale): String = displayName

// Returns the localized display name for a Chelem outcome.
// Chelem options have meaningful translations (None/Aucun, Announced/Annoncé, etc.).
fun Chelem.localizedName(locale: AppLocale): String = when (this) {
    Chelem.NONE                   -> appStrings(locale).chelemNone
    Chelem.ANNOUNCED_REALIZED     -> appStrings(locale).chelemAnnouncedRealized
    Chelem.ANNOUNCED_NOT_REALIZED -> appStrings(locale).chelemAnnouncedNotRealized
    Chelem.NOT_ANNOUNCED_REALIZED -> appStrings(locale).chelemNotAnnouncedRealized
}
