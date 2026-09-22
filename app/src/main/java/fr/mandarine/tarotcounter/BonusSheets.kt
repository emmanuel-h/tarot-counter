package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mandarine.tarotcounter.ui.theme.Dimens

// ─────────────────────────────────────────────────────────────────────────────
// Bonus rows + bottom sheets (Salon, issue #200).
//
// Replaces the old checkbox grid (up to 4 rows × 5 players = 20 checkboxes) with
// three calm rows that each show their current value:
//
//   Bonuses
//   ┌──────────────────────────────────┐
//   │ Petit au bout          None    › │
//   │ Poignée           (A) Alice    › │   red when trumps > 22
//   │ Chelem                 None    › │
//   └──────────────────────────────────┘
//
// Tapping a row opens a modal bottom sheet (a panel that slides up from the
// bottom) where the value is picked. Pure logic lives in Bonuses.kt.
// ─────────────────────────────────────────────────────────────────────────────

// Which sheet is open. `null` (in the state below) means no sheet.
private enum class BonusSheet { PETIT, POIGNEE, CHELEM }

/**
 * The bonus rows of the round entry and the three sheets behind them.
 *
 * @param playerNames  Players in seat order (drives avatar colours).
 * @param taker        The round's taker (chelem candidate).
 * @param partner      The called partner in a 5-player game, else null.
 * @param petitAuBout  Player who won the Petit on the last trick, or null.
 * @param poignees     Every poignée declared this round.
 * @param atoutError   True when the declared poignées need more than 22 trumps.
 * @param atoutErrorText Message explaining [atoutError].
 * @param chelem       Chelem outcome; [Chelem.NONE] by default.
 * @param chelemPlayer Player who called / achieved the chelem.
 */
@Composable
fun BonusesSection(
    playerNames: List<String>,
    taker: String,
    partner: String?,
    petitAuBout: String?,
    onPetitAuBout: (String?) -> Unit,
    poignees: PoigneeDeclarations,
    onPoignees: (PoigneeDeclarations) -> Unit,
    atoutError: Boolean,
    atoutErrorText: String,
    chelem: Chelem,
    chelemPlayer: String?,
    onChelem: (Chelem, String?) -> Unit
) {
    val locale  = LocalAppLocale.current
    val strings = appStrings(locale)
    // `remember` keeps the open sheet across recompositions; null = none open.
    var openSheet by remember { mutableStateOf<BonusSheet?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text  = strings.bonusesLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SalonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            Column {
                BonusRowItem(
                    label   = strings.petit,
                    value   = petitAuBout,
                    none    = strings.noneOption,
                    seat    = playerNames.indexOf(petitAuBout),
                    tag     = "bonus_row_petit",
                    onClick = { openSheet = BonusSheet.PETIT }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val declarants = poignees.declarants(playerNames)
                BonusRowItem(
                    label   = strings.poignee,
                    value   = declarants.joinToString(", ").ifEmpty { null },
                    none    = strings.noneOption,
                    // A single declarant gets their avatar; several are listed by name.
                    seat    = if (declarants.size == 1) playerNames.indexOf(declarants.first()) else -1,
                    isError = atoutError,
                    tag     = "bonus_row_poignee",
                    onClick = { openSheet = BonusSheet.POIGNEE }
                )
                if (atoutError) {
                    Text(
                        text     = atoutErrorText,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(horizontal = Dimens.SpaceM)
                            .padding(bottom = Dimens.SpaceS)
                            .testTag("atout_count_error")
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BonusRowItem(
                    label   = strings.chelemPlaceholder,
                    value   = if (chelem == Chelem.NONE) null else chelem.localizedName(locale),
                    none    = strings.noneOption,
                    seat    = -1,
                    tag     = "bonus_row_chelem",
                    onClick = { openSheet = BonusSheet.CHELEM }
                )
            }
        }
    }

    // ── The open sheet, if any ────────────────────────────────────────────────
    when (openSheet) {
        BonusSheet.PETIT -> PetitSheet(
            playerNames = playerNames,
            selected    = petitAuBout,
            strings     = strings,
            onSelect    = { onPetitAuBout(it); openSheet = null },
            onDismiss   = { openSheet = null }
        )
        BonusSheet.POIGNEE -> PoigneeSheet(
            playerNames    = playerNames,
            poignees       = poignees,
            onPoignees     = onPoignees,
            atoutError     = atoutError,
            atoutErrorText = atoutErrorText,
            strings        = strings,
            onDismiss      = { openSheet = null }
        )
        BonusSheet.CHELEM -> ChelemSheet(
            playerNames  = playerNames,
            candidates   = chelemCandidates(taker, partner, playerNames.size),
            chelem       = chelem,
            chelemPlayer = chelemPlayer,
            onChelem     = onChelem,
            strings      = strings,
            locale       = locale,
            onDismiss    = { openSheet = null }
        )
        null -> Unit
    }
}

// One bonus row: label on the left, current value (muted "None" or the value, with
// an avatar when it names one player) and a chevron on the right. 52 dp tall.
@Composable
private fun BonusRowItem(
    label: String,
    value: String?,
    none: String,
    seat: Int,
    tag: String,
    onClick: () -> Unit,
    isError: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            // Role.Button: announced as a button that opens the picker.
            .selectable(selected = false, role = Role.Button, onClick = onClick)
            .padding(horizontal = Dimens.SpaceM)
            .testTag(tag),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null && seat >= 0) {
            PlayerAvatar(name = value, seatIndex = seat, size = AvatarSize.S)
        }
        Text(
            text     = value ?: none,
            style    = MaterialTheme.typography.bodyMedium,
            color    = when {
                isError        -> scheme.error
                value == null  -> scheme.onSurfaceVariant
                else           -> scheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Capped width so a long list of names never pushes the label away.
            modifier = Modifier.widthIn(max = 180.dp)
        )
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null, // the whole row is the button
            tint               = scheme.onSurfaceVariant
        )
    }
}

// Shared frame of every bonus sheet: title, one explanation line, content, Done.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BonusSheetFrame(
    title: String,
    explanation: String,
    doneLabel: String,
    tag: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    // skipPartiallyExpanded: the sheet opens fully instead of stopping half-way.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenMargin)
                .padding(bottom = Dimens.SpaceM)
                .navigationBarsPadding()
                .testTag(tag),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text  = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
            AppButton(text = doneLabel, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

// A tappable choice line with a check mark when selected. `avatarSeat` < 0 = no avatar.
@Composable
private fun ChoiceRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    avatarSeat: Int = -1
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            // Role.RadioButton + selected: screen readers say "selected, 1 of N".
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag(tag),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (avatarSeat >= 0) PlayerAvatar(name = text, seatIndex = avatarSeat, size = AvatarSize.M)
        Text(
            text     = text,
            style    = MaterialTheme.typography.bodyLarge,
            color    = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = null, // selection is carried by the semantics above
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

// Petit au bout: single choice — nobody, or the player who won the Petit on the
// last trick. Picking closes the sheet straight away.
@Composable
private fun PetitSheet(
    playerNames: List<String>,
    selected: String?,
    strings: AppStrings,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    BonusSheetFrame(
        title       = strings.petit,
        explanation = strings.petitTooltipBody.replace('\n', ' '),
        doneLabel   = strings.done,
        tag         = "sheet_petit",
        onDismiss   = onDismiss
    ) {
        Column {
            ChoiceRow(strings.noneOption, selected == null, { onSelect(null) }, "petit_none")
            playerNames.forEachIndexed { seat, name ->
                ChoiceRow(name, selected == name, { onSelect(name) }, "petit_$name", avatarSeat = seat)
            }
        }
    }
}

// Poignée: per player, None / Simple / Double / Triple. Several players may declare.
// The trump-count validation is shown right here, where it can be fixed.
@Composable
private fun PoigneeSheet(
    playerNames: List<String>,
    poignees: PoigneeDeclarations,
    onPoignees: (PoigneeDeclarations) -> Unit,
    atoutError: Boolean,
    atoutErrorText: String,
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    val levels = listOf(
        PoigneeLevel.NONE   to strings.noneOption,
        PoigneeLevel.SIMPLE to strings.poigneeSimple,
        PoigneeLevel.DOUBLE to strings.poigneeDouble,
        PoigneeLevel.TRIPLE to strings.poigneeTriple
    )
    BonusSheetFrame(
        title       = strings.poignee,
        explanation = strings.poigneeExplain(playerNames.size),
        doneLabel   = strings.done,
        tag         = "sheet_poignee",
        onDismiss   = onDismiss
    ) {
        // One shared size so every segment of every row uses the same font size.
        val segmentSize = rememberSharedAutoSizeState(strings.noneOption)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            playerNames.forEachIndexed { seat, name ->
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)
                    ) {
                        PlayerAvatar(name = name, seatIndex = seat, size = AvatarSize.S)
                        Text(text = name, style = MaterialTheme.typography.titleSmall)
                    }
                    val current = poignees.levelOf(name)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        levels.forEachIndexed { index, (level, label) ->
                            SegmentedButton(
                                shape    = SegmentedButtonDefaults.itemShape(index, levels.size),
                                selected = current == level,
                                onClick  = { onPoignees(poignees.withLevel(name, level)) },
                                icon     = {},
                                colors   = salonSegmentedButtonColors(),
                                modifier = Modifier.testTag("poignee_${name}_${level.name}")
                            ) {
                                AutoSizeText(
                                    text            = label,
                                    modifier        = Modifier.padding(horizontal = 1.dp),
                                    sharedSizeState = segmentSize
                                )
                            }
                        }
                    }
                }
            }
            if (atoutError) {
                Text(
                    text  = atoutErrorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Chelem: outcome first, then (for any outcome but None) who called or achieved it,
// with the reminder that an announced chelem's caller leads the first trick.
@Composable
private fun ChelemSheet(
    playerNames: List<String>,
    candidates: List<String>,
    chelem: Chelem,
    chelemPlayer: String?,
    onChelem: (Chelem, String?) -> Unit,
    strings: AppStrings,
    locale: AppLocale,
    onDismiss: () -> Unit
) {
    BonusSheetFrame(
        title       = strings.chelemLabel,
        explanation = strings.chelemTooltipBody.substringBefore('\n'),
        doneLabel   = strings.done,
        tag         = "sheet_chelem",
        onDismiss   = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            Text(
                text  = strings.chelemOutcomeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            for (option in Chelem.entries) {
                ChoiceRow(
                    text     = option.localizedName(locale),
                    selected = chelem == option,
                    // Changing the outcome resets who called it.
                    onClick  = { onChelem(option, if (option == chelem) chelemPlayer else null) },
                    tag      = "chelem_${option.name}"
                )
            }
            if (chelem != Chelem.NONE) {
                HorizontalDivider()
                Text(
                    text  = strings.chelemPlayerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                for (name in candidates) {
                    ChoiceRow(
                        text       = name,
                        selected   = chelemPlayer == name,
                        // Tapping the selected player again clears the choice.
                        onClick    = { onChelem(chelem, if (chelemPlayer == name) null else name) },
                        tag        = "chelem_player_$name",
                        avatarSeat = playerNames.indexOf(name)
                    )
                }
                if (isAnnouncedChelem(chelem) && chelemPlayer != null) {
                    Text(
                        text  = strings.chelemPlaysFirst(chelemPlayer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
