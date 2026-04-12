package fr.mandarine.tarotcounter

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme

// SettingsScreen consolidates all user-preference controls in one place:
//   - Theme toggle  (☀️ light / 🌙 dark)
//   - Language toggle (🇬🇧 English / 🇫🇷 French)
//   - Developer contact button
//
// It is a full-screen composable accessed from the gear icon on LandingScreen.
//
// onThemeChange:   called when the user taps a theme segment; persisted by the ViewModel.
// onLocaleChange:  called when the user taps a language segment; persisted by the ViewModel.
// onBack:          called when the user taps the back arrow; navigates to LandingScreen.
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onThemeChange: (AppTheme) -> Unit = {},
    onLocaleChange: (AppLocale) -> Unit = {},
    onBack: () -> Unit = {}
) {
    // Read the current locale and theme from the composition tree.
    // These are provided by CompositionLocalProvider in MainActivity and automatically
    // update when the user makes a selection — no extra state is needed here.
    val locale  = LocalAppLocale.current
    val theme   = LocalAppTheme.current
    val strings = appStrings(locale)

    // Controls whether the rules dialog is currently shown.
    // `by` delegation lets us read/write `showRules` directly (no .value needed).
    var showRules by remember { mutableStateOf(false) }

    // Context is needed to launch an external Intent (open the email client).
    val context = LocalContext.current

    // Show the rules dialog on top of the settings screen when the user taps the button.
    // The dialog is dismissed by calling `onDismissRequest` (tap outside) or "Close" button.
    if (showRules) {
        RulesDialog(onDismiss = { showRules = false })
    }

    // Box fills the whole screen and centers content horizontally so it looks good
    // on both phones and wide tablets without stretching across the full width.
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            // ── Back arrow + screen title ─────────────────────────────────────
            // ScreenHeader renders a back arrow (← ) on the leading edge followed
            // by the page title in headlineSmall style — the same pattern used by
            // FinalScoreScreen and ScoreHistoryScreen.
            ScreenHeader(title = strings.settingsTitle, onBack = onBack)

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // ── Theme section ─────────────────────────────────────────────────
            // A section label followed by a segmented button row (☀️ / 🌙).
            // SingleChoiceSegmentedButtonRow makes it obvious that only one option
            // can be selected at a time; the filled segment shows the current choice.
            Text(
                text     = strings.themeLabel,
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            val themeOptions   = listOf(AppTheme.LIGHT to "☀️", AppTheme.DARK to "🌙")
            val themeLabelSize = rememberSharedAutoSizeState(locale)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth(0.5f)) {
                themeOptions.forEachIndexed { index, (themeOption, label) ->
                    SegmentedButton(
                        shape    = SegmentedButtonDefaults.itemShape(index, themeOptions.size),
                        selected = theme == themeOption,
                        // Calling the callback even for the already-selected option is safe:
                        // the ViewModel will simply persist the same value again.
                        onClick  = { onThemeChange(themeOption) },
                        // Suppress the default checkmark — the filled segment already signals
                        // which option is selected.
                        icon     = {}
                    ) {
                        AutoSizeText(
                            text            = label,
                            modifier        = Modifier.padding(horizontal = 1.dp),
                            sharedSizeState = themeLabelSize
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // ── Language section ──────────────────────────────────────────────
            // Identical layout to the theme section: a label + a segmented button row.
            Text(
                text     = strings.languageLabel,
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            val localeOptions   = listOf(AppLocale.EN to "🇬🇧", AppLocale.FR to "🇫🇷")
            val localeLabelSize = rememberSharedAutoSizeState(locale)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth(0.5f)) {
                localeOptions.forEachIndexed { index, (localeOption, label) ->
                    SegmentedButton(
                        shape    = SegmentedButtonDefaults.itemShape(index, localeOptions.size),
                        selected = locale == localeOption,
                        onClick  = { onLocaleChange(localeOption) },
                        icon     = {}
                    ) {
                        AutoSizeText(
                            text            = label,
                            modifier        = Modifier.padding(horizontal = 1.dp),
                            sharedSizeState = localeLabelSize
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // ── Rules section ─────────────────────────────────────────────────
            // A full-width outlined button that opens the rules dialog.
            // AppOutlinedButton shrinks the label automatically so it fits on any width.
            AppOutlinedButton(
                text     = strings.rulesButton,
                onClick  = { showRules = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // ── Feedback / contact section ────────────────────────────────────
            // A TextButton (no fill, no border) keeps the action subtle. The
            // envelope icon makes the purpose immediately clear without reading.
            // Right-aligned so it doesn't compete with the centred toggles above.
            //
            // We use TextButton directly (rather than AppTextButton) because we need
            // to place an Icon alongside AutoSizeText inside the button content slot.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        // ACTION_SENDTO + "mailto:" URI opens only email clients —
                        // messaging apps are excluded by this intent filter.
                        val intent = Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("mailto:mandarinetech.dev@gmail.com")
                        )
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                ) {
                    // Icon on the left, label on the right, with 4 dp gap between them.
                    Icon(
                        imageVector        = Icons.Default.Email,
                        contentDescription = null, // adjacent label already describes the action
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    AutoSizeText(text = strings.feedbackButton)
                }
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    TarotCounterTheme {
        SettingsScreen()
    }
}
