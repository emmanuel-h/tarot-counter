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
    // Section header for the first-dealer selection row on the setup screen.
    val dealerSelectionLabel: String,
    // Label for the "random dealer" option in the dealer selection toggle.
    val randomDealer: String,
    // Label for the "choose a specific player" option in the dealer selection toggle.
    val chooseDealer: String,
    val pastGames: String,
    val resume: String,
    val noRoundsPlayed: String,
    // Tie summary in the past-game card, e.g. "Tie: Alice & Bob".
    val tieResult: (names: String) -> String,
    // "1 round" vs "N rounds" shown in the past-game card footer.
    val roundCount: (count: Int) -> String,
    // Short label next to the 3 | 4 | 5 player-count control.
    val playersLabel: String,
    // Brass overline on the felt "resume" card, shown in upper case.
    val gameInProgress: String,
    // "4 players" — player count in the resume card and past-games rows.
    val playerCount: (count: Int) -> String,
    // "Alice leads +312" / "Alice & Bob lead +20" — `names` has 2+ entries on a tie.
    val leaderLine: (names: List<String>, formattedScore: String) -> String,

    // ── Game Screen ───────────────────────────────────────────────────────────
    // "Round 1" / "Manche 1" header.
    val roundHeader: (n: Int) -> String,
    // "Dealer: Alice" label shown above the attacker selector for context.
    val dealerLabel: (dealer: String) -> String,
    val skipRound: String,
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
    // ── Salon game screen (issue #198) ──
    // Heading of the ranked standings card (also its accessibility label).
    val standings: String,
    // Brass label on the leader's row, shown in upper case.
    val leading: String,
    // Heading above the taker tiles, and its helper text on the right.
    val whoTook: String,
    val tapTheTaker: String,
    // Heading of the mini round log, and the link that opens the history screen.
    val lastRounds: String,
    val seeAll: String,
    // Short round badge in the mini log: "R4" / "M4".
    val roundBadge: (n: Int) -> String,
    // "Won" / "Lost" in the mini log line "Bruno · Garde · Lost".
    val wonShort: String,
    val lostShort: String,
    // Title of the round-entry view, e.g. "Chloé takes".
    val takerTakes: (taker: String) -> String,
    // Accessibility label of the back arrow in the round-entry view.
    val changeTaker: String,
    // ── Round entry (issue #199) ──
    // Small label above the contract cards.
    val contractLabel: String,
    // Label above the 0–3 bout chips, and the helper on the right: "needs 41".
    val boutsLabel: String,
    val boutsNeeds: (required: Int) -> String,
    // Label of the points card, and the two segments of its camp toggle.
    val pointsScored: String,
    val attackCamp: String,
    val defenseCamp: String,
    // Live result pill: "Made by 6 → Chloé +186" / "Short by 4 → Chloé -174".
    // `formattedScore` is already sign-prefixed.
    val resultMade: (margin: Int, taker: String, formattedScore: String) -> String,
    val resultShort: (margin: Int, taker: String, formattedScore: String) -> String,
    // ── Bonus rows + bottom sheets (issue #200) ──
    // Title of the bonus rows block.
    val bonusesLabel: String,
    // Names of the three poignée levels, used on the segmented control in the sheet.
    val poigneeSimple: String,
    val poigneeDouble: String,
    val poigneeTriple: String,
    // One-line explanation at the top of the poignée sheet; thresholds depend on
    // the player count (poigneeThresholds).
    val poigneeExplain: (playerCount: Int) -> String,
    // Label above the chelem outcomes in the chelem sheet.
    val chelemOutcomeLabel: String,
    // Button closing a bonus sheet.
    val done: String,
    // ── Game over (issue #201) ──
    // Heading of the line chart, and its screen-reader description.
    val scoreOverTime: String,
    val scoreChartDescription: (rounds: Int) -> String,
    // Link under the ranking that opens the round-by-round table.
    val seeAllRounds: String,
    // ── Score history (issue #202) ──
    // Empty state shown by both history views before the first round.
    val historyEmpty: String,
    // Detail line of a round card: "2 bouts · 47 pts".
    val boutsPoints: (bouts: Int, points: Int) -> String,
    // ── Settings + Rules restyle (issue #203) ──
    // Group headings of the settings page.
    val appearanceLabel: String,
    val helpLabel: String,
    val aboutLabel: String,
    // Text labels of the theme toggle (no emoji-only labels).
    val themeLight: String,
    val themeDark: String,
    // "Version" row of the About group.
    val versionLabel: String,
    // Column headings of the two rules tables.
    val rulesBoutsColumn: String,
    val rulesNeededColumn: String,
    val rulesContractColumn: String,
    val rulesMultiplierColumn: String,
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
    // Bonus row labels — French Tarot terms, unchanged in both languages.
    val petit: String,
    val poignee: String,
    // Explanation lines shown at the top of the Petit and Chelem bottom sheets.
    val petitTooltipBody: String,
    val chelemTooltipBody: String,
    // Error shown below the points text field when the entered value exceeds 91.
    val pointsOutOfRange: String,
    // Error shown below the bonus grid when the total declared atouts across all
    // poignée declarations exceed the 22 trumps that exist in the deck.
    // `total` is the sum of minimum trump thresholds; `max` is TOTAL_ATOUTS_IN_DECK.
    val atoutCountError: (total: Int, max: Int) -> String,
    // Accessibility label for the "undo previous round" icon button in the game header.
    val undoPreviousRound: String,
    // Confirmation dialog for "Undo previous round".
    val undoConfirmTitle: String,
    val undoConfirmBody: String,
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

    // ── Final Score Screen ────────────────────────────────────────────────────
    val mainMenu: String,
    val backToGame: String,

    // ── Shared components ─────────────────────────────────────────────────────
    // Screen-reader description of a player's avatar circle, e.g. "Player Alice".
    val playerAvatar: (name: String) -> String,
    val gameOver: String,
    val winner: String,
    val itsATie: String,
    val newGame: String,

    // ── Score History Screen ──────────────────────────────────────────────────
    val scoreHistory: String,
    // "Round" / "Manche" column header in the score table.
    val roundColumn: String,
    // Labels for the two-mode view toggle on the history screen.
    // TABLE = the cumulative score table; LIST = the round-by-round detail list.
    val historyViewTable: String,
    val historyViewList: String,

    // ── Back-navigation confirmation dialog (Final Score screen) ──────────────
    // Shown when the user presses the system back button on the Final Score screen.
    // The body warns that leaving will discard unsaved results.
    val backConfirmTitle: String,
    val backConfirmBody: String,
    // Confirm action label — distinct from `cancel` which already exists.
    val backConfirmLeave: String,

    // ── Settings ──────────────────────────────────────────────────────────────
    // Accessibility label for the gear icon button on the landing screen.
    val settings: String,
    // Title shown at the top of the settings page.
    val settingsTitle: String,
    // Section heading for the theme toggle row on the settings page.
    val themeLabel: String,
    // Section heading for the language toggle row on the settings page.
    val languageLabel: String,

    // ── Feedback button (Settings Screen) ────────────────────────────────────
    // Label for the button that opens the user's email client to contact the developer.
    val feedbackButton: String,

    // ── Rules dialog (Settings Screen) ────────────────────────────────────────
    // Label for the button that opens the rules dialog from the settings page.
    val rulesButton: String,
    // Title shown at the top of the rules dialog.
    val rulesTitle: String,
    // Label for the dismiss button at the bottom of the rules dialog.
    val rulesClose: String,
    // Section: how many points are needed to win based on bout count.
    val rulesObjectiveTitle: String,
    val rulesObjectiveBody: String,
    // Section: the four contracts and their score multipliers.
    val rulesContractsTitle: String,
    val rulesContractsBody: String,
    // Section: the formula that converts bouts + points + contract into a score.
    val rulesScoreFormulaTitle: String,
    val rulesScoreFormulaBody: String,
    // Section: how the round score is split between the taker, partner, and defenders.
    val rulesDistributionTitle: String,
    val rulesDistributionBody: String,
    // Section: petit au bout, poignée, and chelem bonuses.
    val rulesBonusTitle: String,
    val rulesBonusBody: String,

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
    dealerSelectionLabel  = "First Dealer",
    randomDealer          = "Random",
    chooseDealer          = "Choose",
    pastGames             = "Past Games",
    resume                = "Resume",
    noRoundsPlayed        = "No rounds played",
    tieResult             = { names -> "Tie: $names" },
    roundCount            = { n -> if (n == 1) "1 round" else "$n rounds" },
    playersLabel          = "Players",
    gameInProgress        = "Game in progress",
    playerCount           = { n -> "$n players" },
    leaderLine            = { names, score ->
        if (names.size == 1) "${names.first()} leads $score"
        else "${names.joinToString(" & ")} lead $score"
    },

    roundHeader           = { n -> "Round $n" },
    dealerLabel           = { dealer -> "Dealer: $dealer" },
    skipRound             = "Skip round",
    // Floating label on the points field; short enough to fit on one line in a
    // half-width field that also has a trailing toggle icon.
    attackerPointsLabel   = "Attacker (0-91)",
    defenderPointsLabel   = "Defenders (0-91)",
    partnerCalledByTaker  = "Partner (called by taker)",
    confirmRound          = "Confirm round",
    scores                = "Scores",
    standings             = "Standings",
    leading               = "Leading",
    whoTook               = "Who took?",
    tapTheTaker           = "Tap the taker",
    lastRounds            = "Last rounds",
    seeAll                = "See all",
    roundBadge            = { n -> "R$n" },
    wonShort              = "Won",
    lostShort             = "Lost",
    takerTakes            = { taker -> "$taker takes" },
    changeTaker           = "Change taker",
    contractLabel         = "Contract",
    boutsLabel            = "Bouts (oudlers)",
    boutsNeeds            = { n -> "needs $n" },
    pointsScored          = "Points scored",
    attackCamp            = "Attack",
    defenseCamp           = "Defense",
    resultMade            = { m, taker, score -> "Made by $m → $taker $score" },
    resultShort           = { m, taker, score -> "Short by $m → $taker $score" },
    bonusesLabel          = "Bonuses",
    poigneeSimple         = "Simple",
    poigneeDouble         = "Double",
    poigneeTriple         = "Triple",
    poigneeExplain        = { n ->
        val (s, d, t) = poigneeThresholds(n)
        "Trumps shown before play: simple $s, double $d, triple $t. " +
            "20 / 30 / 40 pts per player go to the winning camp."
    },
    chelemOutcomeLabel    = "Outcome",
    done                  = "Done",
    scoreOverTime         = "Score over time",
    scoreChartDescription = { n -> "Line chart of every player's cumulative score over $n rounds" },
    seeAllRounds          = "See all rounds",
    historyEmpty          = "No rounds played yet. Scores appear here after the first round.",
    boutsPoints           = { b, p -> "$b bouts · $p pts" },
    appearanceLabel       = "Appearance",
    helpLabel             = "Help",
    aboutLabel            = "About",
    themeLight            = "Light",
    themeDark             = "Dark",
    versionLabel          = "Version",
    rulesBoutsColumn      = "Bouts",
    rulesNeededColumn     = "Points needed",
    rulesContractColumn   = "Contract",
    rulesMultiplierColumn = "Multiplier",
    history               = "History",
    endGame               = "End Game",
    chelemLabel           = "Chelem (grand slam)",
    chelemPlaceholder     = "Chelem",
    chelemPlayerLabel     = "Who called the chelem?",
    chelemPlaysFirst      = { name -> "$name plays first this round." },
    noneOption            = "None",
    petit                 = "Petit au bout",
    poignee               = "Poignée",
    petitTooltipBody      = "The Petit (1 of trumps) is played in the last trick.\n+10 pts × contract multiplier.",
    // Use poigneeThresholds() to get the correct trump count for the current player count.
    chelemTooltipBody     = "All tricks won by the same team.\n\nAnnounced & realized: +400 pts\nNot announced, realized: +200 pts\nAnnounced, not realized: −200 pts\nDefenders realized: −200 pts (taker pays each defender)",
    pointsOutOfRange      = "Must be between 0 and 91",
    atoutCountError       = { total, max -> "Too many trumps declared ($total / $max). Reduce your declarations." },
    undoPreviousRound     = "Undo previous round",
    undoConfirmTitle      = "Undo previous round?",
    undoConfirmBody       = "Are you sure to go back and modify the last round?",
    skipRoundConfirmTitle = "Skip this round?",
    skipRoundConfirmBody  = "No contract will be recorded for this round.",
    endGameConfirmTitle   = "End the game?",
    endGameConfirmBody    = "The current round will not be saved.",
    cancel                = "Cancel",
    skipped               = "Skipped",

    mainMenu              = "Main Menu",
    backToGame            = "Back to game",
    playerAvatar          = { name -> "Player $name" },
    gameOver              = "Game Over",
    winner                = "Winner",
    itsATie               = "It's a tie!",
    newGame               = "New Game",

    scoreHistory          = "Score history",
    roundColumn           = "Round",
    historyViewTable      = "Table",
    historyViewList       = "List",

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

    settings                 = "Settings",
    settingsTitle            = "Settings",
    themeLabel               = "Theme",
    languageLabel            = "Language",
    feedbackButton           = "Send Feedback",

    rulesButton              = "Rules",
    rulesTitle               = "Game Rules",
    rulesClose               = "Close",

    rulesObjectiveTitle      = "Objective",
    rulesObjectiveBody       = "Win the round by reaching the points required by the number of bouts (oudlers) in your tricks. Bouts are the 21 of trumps, the Petit (1 of trumps) and the Excuse.",

    rulesContractsTitle      = "Contracts",
    rulesContractsBody       = "The taker announces a contract that multiplies every score of the round:",

    rulesScoreFormulaTitle   = "Score Formula",
    rulesScoreFormulaBody    = "(25 + |actual − required|) × contract multiplier\n\nThe taker wins if their points ≥ the required threshold.\nOn a win the taker collects from defenders; on a loss the taker pays each defender.",

    rulesDistributionTitle   = "Score Distribution",
    rulesDistributionBody    = "3 or 4 players — no partner:\n• Taker: ±(players − 1) × score\n• Each defender: ∓score\n\n5 players — with called partner:\n• Taker: ±2 × score\n• Partner: ±1 × score\n• Each defender: ∓score\n\nEvery round is zero-sum.",

    rulesBonusTitle          = "Bonuses",
    rulesBonusBody           = "Petit au bout — Petit (1 of trumps) won on the last trick:\n+10 × multiplier to the achieving camp.\n\nPoignée — trumps shown before play:\n• Simple: 20 pts per player\n• Double: 30 pts per player\n• Triple: 40 pts per player\nBonus always goes to the winning camp.\n\nChelem — all tricks won by the same team:\n• Announced & realized: +400 pts\n• Not announced, realized: +200 pts\n• Announced, not realized: −200 pts\n• Defenders realized: −200 pts (taker pays each defender)",
)

// ── French strings ────────────────────────────────────────────────────────────

val FrStrings = AppStrings(
    appTitle              = "Tarot",
    numberOfPlayers       = "Nombre de joueurs",
    playerNamesLabel      = "Noms des joueurs",
    playerFallback        = { i -> "Joueur $i" },
    nameAlreadyUsed       = "Nom déjà utilisé",
    startGame             = "Démarrer",
    dealerSelectionLabel  = "Premier donneur",
    randomDealer          = "Aléatoire",
    chooseDealer          = "Choisir",
    pastGames             = "Parties précédentes",
    resume                = "Reprendre",
    noRoundsPlayed        = "Aucune manche jouée",
    tieResult             = { names -> "Égalité : $names" },
    roundCount            = { n -> if (n == 1) "1 manche" else "$n manches" },
    playersLabel          = "Joueurs",
    gameInProgress        = "Partie en cours",
    playerCount           = { n -> "$n joueurs" },
    leaderLine            = { names, score ->
        if (names.size == 1) "${names.first()} mène avec $score"
        else "${names.joinToString(" & ")} mènent avec $score"
    },

    roundHeader           = { n -> "Manche $n" },
    dealerLabel           = { dealer -> "Donneur : $dealer" },
    skipRound             = "Passer",
    attackerPointsLabel   = "Attaquant (0-91)",
    defenderPointsLabel   = "Défenseurs (0-91)",
    partnerCalledByTaker  = "Appelé (par le preneur)",
    confirmRound          = "Valider",
    scores                = "Scores",
    standings             = "Classement",
    leading               = "En tête",
    whoTook               = "Qui prend ?",
    tapTheTaker           = "Touchez le preneur",
    lastRounds            = "Dernières manches",
    seeAll                = "Tout voir",
    roundBadge            = { n -> "M$n" },
    wonShort              = "Gagnée",
    lostShort             = "Perdue",
    takerTakes            = { taker -> "$taker prend" },
    changeTaker           = "Changer de preneur",
    contractLabel         = "Contrat",
    boutsLabel            = "Bouts",
    boutsNeeds            = { n -> "il faut $n" },
    pointsScored          = "Points réalisés",
    attackCamp            = "Attaque",
    defenseCamp           = "Défense",
    resultMade            = { m, taker, score -> "Fait de $m → $taker $score" },
    resultShort           = { m, taker, score -> "Chuté de $m → $taker $score" },
    bonusesLabel          = "Bonus",
    poigneeSimple         = "Simple",
    poigneeDouble         = "Double",
    poigneeTriple         = "Triple",
    poigneeExplain        = { n ->
        val (s, d, t) = poigneeThresholds(n)
        "Atouts montrés avant le jeu : simple $s, double $d, triple $t. " +
            "20 / 30 / 40 pts par joueur vont au camp gagnant."
    },
    chelemOutcomeLabel    = "Résultat",
    done                  = "OK",
    scoreOverTime         = "Évolution des scores",
    scoreChartDescription = { n -> "Courbe du score cumulé de chaque joueur sur $n manches" },
    seeAllRounds          = "Voir toutes les manches",
    historyEmpty          = "Aucune manche jouée. Les scores s'afficheront après la première manche.",
    boutsPoints           = { b, p -> "$b bouts · $p pts" },
    appearanceLabel       = "Apparence",
    helpLabel             = "Aide",
    aboutLabel            = "À propos",
    themeLight            = "Clair",
    themeDark             = "Sombre",
    versionLabel          = "Version",
    rulesBoutsColumn      = "Bouts",
    rulesNeededColumn     = "Points à faire",
    rulesContractColumn   = "Contrat",
    rulesMultiplierColumn = "Multiplicateur",
    history               = "Historique",
    endGame               = "Fin de partie",
    chelemLabel           = "Chelem (grand chelem)",
    chelemPlaceholder     = "Chelem",
    chelemPlayerLabel     = "Qui a annoncé le chelem ?",
    chelemPlaysFirst      = { name -> "$name joue en premier ce tour." },
    noneOption            = "Personne",
    petit                 = "Petit au bout",
    poignee               = "Poignée",
    petitTooltipBody      = "Le Petit est joué au dernier pli.\n+10 pts × multiplicateur du contrat.",
    // Use poigneeThresholds() pour obtenir le bon seuil d'atouts selon le nombre de joueurs.
    chelemTooltipBody     = "Tous les plis remportés par la même équipe.\n\nAnnoncé et réalisé : +400 pts\nNon annoncé, réalisé : +200 pts\nAnnoncé, non réalisé : −200 pts\nDéfense réalise : −200 pts (le preneur paye chaque défenseur)",
    pointsOutOfRange      = "Doit être entre 0 et 91",
    atoutCountError       = { total, max -> "Trop d'atouts déclarés ($total / $max). Réduisez vos déclarations." },
    undoPreviousRound     = "Revenir à la manche précédente",
    undoConfirmTitle      = "Revenir à la manche précédente ?",
    undoConfirmBody       = "Êtes-vous sûr de vouloir modifier les données de la manche précédente ?.",
    skipRoundConfirmTitle = "Passer ce tour ?",
    skipRoundConfirmBody  = "Aucun contrat ne sera enregistré pour ce tour.",
    endGameConfirmTitle   = "Terminer la partie ?",
    endGameConfirmBody    = "Le tour en cours ne sera pas sauvegardé.",
    cancel                = "Annuler",
    skipped               = "Passée",

    mainMenu              = "Menu principal",
    backToGame            = "Retour",
    playerAvatar          = { name -> "Joueur $name" },
    gameOver              = "Fin de partie",
    winner                = "Gagnant·e",
    itsATie               = "Égalité !",
    newGame               = "Nouvelle partie",

    scoreHistory          = "Historique des scores",
    roundColumn           = "Manche",
    historyViewTable      = "Tableau",
    historyViewList       = "Liste",

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

    settings                 = "Paramètres",
    settingsTitle            = "Paramètres",
    themeLabel               = "Thème",
    languageLabel            = "Langue",
    feedbackButton           = "Contacter le développeur",

    rulesButton              = "Règles",
    rulesTitle               = "Règles du jeu",
    rulesClose               = "Fermer",

    rulesObjectiveTitle      = "Objectif",
    rulesObjectiveBody       = "Gagner la manche en atteignant le seuil de points fixé par le nombre de bouts dans vos plis. Les bouts sont le 21 d'atout, le Petit (1 d'atout) et l'Excuse.",

    rulesContractsTitle      = "Contrats",
    rulesContractsBody       = "Le preneur annonce un contrat qui multiplie tous les scores de la manche :",

    rulesScoreFormulaTitle   = "Calcul du score",
    rulesScoreFormulaBody    = "(25 + |points réels − seuil|) × multiplicateur du contrat\n\nLe preneur gagne si ses points sont supérieurs au seuil requis.\nEn cas de victoire il encaisse ; en cas de défaite il paye chaque défenseur.",

    rulesDistributionTitle   = "Répartition des scores",
    rulesDistributionBody    = "3 ou 4 joueurs — sans appelé :\n• Preneur : (joueurs − 1) × score\n• Chaque défenseur : score\n\n5 joueurs — avec appelé :\n• Preneur : 2 × score\n• Appelé : score\n• Chaque défenseur : score\n\nChaque manche est à somme nulle.",

    rulesBonusTitle          = "Bonus",
    rulesBonusBody           = "Petit au bout — le Petit (1 d'atout) remporté au dernier pli :\n+10 × multiplicateur pour le camp qui le réalise.\n\nPoignée — atouts déclarés avant le jeu :\n• Simple : 20 pts par joueur\n• Double : 30 pts par joueur\n• Triple : 40 pts par joueur\nLe bonus va toujours au camp gagnant.\n\nChelem — tous les plis remportés par la même équipe :\n• Annoncé et réalisé : +400 pts\n• Non annoncé, réalisé : +200 pts\n• Annoncé, non réalisé : −200 pts\n• Défense réalise : −200 pts (le preneur paye chaque défenseur)",
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
