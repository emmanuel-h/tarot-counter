package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme

// RulesDialog shows a scrollable summary of all scoring rules currently implemented
// in the game. It is opened when the user taps the "Rules" button on SettingsScreen.
//
// The dialog is capped at 85 % of the screen height so it never overflows on small
// devices. The content column scrolls independently within that space.
//
// onDismiss: called when the user taps "Close" or taps the scrim (dim area outside).
@Composable
fun RulesDialog(onDismiss: () -> Unit) {
    // Read the current locale from the composition tree so the dialog
    // automatically switches language when the user changes it in settings.
    val locale  = LocalAppLocale.current
    val strings = appStrings(locale)

    // Dialog is a bare-window overlay drawn on top of everything else.
    // onDismissRequest handles taps on the scrim around the dialog.
    Dialog(onDismissRequest = onDismiss) {

        // Surface provides the correct Material shape, elevation and background colour.
        // fillMaxHeight(0.85f) prevents the dialog from being taller than 85 % of the
        // screen — important on phones with many rule sections that exceed one screenful.
        Surface(
            modifier      = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape         = MaterialTheme.shapes.large,
            color         = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // ── Dialog title ──────────────────────────────────────────────
                Text(
                    text  = strings.rulesTitle,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Scrollable rules content ──────────────────────────────────
                // weight(1f, fill = false) lets this column grow to fill the space
                // between the title and the Close button, and enables scrolling when
                // the content is taller than the remaining height.
                // Without weight() the column would push the Close button off-screen.
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Each entry is a (sectionTitle, sectionBody) pair.
                    // Adding a new section only requires inserting a pair here —
                    // the rendering loop below handles the rest automatically.
                    val sections = listOf(
                        strings.rulesObjectiveTitle   to strings.rulesObjectiveBody,
                        strings.rulesContractsTitle   to strings.rulesContractsBody,
                        strings.rulesScoreFormulaTitle to strings.rulesScoreFormulaBody,
                        strings.rulesDistributionTitle to strings.rulesDistributionBody,
                        strings.rulesBonusTitle        to strings.rulesBonusBody,
                    )

                    sections.forEachIndexed { index, (title, body) ->
                        RulesSection(title = title, body = body)

                        // Place a divider between sections, but not after the last one.
                        if (index < sections.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Close button ──────────────────────────────────────────────
                // Right-aligned to match the "Send Feedback" button style on SettingsScreen.
                // AppTextButton wraps TextButton + AutoSizeText so the label always fits.
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    AppTextButton(
                        text    = strings.rulesClose,
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

// Renders one rules section: a bold heading followed by a body paragraph.
// Private because it is only ever called from RulesDialog above.
@Composable
private fun RulesSection(title: String, body: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(6.dp))
    // bodyMedium keeps the text readable but compact enough to fit many rules on screen.
    Text(
        text     = body,
        style    = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun RulesDialogPreview() {
    TarotCounterTheme {
        RulesDialog(onDismiss = {})
    }
}
