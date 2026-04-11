package fr.mandarine.tarotcounter

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
    // `formattedScore` is already sign-prefixed (e.g. "+120") via Int.withSign().
    val winnerResult: (name: String, formattedScore: String) -> String,
    // Tie summary in the past-game card, e.g. "Tie: Alice & Bob".
    val tieResult: (names: String) -> String,
    // "1 round" vs "N rounds" shown in the past-game card footer.
    val roundCount: (count: Int) -> String,

    // ── Game Screen ───────────────────────────────────────────────────────────
    // "Round 1" / "Manche 1" header.
    val roundHeader: (n: Int) -> String,
    // "Dealer: Alice" label shown above the attacker selector for context.
    val dealerLabel: (dealer: String) -> String,
    // Label for the attacker-selector row, e.g. "Attacker" / "Preneur".
    val attackerLabel: String,
    // "{Attacker} — choose a contract:" prompt above the contract chips.
    // Only shown once an attacker has been selected.
    val chooseContract: (taker: String) -> String,
    val skipRound: String,
    val numberOfBouts: String,
    // Text field label shown when the user is entering the attacker (taker)'s points.
    // Also used as the content description of the trailing toggle icon when in defender mode
    // (tapping it switches back to attacker mode).
    val attackerPointsLabel: String,
    // Text field label shown when the user is entering the defenders' points.
    // Also used as the content description of the trailing toggle icon when in attacker mode
    // (tapping it switches to defender mode).
    val defenderPointsLabel: String,
    val partnerCalledByTaker: String,
    val confirmRound: String,
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
    // Poignée tooltip bodies are lambdas so the trump threshold adapts to the player count.
    // The official FFT rules specify different thresholds: 8/10/13 for 5 players,
    // 10/13/15 for 4 players, and 13/15/18 for 3 players.
    val poigneeTooltipBody: (playerCount: Int) -> String,
    val doublePoigneeTooltipBody: (playerCount: Int) -> String,
    val triplePoigneeTooltipBody: (playerCount: Int) -> String,
    val chelemTooltipBody: String,
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
    // `formattedScore` is already sign-prefixed (e.g. "+42") via Int.withSign().
    val scoreDisplay: (formattedScore: String) -> String,
    val itsATie: String,
    val newGame: String,

    // ── Score History Screen ──────────────────────────────────────────────────
    val scoreHistory: String,
    // "Round" / "Manche" column header in the score table.
    val roundColumn: String,

    // ── Back-navigation confirmation dialog (Final Score screen) ──────────────
    // Shown when the user presses the system back button on the Final Score screen.
    // The body warns that leaving will discard unsaved results.
    val backConfirmTitle: String,
    val backConfirmBody: String,
    // Confirm action label — distinct from `cancel` which already exists.
    val backConfirmLeave: String,

    // ── Feedback button (Landing Screen) ─────────────────────────────────────
    // Label for the button that opens the user's email client to contact the developer.
    val feedbackButton: String,

    // ── Chelem enum labels ────────────────────────────────────────────────────
    val chelemNone: String,
    val chelemAnnouncedRealized: String,
    val chelemAnnouncedNotRealized: String,
    val chelemNotAnnouncedRealized: String,
    // Label for the rare defender-chelem case (R-RO201206.pdf p.6):
    // defenders won every trick without announcing it.
    val chelemDefendersRealized: String,

    // ── Contract enum labels ──────────────────────────────────────────────────
    // In French (and internationally) these are always the French Tarot terms.
    // In English, plain translations are provided for accessibility.
    val contractPrise: String,
    val contractGarde: String,
    val contractGardeSans: String,
    val contractGardeContre: String,
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
    winnerResult          = { name, formattedScore -> "Winner: $name ($formattedScore)" },
    tieResult             = { names -> "Tie: $names" },
    roundCount            = { n -> if (n == 1) "1 round" else "$n rounds" },

    roundHeader           = { n -> "Round $n" },
    dealerLabel           = { dealer -> "Dealer: $dealer" },
    attackerLabel         = "Attacker",
    chooseContract        = { taker -> "$taker — choose a contract:" },
    skipRound             = "Skip round",
    numberOfBouts         = "Number of bouts (oudlers)",
    // Label shown on the points field and used as the toggle icon's content description.
    // The range (0-91) is always included so users know the valid input bounds at a glance.
    attackerPointsLabel   = "Attacker's pts (0-91)",
    defenderPointsLabel   = "Defenders' pts (0-91)",
    partnerCalledByTaker  = "Partner (called by taker)",
    confirmRound          = "Confirm round",
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
    doublePoignee         = "Double poignée",
    triplePoignee         = "Triple poignée",
    petitTooltipBody      = "The Petit (1 of trumps) is played in the last trick.\n+10 pts × contract multiplier.",
    // Use poigneeThresholds() to get the correct trump count for the current player count.
    poigneeTooltipBody    = { n -> "${poigneeThresholds(n).first} trumps shown before play.\nBonus: 20 pts per player." },
    doublePoigneeTooltipBody = { n -> "${poigneeThresholds(n).second} trumps shown before play.\nBonus: 30 pts per player." },
    triplePoigneeTooltipBody = { n -> "${poigneeThresholds(n).third} trumps shown before play.\nBonus: 40 pts per player." },
    chelemTooltipBody     = "All tricks won by the same team.\n\nAnnounced & realized: +400 pts\nNot announced, realized: +200 pts\nAnnounced, not realized: −200 pts\nDefenders realized: −200 pts (taker pays each defender)",
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
    scoreDisplay          = { formattedScore -> "$formattedScore pts" },
    itsATie               = "It's a tie!",
    newGame               = "New Game",

    scoreHistory          = "Score history",
    roundColumn           = "Round",

    backConfirmTitle      = "Leave the game?",
    backConfirmBody       = "The game results won't be saved if you leave now.",
    backConfirmLeave      = "Leave",

    chelemNone               = "None",
    chelemAnnouncedRealized  = "Announced & realized",
    chelemAnnouncedNotRealized = "Announced, not realized",
    chelemNotAnnouncedRealized = "Not announced, realized",
    chelemDefendersRealized  = "Defenders realized",

    // English translations for the four contract levels.
    contractPrise        = "Small",
    contractGarde        = "Guard",
    contractGardeSans    = "Guard Without",
    contractGardeContre  = "Guard Against",

    feedbackButton           = "Send Feedback",
)

// ── French strings ────────────────────────────────────────────────────────────

val FrStrings = AppStrings(
    appTitle              = "Tarot",
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
    winnerResult          = { name, formattedScore -> "Gagnant : $name ($formattedScore)" },
    tieResult             = { names -> "Égalité : $names" },
    roundCount            = { n -> if (n == 1) "1 manche" else "$n manches" },

    roundHeader           = { n -> "Manche $n" },
    dealerLabel           = { dealer -> "Distributeur : $dealer" },
    attackerLabel         = "Preneur",
    chooseContract        = { taker -> "$taker — choisissez un contrat :" },
    skipRound             = "Passer la manche",
    numberOfBouts         = "Nombre de bouts (oudlers)",
    attackerPointsLabel   = "Pts attaquant (0-91)",
    defenderPointsLabel   = "Pts défenseurs (0-91)",
    partnerCalledByTaker  = "Appelé (par le preneur)",
    confirmRound          = "Valider la manche",
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
    doublePoignee         = "Double poignée",
    triplePoignee         = "Triple poignée",
    petitTooltipBody      = "Le Petit est joué au dernier pli.\n+10 pts × multiplicateur du contrat.",
    // Use poigneeThresholds() pour obtenir le bon seuil d'atouts selon le nombre de joueurs.
    poigneeTooltipBody    = { n -> "${poigneeThresholds(n).first} atouts déclarés avant le jeu.\nBonus : 20 pts par joueur." },
    doublePoigneeTooltipBody = { n -> "${poigneeThresholds(n).second} atouts déclarés avant le jeu.\nBonus : 30 pts par joueur." },
    triplePoigneeTooltipBody = { n -> "${poigneeThresholds(n).third} atouts déclarés avant le jeu.\nBonus : 40 pts par joueur." },
    chelemTooltipBody     = "Tous les plis remportés par la même équipe.\n\nAnnoncé et réalisé : +400 pts\nNon annoncé, réalisé : +200 pts\nAnnoncé, non réalisé : −200 pts\nDéfense réalise : −200 pts (le preneur paye chaque défenseur)",
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
    scoreDisplay          = { formattedScore -> "$formattedScore pts" },
    itsATie               = "Égalité !",
    newGame               = "Nouvelle partie",

    scoreHistory          = "Historique des scores",
    roundColumn           = "Manche",

    backConfirmTitle      = "Quitter la partie ?",
    backConfirmBody       = "Les résultats ne seront pas sauvegardés si vous quittez maintenant.",
    backConfirmLeave      = "Quitter",

    chelemNone               = "Aucun",
    chelemAnnouncedRealized  = "Annoncé et réalisé",
    chelemAnnouncedNotRealized = "Annoncé, non réalisé",
    chelemNotAnnouncedRealized = "Non annoncé, réalisé",
    chelemDefendersRealized  = "Défense réalise",

    // French contract names — the canonical French Tarot terminology.
    contractPrise        = "Prise",
    contractGarde        = "Garde",
    contractGardeSans    = "Garde Sans",
    contractGardeContre  = "Garde Contre",

    feedbackButton           = "Contacter le développeur",
)

// Returns the AppStrings for the given locale.
fun appStrings(locale: AppLocale): AppStrings = when (locale) {
    AppLocale.EN -> EnStrings
    AppLocale.FR -> FrStrings
}

// ── Enum localization extensions ──────────────────────────────────────────────

// Returns the localized display name for a Contract.
// French keeps the canonical Tarot terms; English provides plain translations
// so users unfamiliar with French Tarot can understand each contract level.
fun Contract.localizedName(locale: AppLocale): String {
    val strings = appStrings(locale)
    return when (this) {
        Contract.PRISE        -> strings.contractPrise
        Contract.GARDE        -> strings.contractGarde
        Contract.GARDE_SANS   -> strings.contractGardeSans
        Contract.GARDE_CONTRE -> strings.contractGardeContre
    }
}

// Returns the localized display name for a Chelem outcome.
// Chelem options have meaningful translations (None/Aucun, Announced/Annoncé, etc.).
fun Chelem.localizedName(locale: AppLocale): String = when (this) {
    Chelem.NONE                   -> appStrings(locale).chelemNone
    Chelem.ANNOUNCED_REALIZED     -> appStrings(locale).chelemAnnouncedRealized
    Chelem.ANNOUNCED_NOT_REALIZED -> appStrings(locale).chelemAnnouncedNotRealized
    Chelem.NOT_ANNOUNCED_REALIZED -> appStrings(locale).chelemNotAnnouncedRealized
    // Rare case from R-RO201206.pdf p.6: defenders won every trick without announcing it.
    Chelem.DEFENDERS_REALIZED     -> appStrings(locale).chelemDefendersRealized
}
