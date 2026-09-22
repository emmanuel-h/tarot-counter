package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import fr.mandarine.tarotcounter.ui.theme.Dimens
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme

// RulesScreen (Salon restyle, issue #203): a full-screen page replacing the old
// dialog, opened from Settings → Help → Rules.
//
//   ┌──────────────────────────────┐
//   │ ←  Game Rules                │
//   │ Objective                    │  SectionHeader + body
//   │ ┌──────────────────────────┐ │
//   │ │ Bouts     Points needed  │ │  table from requiredPoints()
//   │ │   0            56        │ │
//   │ │   3            36        │ │
//   │ └──────────────────────────┘ │
//   │ ═════════ ♠ ♥ ♦ ♣ ═════════  │
//   │ Contracts                    │
//   │ ┌──────────────────────────┐ │
//   │ │ Contract     Multiplier  │ │  table from Contract.multiplier
//   │ │ Small            ×1      │ │
//   │ └──────────────────────────┘ │
//   │ Score Formula / Score Distribution / Bonuses  (text sections)
//   └──────────────────────────────┘
//
// The tables are built from the scoring code itself (RulesData.kt), so they can
// never disagree with the scores the app computes.
@Composable
fun RulesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val locale  = LocalAppLocale.current
    val strings = appStrings(locale)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenMargin)
                .padding(bottom = Dimens.SpaceL)
                .testTag("rules_screen"),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
        ) {
            SalonTopBar(
                title                  = strings.rulesTitle,
                onBack                 = onBack,
                backContentDescription = strings.rulesClose
            )

            // ── Objective + bouts table ───────────────────────────────────────
            RulesSection(strings.rulesObjectiveTitle, strings.rulesObjectiveBody)
            RulesTable(
                headers = strings.rulesBoutsColumn to strings.rulesNeededColumn,
                rows    = boutThresholdRows().map { (bouts, needed) -> bouts.toString() to needed.toString() },
                tag     = "rules_bouts_table"
            )

            SuitDivider(modifier = Modifier.padding(vertical = Dimens.SpaceS))

            // ── Contracts + multipliers table ─────────────────────────────────
            RulesSection(strings.rulesContractsTitle, strings.rulesContractsBody)
            RulesTable(
                headers = strings.rulesContractColumn to strings.rulesMultiplierColumn,
                rows    = contractMultiplierRows().map { (contract, multiplier) ->
                    contract.localizedName(locale) to multiplier
                },
                tag     = "rules_contracts_table"
            )

            SuitDivider(modifier = Modifier.padding(vertical = Dimens.SpaceS))

            // ── Text-only sections ────────────────────────────────────────────
            RulesSection(strings.rulesScoreFormulaTitle, strings.rulesScoreFormulaBody)
            RulesSection(strings.rulesDistributionTitle, strings.rulesDistributionBody)
            RulesSection(strings.rulesBonusTitle, strings.rulesBonusBody)
        }
    }
}

// A section: Cormorant heading (left-aligned) then a body paragraph.
@Composable
private fun RulesSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        SectionHeader(title = title)
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
}

// A two-column table in a paper card: muted header row, then hairline-separated
// rows. The second column is centred (numbers and multipliers).
@Composable
private fun RulesTable(headers: Pair<String, String>, rows: List<Pair<String, String>>, tag: String) {
    SalonCard(
        modifier       = Modifier.fillMaxWidth().testTag(tag),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column {
            RulesTableRow(headers.first, headers.second, header = true)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            rows.forEachIndexed { index, (left, right) ->
                RulesTableRow(left, right, header = false)
                if (index < rows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun RulesTableRow(left: String, right: String, header: Boolean) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyLarge
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .heightIn(min = if (header) 40.dp else 44.dp)
            .padding(horizontal = Dimens.SpaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = left, style = style, color = color, modifier = Modifier.weight(1f))
        Text(text = right, style = style, color = color, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(heightDp = 1400)
@Composable
private fun RulesScreenPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    TarotCounterTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            RulesScreen(onBack = {})
        }
    }
}
